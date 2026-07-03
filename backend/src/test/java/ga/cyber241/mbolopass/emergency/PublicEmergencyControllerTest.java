package ga.cyber241.mbolopass.emergency;

import static org.assertj.core.api.Assertions.assertThat;
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
class PublicEmergencyControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void publicEmergencyReturnsOnlyVitalAllowedFields() throws Exception {
    String email = email();
    String bearer = bearer(register(email, "Amina", "N."));
    prepareEmergencyProfile(bearer);

    String token = createToken(bearer);
    MvcResult result = mockMvc.perform(get("/api/v1/public/emergency/" + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloodType").value("O+"))
        .andExpect(jsonPath("$.allergies[0].label").value("Allergie critique demo"))
        .andExpect(jsonPath("$.allergies[0].severity").value("CRITICAL"))
        .andExpect(jsonPath("$.criticalConditions[0]").value("Condition active demo"))
        .andExpect(jsonPath("$.criticalMedications[0]").value("Traitement critique demo"))
        .andExpect(jsonPath("$.emergencyNotes").value("Notes urgence demo limitees"))
        .andExpect(jsonPath("$.emergencyContact.phone").value("+24100000000"))
        .andExpect(jsonPath("$.patientDisplayName").doesNotExist())
        .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).doesNotContain(email, "password", "passwordHash", "birthDate", "token", "historique", "Allergie faible demo", "Condition historique demo", "Traitement non critique demo");
  }

  @Test
  void revokedAndExpiredQrTokensDoNotExposeEmergencyData() throws Exception {
    String bearer = bearer(register(email(), "Amina", "N."));
    prepareEmergencyProfile(bearer);

    String revokedToken = createToken(bearer);
    mockMvc.perform(post("/api/v1/card/me/qr-token/revoke").header("Authorization", bearer))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/public/emergency/" + revokedToken))
        .andExpect(status().isGone());

    String expiredToken = createToken(bearer);
    jdbcTemplate.update("update qr_tokens set expires_at = CURRENT_TIMESTAMP - INTERVAL '1' DAY where status = 'ACTIVE'");
    mockMvc.perform(get("/api/v1/public/emergency/" + expiredToken))
        .andExpect(status().isGone());
  }

  private void prepareEmergencyProfile(String bearer) throws Exception {
    mockMvc.perform(put("/api/v1/health-profile/me")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("bloodType", "O+", "emergencyNotes", "Notes urgence demo limitees", "lastMedicalVisitDate", "2026-01-15"))))
        .andExpect(status().isOk());
    postItem("/api/v1/health-profile/me/allergies", bearer, Map.of("label", "Allergie critique demo", "level", "CRITICAL"));
    postItem("/api/v1/health-profile/me/allergies", bearer, Map.of("label", "Allergie faible demo", "level", "LOW"));
    postItem("/api/v1/health-profile/me/conditions", bearer, Map.of("label", "Condition active demo", "level", "ACTIVE"));
    postItem("/api/v1/health-profile/me/conditions", bearer, Map.of("label", "Condition historique demo", "level", "HISTORICAL"));
    postItem("/api/v1/health-profile/me/medications", bearer, Map.of("label", "Traitement critique demo", "critical", true));
    postItem("/api/v1/health-profile/me/medications", bearer, Map.of("label", "Traitement non critique demo", "critical", false));
    postItem("/api/v1/health-profile/me/emergency-contacts", bearer, Map.of("label", "Mariam N.", "level", "Mere", "phone", "+24100000000", "critical", true));
  }

  private void postItem(String path, String bearer, Map<String, ?> payload) throws Exception {
    mockMvc.perform(post(path)
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(payload)))
        .andExpect(status().isOk());
  }

  private JsonNode register(String email, String firstName, String lastName) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", firstName, "lastName", lastName, "email", email, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String createToken(String bearer) throws Exception {
    MvcResult qr = mockMvc.perform(post("/api/v1/card/me/qr-token").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andReturn();
    String url = objectMapper.readTree(qr.getResponse().getContentAsString()).get("emergencyUrl").asText();
    return url.substring(url.lastIndexOf('/') + 1);
  }

  private String bearer(JsonNode auth) { return "Bearer " + auth.get("accessToken").asText(); }
  private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
  private String email() { return "public-emergency-" + UUID.randomUUID() + "@example.test"; }
}