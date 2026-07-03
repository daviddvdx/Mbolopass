package ga.cyber241.mbolopass.health;

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

    mockMvc.perform(put("/api/v1/health-profile/me")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "bloodType", "O+",
                "emergencyNotes", "Profil de demonstration uniquement",
                "lastMedicalVisitDate", "2026-01-15"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloodType").value("O+"));

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
