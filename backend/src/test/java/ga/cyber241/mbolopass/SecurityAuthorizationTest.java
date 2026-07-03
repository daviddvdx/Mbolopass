package ga.cyber241.mbolopass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void protectedRoutesReturnUnauthorizedWithoutJwtAndForbiddenWithoutRole() throws Exception {
    mockMvc.perform(get("/api/v1/card/me"))
        .andExpect(status().isUnauthorized());

    String patient = bearer(register(email(), "Patient", "Role"));
    mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", patient))
        .andExpect(status().isForbidden());
  }

  @Test
  void browserCannotSelfAssignRoleDuringRegistration() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "firstName", "Role",
                "lastName", "Attempt",
                "email", email(),
                "password", "Password123!",
                "role", "ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.role").value("PATIENT"))
        .andReturn();

    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("password", "passwordHash");
  }

  @Test
  void patientCannotAccessUnrelatedDependentProfile() throws Exception {
    String owner = bearer(register(email(), "Parent", "One"));
    String other = bearer(register(email(), "Parent", "Two"));
    String dependentId = postJson("/api/v1/dependents", owner, Map.of("firstName", "Profil", "lastName", "Protege"))
        .get("id").asText();

    mockMvc.perform(get("/api/v1/dependents/" + dependentId).header("Authorization", other))
        .andExpect(status().isForbidden());
  }

  @Test
  void patientCannotDownloadAnotherUsersDocumentAndDocumentDtoIsMinimal() throws Exception {
    String owner = bearer(register(email(), "Doc", "Owner"));
    String other = bearer(register(email(), "Doc", "Other"));

    MvcResult upload = mockMvc.perform(multipart("/api/v1/documents")
            .file(new MockMultipartFile("file", "document.pdf", "application/pdf", "contenu".getBytes(StandardCharsets.UTF_8)))
            .file(new MockMultipartFile("metadata", "", "application/json", json(Map.of("title", "Document prive", "category", "OTHER")).getBytes(StandardCharsets.UTF_8)))
            .header("Authorization", owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.ownerId").doesNotExist())
        .andExpect(jsonPath("$.storageKey").doesNotExist())
        .andReturn();

    String body = upload.getResponse().getContentAsString();
    String documentId = objectMapper.readTree(body).get("id").asText();
    assertThat(body).doesNotContain("storageKey", "ownerId", "healthProfileId", "dependentProfileId");

    mockMvc.perform(get("/api/v1/documents/" + documentId + "/download").header("Authorization", other))
        .andExpect(status().isForbidden());
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

  private String bearer(JsonNode auth) { return "Bearer " + auth.get("accessToken").asText(); }
  private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
  private String email() { return "security-" + UUID.randomUUID() + "@example.test"; }
}
