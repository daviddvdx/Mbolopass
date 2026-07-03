package ga.cyber241.mbolopass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
class UserProfileSecurityTest {
  private static final byte[] PNG_1X1 = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void profileAndPhotoRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/profile/me"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(multipart("/api/v1/profile/me/photo")
            .file(new MockMultipartFile("file", "avatar.png", "image/png", PNG_1X1)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void profileResponseIsMinimalAndUpdateIgnoresPrivilegeFields() throws Exception {
    String bearer = bearer(register(email(), "Amina", "N."));

    MvcResult result = mockMvc.perform(put("/api/v1/profile/me")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "firstName", "Amina",
                "lastName", "Secure",
                "birthDate", "2020-01-02",
                "gender", "F",
                "role", "ADMIN",
                "userId", UUID.randomUUID().toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Amina"))
        .andExpect(jsonPath("$.lastName").value("Secure"))
        .andExpect(jsonPath("$.birthDate").value("2020-01-02"))
        .andExpect(jsonPath("$.profilePhotoUrl").doesNotExist())
        .andReturn();

    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("password", "passwordHash", "accessToken", "permissions", "ADMIN");
  }

  @Test
  void profilePhotoValidatesContentAndDeletion() throws Exception {
    String bearer = bearer(register(email(), "Photo", "Owner"));

    mockMvc.perform(multipart("/api/v1/profile/me/photo")
            .file(new MockMultipartFile("file", "fake.jpg", "image/jpeg", "<html></html>".getBytes(StandardCharsets.UTF_8)))
            .header("Authorization", bearer))
        .andExpect(status().isBadRequest());

    mockMvc.perform(multipart("/api/v1/profile/me/photo")
            .file(new MockMultipartFile("file", "vector.svg", "image/svg+xml", "<svg></svg>".getBytes(StandardCharsets.UTF_8)))
            .header("Authorization", bearer))
        .andExpect(status().isBadRequest());

    mockMvc.perform(multipart("/api/v1/profile/me/photo")
            .file(new MockMultipartFile("file", "huge.png", "image/png", new byte[(int) (5L * 1024 * 1024 + 1)]))
            .header("Authorization", bearer))
        .andExpect(status().isBadRequest());

    mockMvc.perform(multipart("/api/v1/profile/me/photo")
            .file(new MockMultipartFile("file", "avatar.png", "image/png", PNG_1X1))
            .header("Authorization", bearer))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/profile/me/photo").header("Authorization", bearer))
        .andExpect(status().isOk());

    mockMvc.perform(delete("/api/v1/profile/me/photo").header("Authorization", bearer))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/profile/me/photo").header("Authorization", bearer))
        .andExpect(status().isNotFound());
  }

  @Test
  void parentCannotUploadPhotoForUnrelatedDependent() throws Exception {
    String owner = bearer(register(email(), "Parent", "One"));
    String other = bearer(register(email(), "Parent", "Two"));
    String dependentId = postJson("/api/v1/dependents", owner, Map.of("firstName", "Profil", "lastName", "Protege"))
        .get("id").asText();

    mockMvc.perform(multipart("/api/v1/dependents/" + dependentId + "/photo")
            .file(new MockMultipartFile("file", "avatar.png", "image/png", PNG_1X1))
            .header("Authorization", other))
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
  private String email() { return "profile-" + UUID.randomUUID() + "@example.test"; }
}
