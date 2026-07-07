package ga.cyber241.mbolopass.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthProfileControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void healthProfileRequiresBearerToken() throws Exception {
    mockMvc.perform(get("/api/v1/health-profile/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void patientCanUpdateAndReadOwnMinimalProfile() throws Exception {
    String bearer = bearer(register(email(), "Amina", "N."));

    MvcResult before = mockMvc.perform(get("/api/v1/health-profile/me").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cardNumber").exists())
        .andReturn();
    String cardNumber = objectMapper.readTree(before.getResponse().getContentAsString()).get("cardNumber").asText();

    mockMvc.perform(put("/api/v1/health-profile/me")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "bloodType", "O+",
                "emergencyNotes", "Profil de demonstration uniquement",
                "lastMedicalVisitDate", "2026-01-15"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloodType").value("O+"))
        .andExpect(jsonPath("$.cardNumber").value(cardNumber));

    mockMvc.perform(get("/api/v1/health-profile/me").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloodType").value("O+"))
        .andExpect(jsonPath("$.emergencyNotes").value("Profil de demonstration uniquement"));
  }

  @Test
  void patientCannotDeleteAnotherPatientsHealthItem() throws Exception {
    String owner = bearer(register(email(), "Patient", "One"));
    String other = bearer(register(email(), "Patient", "Two"));

    MvcResult created = mockMvc.perform(post("/api/v1/health-profile/me/allergies")
            .header("Authorization", owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", "Profil de demonstration", "level", "HIGH"))))
        .andExpect(status().isOk())
        .andReturn();

    String itemId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    mockMvc.perform(delete("/api/v1/health-profile/me/allergies/" + itemId).header("Authorization", other))
        .andExpect(status().isNotFound());
  }

  @Test
  void patientCanAddAllergyAndConditionAndReadOnlyOwnItems() throws Exception {
    String owner = bearer(register(email(), "Patient", "One"));
    String other = bearer(register(email(), "Patient", "Two"));

    mockMvc.perform(post("/api/v1/health-profile/me/allergies")
            .header("Authorization", owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", "  Penicilline  ", "level", "HIGH", "critical", true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Penicilline"))
        .andExpect(jsonPath("$.level").value("HIGH"));

    mockMvc.perform(post("/api/v1/health-profile/me/conditions")
            .header("Authorization", owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", "Asthme", "level", "ACTIVE", "critical", false))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Asthme"))
        .andExpect(jsonPath("$.level").value("ACTIVE"));

    mockMvc.perform(get("/api/v1/health-profile/me/allergies").header("Authorization", owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].label").value("Penicilline"));

    mockMvc.perform(get("/api/v1/health-profile/me/conditions").header("Authorization", owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].label").value("Asthme"));

    mockMvc.perform(get("/api/v1/health-profile/me/allergies").header("Authorization", other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void healthItemValidationRejectsInvalidValuesAndDuplicates() throws Exception {
    String bearer = bearer(register(email(), "Patient", "Validation"));

    mockMvc.perform(post("/api/v1/health-profile/me/allergies")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", "Pollen", "level", "EXTREME"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Severite d'allergie invalide"));

    mockMvc.perform(post("/api/v1/health-profile/me/conditions")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", "Asthme", "level", "CURRENT"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Statut de condition invalide"));

    mockMvc.perform(post("/api/v1/health-profile/me/allergies")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", "   ", "level", "LOW"))))
        .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/v1/health-profile/me/allergies")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", "Latex", "level", "MEDIUM"))))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/health-profile/me/allergies")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", " latex ", "level", "MEDIUM"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Cette allergie existe deja"));
  }

  @Test
  void healthItemsArePersistedInDatabase() throws Exception {
    String bearer = bearer(register(email(), "Patient", "Persisted"));

    mockMvc.perform(post("/api/v1/health-profile/me/conditions")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("label", "Hypertension", "level", "HISTORICAL"))))
        .andExpect(status().isOk());

    MvcResult listed = mockMvc.perform(get("/api/v1/health-profile/me/conditions").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode body = objectMapper.readTree(listed.getResponse().getContentAsString());
    assertThat(body).hasSize(1);
    assertThat(body.get(0).get("label").asText()).isEqualTo("Hypertension");
  }

  private JsonNode register(String email, String firstName, String lastName) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", firstName, "lastName", lastName, "email", email, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String bearer(JsonNode auth) { return "Bearer " + auth.get("accessToken").asText(); }
  private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
  private String email() { return "health-" + UUID.randomUUID() + "@example.test"; }
}
