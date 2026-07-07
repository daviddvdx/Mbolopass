package ga.cyber241.mbolopass.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class ClinicalWorkspaceSecurityTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository users;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired JdbcTemplate jdbc;

  @Test
  void professionalFlowRequiresApprovalActiveAccessAndAuditsActions() throws Exception {
    JsonNode patient = registerPatient();
    String patientBearer = bearer(patient);
    String healthProfileId = getJson("/api/v1/health-profile/me", patientBearer).get("id").asText();

    JsonNode pendingProfessional = registerProfessional("PHYSICIAN");
    String pendingBearer = bearer(pendingProfessional);

    mockMvc.perform(post("/api/v1/professional/encounters/" + UUID.randomUUID() + "/diagnoses")
            .header("Authorization", patientBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("referenceCatalogId", UUID.randomUUID().toString()))))
        .andExpect(status().isForbidden());

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/professional/patients/" + healthProfileId + "/record").header("Authorization", pendingBearer))
        .andExpect(status().isForbidden());

    String adminBearer = bearer(login(createAdmin()));
    String professionalId = getJson("/api/v1/professional/me", pendingBearer).get("id").asText();
    mockMvc.perform(post("/api/v1/admin/professionals/" + professionalId + "/approve").header("Authorization", adminBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationStatus").value("APPROVED"));

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/professional/patients/" + healthProfileId + "/record").header("Authorization", pendingBearer))
        .andExpect(status().isForbidden());

    JsonNode access = postJson("/api/v1/professional/access-requests", pendingBearer, Map.of("healthProfileId", healthProfileId, "reason", "Consultation programmee"));
    String accessId = access.get("id").asText();
    mockMvc.perform(post("/api/v1/patient/access-requests/" + accessId + "/approve").header("Authorization", patientBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/professional/patients/" + healthProfileId + "/record").header("Authorization", pendingBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.healthProfileId").value(healthProfileId));
    assertThat(auditCount("PATIENT_RECORD_OPENED")).isGreaterThanOrEqualTo(1);

    JsonNode catalog = postJson("/api/v1/admin/medical-catalog", adminBearer, Map.of("category", "CONDITION", "codeSystem", "INTERNAL", "code", "ASTHME", "displayName", "Asthme"));
    JsonNode exam = postJson("/api/v1/admin/exam-catalog", adminBearer, Map.of("code", "NFS", "name", "Numeration formule sanguine", "category", "BIOLOGY"));

    JsonNode encounter = postJson("/api/v1/professional/patients/" + healthProfileId + "/encounters", pendingBearer, Map.of("encounterType", "CONSULTATION", "reason", "Controle", "clinicalNotes", "Note clinique"));
    String encounterId = encounter.get("id").asText();

    mockMvc.perform(post("/api/v1/professional/encounters/" + encounterId + "/diagnoses")
            .header("Authorization", pendingBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("referenceCatalogId", catalog.get("id").asText(), "clinicalStatus", "ACTIVE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("HEALTH_PROFESSIONAL"));

    JsonNode order = postJson("/api/v1/professional/encounters/" + encounterId + "/exam-orders", pendingBearer, Map.of("examCatalogId", exam.get("id").asText(), "priority", "ROUTINE", "clinicalReason", "Bilan"));
    JsonNode lab = registerProfessional("LAB_TECHNICIAN");
    String labBearer = bearer(lab);
    String labId = getJson("/api/v1/professional/me", labBearer).get("id").asText();
    mockMvc.perform(post("/api/v1/admin/professionals/" + labId + "/approve").header("Authorization", adminBearer)).andExpect(status().isOk());
    JsonNode labAccess = postJson("/api/v1/professional/access-requests", labBearer, Map.of("healthProfileId", healthProfileId, "reason", "Resultat laboratoire"));
    mockMvc.perform(post("/api/v1/patient/access-requests/" + labAccess.get("id").asText() + "/approve").header("Authorization", patientBearer)).andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/professional/encounters/" + encounterId + "/diagnoses")
            .header("Authorization", labBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("referenceCatalogId", catalog.get("id").asText()))))
        .andExpect(status().isForbidden());

    JsonNode result = postJson("/api/v1/professional/exam-orders/" + order.get("id").asText() + "/results", labBearer, Map.of("resultSummary", "Resultat disponible", "resultValue", "12", "unit", "g/dL"));
    mockMvc.perform(post("/api/v1/professional/exam-results/" + result.get("id").asText() + "/validate").header("Authorization", pendingBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VALIDATED"));

    String before = getJson("/api/v1/health-profile/me", patientBearer).get("updatedAt").asText();
    mockMvc.perform(post("/api/v1/professional/encounters/" + encounterId + "/close").header("Authorization", pendingBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
    String after = getJson("/api/v1/health-profile/me", patientBearer).get("updatedAt").asText();
    assertThat(after).isNotEqualTo(before);

    mockMvc.perform(patch("/api/v1/professional/encounters/" + encounterId)
            .header("Authorization", pendingBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("clinicalNotes", "Modification interdite"))))
        .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/v1/patient/access-requests/" + accessId + "/revoke").header("Authorization", patientBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/professional/patients/" + healthProfileId + "/record").header("Authorization", pendingBearer))
        .andExpect(status().isForbidden());
  }

  @Test
  void changingPatientIdInUrlDoesNotBypassAccessChecks() throws Exception {
    JsonNode patientA = registerPatient();
    JsonNode patientB = registerPatient();
    String bearerA = bearer(patientA);
    String profileA = getJson("/api/v1/health-profile/me", bearerA).get("id").asText();
    String profileB = getJson("/api/v1/health-profile/me", bearer(patientB)).get("id").asText();
    String adminBearer = bearer(login(createAdmin()));
    JsonNode professional = registerProfessional("PHYSICIAN");
    String professionalBearer = bearer(professional);
    String professionalId = getJson("/api/v1/professional/me", professionalBearer).get("id").asText();
    mockMvc.perform(post("/api/v1/admin/professionals/" + professionalId + "/approve").header("Authorization", adminBearer)).andExpect(status().isOk());
    JsonNode access = postJson("/api/v1/professional/access-requests", professionalBearer, Map.of("healthProfileId", profileA, "reason", "Acces A"));
    mockMvc.perform(post("/api/v1/patient/access-requests/" + access.get("id").asText() + "/approve").header("Authorization", bearerA)).andExpect(status().isOk());
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/professional/patients/" + profileB + "/record").header("Authorization", professionalBearer))
        .andExpect(status().isForbidden());
  }

  @Test
  void professionalStatusMustBeApprovedForClinicalActions() throws Exception {
    JsonNode patient = registerPatient();
    String healthProfileId = getJson("/api/v1/health-profile/me", bearer(patient)).get("id").asText();
    String adminBearer = bearer(login(createAdmin()));

    JsonNode pending = registerProfessional("PHYSICIAN");
    String pendingBearer = bearer(pending);
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/professional/patients/" + healthProfileId + "/record").header("Authorization", pendingBearer))
        .andExpect(status().isForbidden());

    JsonNode rejected = registerProfessional("PHYSICIAN");
    String rejectedBearer = bearer(rejected);
    String rejectedId = getJson("/api/v1/professional/me", rejectedBearer).get("id").asText();
    mockMvc.perform(post("/api/v1/admin/professionals/" + rejectedId + "/reject").header("Authorization", adminBearer)).andExpect(status().isOk());
    mockMvc.perform(post("/api/v1/professional/access-requests")
            .header("Authorization", rejectedBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("healthProfileId", healthProfileId, "reason", "Tentative refusee"))))
        .andExpect(status().isForbidden());

    JsonNode suspended = registerProfessional("PHYSICIAN");
    String suspendedBearer = bearer(suspended);
    String suspendedId = getJson("/api/v1/professional/me", suspendedBearer).get("id").asText();
    mockMvc.perform(post("/api/v1/admin/professionals/" + suspendedId + "/approve").header("Authorization", adminBearer)).andExpect(status().isOk());
    mockMvc.perform(post("/api/v1/admin/professionals/" + suspendedId + "/suspend").header("Authorization", adminBearer)).andExpect(status().isOk());
    mockMvc.perform(post("/api/v1/professional/access-requests")
            .header("Authorization", suspendedBearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("healthProfileId", healthProfileId, "reason", "Tentative suspendue"))))
        .andExpect(status().isForbidden());
  }

  private JsonNode registerPatient() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Patient", "lastName", UUID.randomUUID().toString().substring(0, 6), "email", email("patient"), "password", "Password123!"))))
        .andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode registerProfessional(String type) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/register-professional")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("firstName", "Pro", "lastName", type, "email", email("pro"), "password", "Password123!", "professionalType", type, "licenseNumber", "LIC-" + UUID.randomUUID(), "organizationName", "Mbolo Clinic"))))
        .andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
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
        .andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode getJson(String path, String bearer) throws Exception {
    MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).header("Authorization", bearer)).andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }
  private JsonNode postJson(String path, String bearer, Map<String, ?> payload) throws Exception {
    MvcResult result = mockMvc.perform(post(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(json(payload))).andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }
  private long auditCount(String action) {
    Long count = jdbc.queryForObject("select count(*) from clinical_audit_events where action = ?", Long.class, action);
    return count == null ? 0 : count;
  }
  private String bearer(JsonNode auth) { return "Bearer " + auth.get("accessToken").asText(); }
  private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
  private String email(String prefix) { return prefix + "-" + UUID.randomUUID() + "@example.test"; }
}
