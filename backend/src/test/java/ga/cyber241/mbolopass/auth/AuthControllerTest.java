package ga.cyber241.mbolopass.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileRepository;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository users;
  @Autowired HealthProfileRepository profiles;
  @Autowired HealthProfileService healthProfiles;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void registerCreatesPatientWithBcryptPasswordAndHealthProfile() throws Exception {
    String email = email();
    MvcResult result = register(email, "Amina", "N.");
    String body = result.getResponse().getContentAsString();
    JsonNode json = objectMapper.readTree(body);

    assertThat(json.get("accessToken").asText()).isNotBlank();
    assertThat(json.at("/user/role").asText()).isEqualTo("PATIENT");
    assertThat(body).doesNotContain("passwordHash", "Password123!");

    User user = users.findByEmail(email).orElseThrow();
    assertThat(user.getRole()).isEqualTo(Role.PATIENT);
    assertThat(user.getPasswordHash()).isNotEqualTo("Password123!");
    assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();
    HealthProfile profile = profiles.findByUserId(user.getId()).orElseThrow();
    assertThat(profile.getCardNumber()).matches("MBP-\\d{4}-[A-HJ-KM-NP-Z2-9]{4}-[A-HJ-KM-NP-Z2-9]{4}");
  }

  @Test
  void registerCreatesDistinctPersistedCardNumbersAndIgnoresSubmittedCardNumber() throws Exception {
    MvcResult first = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "firstName", "Carte",
                "lastName", "One",
                "email", email(),
                "password", "Password123!",
                "cardNumber", "MBP-2026-AAAA-BBBB"))))
        .andExpect(status().isOk())
        .andReturn();
    MvcResult second = register(email(), "Carte", "Two");

    JsonNode firstBody = objectMapper.readTree(first.getResponse().getContentAsString());
    JsonNode secondBody = objectMapper.readTree(second.getResponse().getContentAsString());
    HealthProfile firstProfile = profiles.findByUserId(UUID.fromString(firstBody.at("/user/id").asText())).orElseThrow();
    HealthProfile secondProfile = profiles.findByUserId(UUID.fromString(secondBody.at("/user/id").asText())).orElseThrow();

    assertThat(firstProfile.getCardNumber()).isNotEqualTo("MBP-2026-AAAA-BBBB");
    assertThat(firstProfile.getCardNumber()).isNotEqualTo(secondProfile.getCardNumber());
    assertThat(jdbcTemplate.queryForObject("select card_number from health_profiles where id = ?", String.class, firstProfile.getId()))
        .isEqualTo(firstProfile.getCardNumber());
  }

  @Test
  void existingProfileWithoutCardNumberIsBackfilledAndProfileUpdateKeepsSameNumber() throws Exception {
    String email = email();
    JsonNode auth = objectMapper.readTree(register(email, "Ancien", "Profil").getResponse().getContentAsString());
    User user = users.findByEmail(email).orElseThrow();
    HealthProfile profile = profiles.findByUserId(user.getId()).orElseThrow();
    String original = profile.getCardNumber();

    mockMvc.perform(get("/api/v1/profile/me").header("Authorization", "Bearer " + auth.get("accessToken").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cardNumber").value(original));

    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Blocked", "lastName", "Client", "email", email(), "password", "Password123!", "cardNumber", "MBP-2026-FAKE-FAKE"))))
        .andExpect(status().isOk());

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/profile/me")
            .header("Authorization", "Bearer " + auth.get("accessToken").asText())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Ancien", "lastName", "Modifie", "cardNumber", "MBP-2026-FAKE-FAKE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cardNumber").value(original));

    jdbcTemplate.execute("alter table health_profiles alter column card_number drop not null");
    jdbcTemplate.update("update health_profiles set card_number = null where id = ?", profile.getId());
    assertThat(healthProfiles.backfillMissingCardNumbers()).isEqualTo(1);

    String backfilled = jdbcTemplate.queryForObject("select card_number from health_profiles where id = ?", String.class, profile.getId());
    assertThat(backfilled).isNotBlank().isNotEqualTo(original).matches("MBP-\\d{4}-[A-HJ-KM-NP-Z2-9]{4}-[A-HJ-KM-NP-Z2-9]{4}");
  }

  @Test
  void registerIgnoresSubmittedRoleAndNormalizesIdentityFields() throws Exception {
    String email = email();
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "firstName", "  Amina  ",
                "lastName", "  N.  ",
                "email", email.toUpperCase(),
                "password", "Password123!",
                "role", "HEALTH_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value(email))
        .andExpect(jsonPath("$.user.firstName").value("Amina"))
        .andExpect(jsonPath("$.user.lastName").value("N."))
        .andExpect(jsonPath("$.user.role").value("PATIENT"))
        .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).doesNotContain("passwordHash", "ROLE_HEALTH_ADMIN");

    User user = users.findByEmail(email).orElseThrow();
    assertThat(user.getRole()).isEqualTo(Role.PATIENT);
    assertThat(profiles.findByUserId(user.getId())).isPresent();
  }

  @Test
  void registerMissingPasswordReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Amina", "lastName", "N.", "email", email()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void loginReturnsJwtAndRejectsBadPassword() throws Exception {
    String email = email();
    register(email, "Amina", "N.");

    MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.user.email").value(email))
        .andReturn();

    String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", "bad-password"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutRevokesCurrentToken() throws Exception {
    String email = email();
    register(email, "Logout", "Token");
    String token = token(login(email));

    mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginMissingRequiredFieldReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", "missing-password@example.test"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void meReturnsCanonicalAuthoritiesForAdminAndPatient() throws Exception {
    String adminEmail = createUser(Role.HEALTH_ADMIN);
    String patientEmail = email();
    register(patientEmail, "Patient", "Role");

    String adminToken = token(login(adminEmail));
    MvcResult adminMe = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("HEALTH_ADMIN"))
        .andExpect(jsonPath("$.roles[0]").value("ROLE_HEALTH_ADMIN"))
        .andReturn();
    assertThat(adminMe.getResponse().getContentAsString()).doesNotContain("ROLE_PATIENT");

    String patientToken = token(login(patientEmail));
    mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + patientToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("PATIENT"))
        .andExpect(jsonPath("$.roles[0]").value("ROLE_PATIENT"));
  }

  @Test
  void adminIsNotTreatedAsPatientForPatientRoutes() throws Exception {
    String adminEmail = createUser(Role.HEALTH_ADMIN);
    String adminBearer = "Bearer " + token(login(adminEmail));

    mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", adminBearer))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/card/me").header("Authorization", adminBearer))
        .andExpect(status().isForbidden());
  }

  private MvcResult register(String email, String firstName, String lastName) throws Exception {
    return mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", firstName, "lastName", lastName, "email", email, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
  }

  private String createUser(Role role) {
    User user = new User();
    user.setEmail(email());
    user.setFirstName("Role");
    user.setLastName(role.name());
    user.setRole(role);
    user.setPasswordHash(passwordEncoder.encode("Password123!"));
    users.save(user);
    return user.getEmail();
  }

  private MvcResult login(String email) throws Exception {
    return mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
  }

  private String token(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
  }

  private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
  private String email() { return "auth-" + UUID.randomUUID() + "@example.test"; }
}
