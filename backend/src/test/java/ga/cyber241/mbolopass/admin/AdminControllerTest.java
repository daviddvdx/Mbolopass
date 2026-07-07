package ga.cyber241.mbolopass.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ga.cyber241.mbolopass.admin.audit.AdminAuditLogRepository;
import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.emergency.CardService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository users;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired HealthProfileService healthProfiles;
  @Autowired CardService cards;
  @Autowired AdminAuditLogRepository auditLogs;

  @Test
  void adminCanAccessDashboardButPatientAndProfessionalAreForbidden() throws Exception {
    String admin = bearer(createUser(Role.HEALTH_ADMIN));
    String patient = bearer(createUser(Role.PATIENT));
    String professional = bearer(createUser(Role.HEALTH_PROFESSIONAL));

    mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsers").exists());

    mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", patient))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", professional))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreatesBlocksUnblocksUserAndAuditLogIsCreated() throws Exception {
    String admin = bearer(createUser(Role.HEALTH_ADMIN));
    long before = auditLogs.count();
    String professionalEmail = email();

    MvcResult created = mockMvc.perform(post("/api/v1/admin/users")
            .header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Admin", "lastName", "Created", "email", professionalEmail, "password", "Password123!", "role", "HEALTH_PROFESSIONAL"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andReturn();

    JsonNode user = objectMapper.readTree(created.getResponse().getContentAsString());
    String userId = user.get("id").asText();

    mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/status")
            .header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("enabled", false, "reason", "Test blocage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));

    mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/status")
            .header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("enabled", true, "reason", "Test deblocage"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true));

    MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", professionalEmail, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
    String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("HEALTH_PROFESSIONAL"))
        .andExpect(jsonPath("$.professionalProfile.exists").value(true))
        .andExpect(jsonPath("$.professionalProfile.verificationStatus").value("APPROVED"))
        .andExpect(jsonPath("$.professionalProfile.isApproved").value(true));

    assertThat(auditLogs.count()).isGreaterThanOrEqualTo(before + 3);
  }

  @Test
  void adminCannotBlockOwnAccount() throws Exception {
    User adminUser = createUser(Role.HEALTH_ADMIN);
    String admin = bearer(adminUser);

    mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getId() + "/status")
            .header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("enabled", false, "reason", "Self block"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminRevokesQrAndNeverReceivesRawToken() throws Exception {
    String admin = bearer(createUser(Role.HEALTH_ADMIN));
    User patient = createUser(Role.PATIENT);
    healthProfiles.ensureFor(patient);
    CardService.QrTokenResponse qr = cards.regenerate(patient.getEmail());

    MvcResult list = mockMvc.perform(get("/api/v1/admin/qr-cards")
            .header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].tokenHash").doesNotExist())
        .andReturn();
    String body = list.getResponse().getContentAsString();
    assertThat(body).doesNotContain(qr.emergencyUrl().substring(qr.emergencyUrl().lastIndexOf('/') + 1));

    String qrTokenId = objectMapper.readTree(body).at("/content/0/qrTokenId").asText();
    long before = auditLogs.count();
    mockMvc.perform(patch("/api/v1/admin/qr-cards/" + qrTokenId + "/revoke")
            .header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("reason", "Test revocation"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));
    assertThat(auditLogs.count()).isGreaterThan(before);
  }

  private User createUser(Role role) {
    User user = new User();
    user.setEmail(email());
    user.setFirstName(role.name());
    user.setLastName("Demo");
    user.setRole(role);
    user.setPasswordHash(passwordEncoder.encode("Password123!"));
    users.save(user);
    if (role == Role.PATIENT) healthProfiles.ensureFor(user);
    return user;
  }

  private String bearer(User user) throws Exception {
    MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", user.getEmail(), "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
    return "Bearer " + objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
  }

  private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
  private String email() { return "admin-test-" + UUID.randomUUID() + "@example.test"; }
}
