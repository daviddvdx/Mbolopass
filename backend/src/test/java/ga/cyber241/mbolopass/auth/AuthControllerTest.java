package ga.cyber241.mbolopass.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.health.HealthProfileRepository;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
  @Autowired PasswordEncoder passwordEncoder;

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
    assertThat(profiles.findByUserId(user.getId())).isPresent();
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
  void loginMissingRequiredFieldReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", "missing-password@example.test"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void meReturnsCanonicalAuthoritiesForAdminAndPatient() throws Exception {
    String adminEmail = createUser(Role.ADMIN);
    String patientEmail = email();
    register(patientEmail, "Patient", "Role");

    String adminToken = token(login(adminEmail));
    MvcResult adminMe = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
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
    String adminEmail = createUser(Role.ADMIN);
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
