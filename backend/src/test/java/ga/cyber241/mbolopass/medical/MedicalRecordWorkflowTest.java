package ga.cyber241.mbolopass.medical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ga.cyber241.mbolopass.common.Enums.Role;
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
class MedicalRecordWorkflowTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository users;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired JdbcTemplate jdbc;

  @Test
  void secureMedicalRecordAccessWorkflow() throws Exception {
    JsonNode patient = registerPatient();
    JsonNode otherPatient = registerPatient();
    String patientBearer = bearer(patient);
    String otherPatientBearer = bearer(otherPatient);
    String patientId = patient.at("/user/id").asText();

    mockMvc.perform(get("/api/v1/patient/medical-record").header("Authorization", patientBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.patientId").value(patientId));

    JsonNode professional = registerProfessional();
    String professionalBearer = bearer(professional);
    approveProfessional(professionalBearer);

    mockMvc.perform(get("/api/v1/health-professionals/patients?query=Patient&page=0&size=5").header("Authorization", professionalBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").exists())
        .andExpect(jsonPath("$.content[0].firstName").exists())
        .andExpect(jsonPath("$.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.content[0].history").doesNotExist());

    mockMvc.perform(get("/api/v1/health-professionals/patients/" + patientId + "/medical-record").header("Authorization", professionalBearer))
        .andExpect(status().isForbidden());

    JsonNode request = postJson("/api/v1/health-professionals/medical-access-requests", professionalBearer, Map.of("patientId", patientId, "reason", "Consultation programmee"));
    String requestId = request.get("id").asText();
    assertThat(request.get("status").asText()).isEqualTo("PENDING");
    mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", patientBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(1));

    mockMvc.perform(post("/api/v1/patient/medical-access-requests/" + requestId + "/approve").header("Authorization", otherPatientBearer))
        .andExpect(status().isNotFound());

    mockMvc.perform(post("/api/v1/patient/medical-access-requests/" + requestId + "/approve").header("Authorization", patientBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    JsonNode codeResponse = postJson("/api/v1/patient/medical-access-requests/" + requestId + "/temporary-code", patientBearer, Map.of());
    String code = codeResponse.get("code").asText();
    assertThat(code).matches("[A-HJ-KM-NP-Z2-9]{8}");
    String hash = jdbc.queryForObject("select code_hash from medical_record_access_requests where id = ?", String.class, UUID.fromString(requestId));
    assertThat(hash).isNotBlank().isNotEqualTo(code);
    MvcResult notifications = mockMvc.perform(get("/api/v1/notifications").header("Authorization", professionalBearer))
        .andExpect(status().isOk())
        .andReturn();
    assertThat(notifications.getResponse().getContentAsString()).doesNotContain(code, hash);

    mockMvc.perform(post("/api/v1/health-professionals/medical-access-requests/" + requestId + "/activate")
            .header("Authorization", professionalBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("code", code))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc.perform(post("/api/v1/health-professionals/medical-access-requests/" + requestId + "/activate")
            .header("Authorization", professionalBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("code", code))))
        .andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/v1/health-professionals/patients/" + patientId + "/medical-record").header("Authorization", professionalBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.patientId").value(patientId));

    mockMvc.perform(post("/api/v1/health-professionals/patients/" + patientId + "/medical-history")
            .header("Authorization", professionalBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("category", "CHRONIC_CONDITION", "title", "Asthme", "description", "Suivi clinique"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Asthme"));

    mockMvc.perform(post("/api/v1/health-professionals/patients/" + patientId + "/consultations")
            .header("Authorization", professionalBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("reason", "Controle respiratoire", "notes", "Observation clinique"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reason").value("Controle respiratoire"));

    mockMvc.perform(get("/api/v1/health-professionals/patients/" + patientId + "/medical-record").header("Authorization", bearer(login(createAdmin()))))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/v1/patient/medical-access-requests/" + requestId + "/revoke").header("Authorization", patientBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));

    mockMvc.perform(get("/api/v1/health-professionals/patients/" + patientId + "/medical-record").header("Authorization", professionalBearer))
        .andExpect(status().isForbidden());

    Integer audits = jdbc.queryForObject("select count(*) from medical_audit_events", Integer.class);
    assertThat(audits).isNotNull().isGreaterThanOrEqualTo(8);
  }

  @Test
  void pendingProfessionalCannotUseMedicalWorkflow() throws Exception {
    String bearer = bearer(registerProfessional());
    mockMvc.perform(get("/api/v1/health-professionals/patients?query=Patient").header("Authorization", bearer))
        .andExpect(status().isForbidden());
  }

  private JsonNode registerPatient() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Patient", "lastName", UUID.randomUUID().toString().substring(0, 6), "email", email("patient"), "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode registerProfessional() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register-professional")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Pro", "lastName", "Medical", "email", email("pro"), "password", "Password123!", "professionalType", "PHYSICIAN", "licenseNumber", "LIC-" + UUID.randomUUID(), "organizationName", "Mbolo Clinic"))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private void approveProfessional(String professionalBearer) throws Exception {
    String adminBearer = bearer(login(createAdmin()));
    String professionalProfileId = getJson("/api/v1/professional/me", professionalBearer).get("id").asText();
    mockMvc.perform(post("/api/v1/admin/professionals/" + professionalProfileId + "/approve").header("Authorization", adminBearer))
        .andExpect(status().isOk());
  }

  private String createAdmin() {
    User admin = new User();
    admin.setEmail(email("admin"));
    admin.setFirstName("Admin");
    admin.setLastName("Sante");
    admin.setRole(Role.HEALTH_ADMIN);
    admin.setPasswordHash(passwordEncoder.encode("Password123!"));
    users.save(admin);
    return admin.getEmail();
  }

  private JsonNode login(String email) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", "Password123!"))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode getJson(String path, String bearer) throws Exception {
    MvcResult result = mockMvc.perform(get(path).header("Authorization", bearer)).andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode postJson(String path, String bearer, Map<String, ?> payload) throws Exception {
    MvcResult result = mockMvc.perform(post(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(json(payload)))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String bearer(JsonNode auth) { return "Bearer " + auth.get("accessToken").asText(); }
  private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
  private String email(String prefix) { return prefix + "-" + UUID.randomUUID() + "@example.test"; }
}
