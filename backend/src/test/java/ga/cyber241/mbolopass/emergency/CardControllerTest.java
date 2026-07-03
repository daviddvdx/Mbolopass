package ga.cyber241.mbolopass.emergency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ga.cyber241.mbolopass.common.Enums.QrTokenStatus;
import ga.cyber241.mbolopass.health.HealthProfile;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository users;
  @Autowired HealthProfileRepository profiles;
  @Autowired QrTokenRepository qrTokens;

  @Test
  void cardRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/card/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedUserGetsOnlyOwnMinimalCard() throws Exception {
    JsonNode owner = register(email(), "Amina", "N.");
    JsonNode other = register(email(), "Boris", "K.");

    MvcResult ownerCard = mockMvc.perform(get("/api/v1/card/me").header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cardId").exists())
        .andExpect(jsonPath("$.fullName").value("Amina N."))
        .andExpect(jsonPath("$.bloodType").doesNotExist())
        .andExpect(jsonPath("$.profileCompletionPercentage").value(0))
        .andExpect(jsonPath("$.qrStatus").value("MISSING"))
        .andExpect(jsonPath("$.lastUpdatedAt").exists())
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.birthDate").doesNotExist())
        .andExpect(jsonPath("$.emergencyNotes").doesNotExist())
        .andExpect(jsonPath("$.allergies").doesNotExist())
        .andExpect(jsonPath("$.criticalMedications").doesNotExist())
        .andExpect(jsonPath("$.emergencyUrl").doesNotExist())
        .andReturn();

    MvcResult otherCard = mockMvc.perform(get("/api/v1/card/me").header("Authorization", bearer(other)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Boris K."))
        .andReturn();

    String ownerBody = ownerCard.getResponse().getContentAsString();
    String otherBody = otherCard.getResponse().getContentAsString();
    assertThat(ownerBody).doesNotContain(other.at("/user/email").asText(), "Boris");
    assertThat(otherBody).doesNotContain(owner.at("/user/email").asText(), "Amina");
  }

  @Test
  void readingCardDoesNotGenerateQrTokenAndQrGenerationIsLinkedToCurrentUser() throws Exception {
    JsonNode auth = register(email(), "Carte", "Test");
    User user = users.findByEmail(auth.at("/user/email").asText()).orElseThrow();
    HealthProfile profile = profiles.findByUserId(user.getId()).orElseThrow();

    mockMvc.perform(get("/api/v1/card/me").header("Authorization", bearer(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.qrStatus").value("MISSING"));

    assertThat(qrTokens.findByHealthProfileIdAndStatus(profile.getId(), QrTokenStatus.ACTIVE)).isEmpty();

    mockMvc.perform(post("/api/v1/card/me/qr-token").header("Authorization", bearer(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emergencyUrl").exists())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(qrTokens.findByHealthProfileIdAndStatus(profile.getId(), QrTokenStatus.ACTIVE)).hasSize(1);
  }

  @Test
  void cardDtoDoesNotExposeDetailedMedicalData() throws Exception {
    JsonNode auth = register(email(), "Minimal", "Dto");
    String bearer = bearer(auth);

    mockMvc.perform(put("/api/v1/health-profile/me")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("bloodType", "O+", "birthDate", "1990-01-01", "emergencyNotes", "Notes urgence demo"))))
        .andExpect(status().isOk());
    postJson("/api/v1/health-profile/me/allergies", bearer, Map.of("label", "Allergie demo", "level", "CRITICAL"));
    postJson("/api/v1/health-profile/me/medications", bearer, Map.of("label", "Traitement critique demo", "critical", true));

    MvcResult card = mockMvc.perform(get("/api/v1/card/me").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloodType").value("O+"))
        .andExpect(jsonPath("$.fullName").value("Minimal D."))
        .andExpect(jsonPath("$.emergencyNotes").doesNotExist())
        .andExpect(jsonPath("$.birthDate").doesNotExist())
        .andExpect(jsonPath("$.allergies").doesNotExist())
        .andExpect(jsonPath("$.criticalMedications").doesNotExist())
        .andReturn();

    assertThat(card.getResponse().getContentAsString())
        .doesNotContain("Notes urgence demo", "Allergie demo", "Traitement critique demo", auth.at("/user/email").asText());
  }

  private JsonNode register(String email, String firstName, String lastName) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", firstName, "lastName", lastName, "email", email, "password", "Password123!"))))
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
    return "card-" + UUID.randomUUID() + "@example.test";
  }
}
