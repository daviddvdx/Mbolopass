package ga.cyber241.mbolopass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MboloPassApiIntegrationTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void authRegisterLoginAndMeFlowWorks() throws Exception {
    String email = email();
    JsonNode register = register(email, "Amina", "N.");

    assertThat(register.get("accessToken").asText()).isNotBlank();
    assertThat(register.get("tokenType").asText()).isEqualTo("Bearer");
    assertThat(register.get("expiresInMinutes").asLong()).isEqualTo(120);
    assertThat(register.at("/user/role").asText()).isEqualTo("PATIENT");

    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Amina", "lastName", "N.", "email", email, "password", "Password123!"))))
        .andExpect(status().isConflict());

    JsonNode login = login(email);
    mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(login)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", "bad-password"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void healthProfileRequiresJwtAndPatientCannotDeleteOtherPatientData() throws Exception {
    mockMvc.perform(get("/api/v1/health-profile/me")).andExpect(status().isUnauthorized());

    JsonNode owner = register(email(), "Patient", "One");
    JsonNode other = register(email(), "Patient", "Two");

    JsonNode allergy = postJson("/api/v1/health-profile/me/allergies", bearer(owner), Map.of("label", "Profil de demonstration", "level", "HIGH"));

    mockMvc.perform(delete("/api/v1/health-profile/me/allergies/" + allergy.get("id").asText())
            .header("Authorization", bearer(other)))
        .andExpect(status().isNotFound());
  }

  @Test
  void qrActiveRevokedAndExpiredFlowsProtectEmergencyData() throws Exception {
    String email = email();
    JsonNode auth = register(email, "Amina", "N.");
    String bearer = bearer(auth);

    mockMvc.perform(put("/api/v1/health-profile/me")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("bloodType", "O+", "lastMedicalVisitDate", "2026-01-15"))))
        .andExpect(status().isOk());

    postJson("/api/v1/health-profile/me/allergies", bearer, Map.of("label", "Penicilline demo", "level", "CRITICAL"));
    postJson("/api/v1/health-profile/me/conditions", bearer, Map.of("label", "Condition active demo", "level", "ACTIVE"));
    postJson("/api/v1/health-profile/me/medications", bearer, Map.of("label", "Traitement critique demo", "critical", true));
    postJson("/api/v1/health-profile/me/emergency-contacts", bearer, Map.of("label", "Mariam N.", "level", "Mere", "phone", "+24100000000", "critical", true));

    JsonNode qr = postJson("/api/v1/card/me/qr-token", bearer, Map.of());
    String emergencyUrl = qr.get("emergencyUrl").asText();
    String token = emergencyUrl.substring(emergencyUrl.lastIndexOf('/') + 1);

    MvcResult result = mockMvc.perform(get("/api/v1/public/emergency/" + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloodType").value("O+"))
        .andExpect(jsonPath("$.allergies[0].severity").value("CRITICAL"))
        .andExpect(jsonPath("$.emergencyContact.phone").value("+24100000000"))
        .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).doesNotContain(email, "birthDate", "token", "history", "historique", "patientDisplayName", "email");

    mockMvc.perform(post("/api/v1/card/me/qr-token/revoke").header("Authorization", bearer))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/public/emergency/" + token)).andExpect(status().isGone());

    JsonNode expiredQr = postJson("/api/v1/card/me/qr-token", bearer, Map.of());
    String expiredToken = expiredQr.get("emergencyUrl").asText().substring(expiredQr.get("emergencyUrl").asText().lastIndexOf('/') + 1);
    jdbcTemplate.update("update qr_tokens set expires_at = CURRENT_TIMESTAMP - INTERVAL '1' DAY where status = 'ACTIVE'");
    mockMvc.perform(get("/api/v1/public/emergency/" + expiredToken)).andExpect(status().isGone());
  }

  @Test
  void hibernateCreatedExpectedTables() {
    Integer count = jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
    assertThat(count).isNotNull();
  }

  @Test
  void actuatorHealthIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists());
  }

  private JsonNode register(String email, String firstName, String lastName) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", firstName, "lastName", lastName, "email", email, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode login(String email) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode postJson(String path, String bearer, Map<String, ?> body) throws Exception {
    MvcResult result = mockMvc.perform(post(path)
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(body)))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String bearer(JsonNode response) {
    return "Bearer " + response.get("accessToken").asText();
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private String email() {
    return "demo-" + UUID.randomUUID() + "@example.test";
  }
}

