package ga.cyber241.mbolopass.clinical;

import ga.cyber241.mbolopass.common.Enums.*;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileRepository;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ClinicalService {
  private static final Logger log = LoggerFactory.getLogger(ClinicalService.class);
  private final UserRepository users;
  private final HealthProfileRepository healthProfiles;
  private final ProfessionalProfileRepository professionals;
  private final PatientProfessionalAccessRepository accesses;
  private final ClinicalEncounterRepository encounters;
  private final MedicalReferenceCatalogRepository catalog;
  private final ExamCatalogRepository examCatalog;
  private final ExamOrderRepository examOrders;
  private final ExamResultRepository examResults;
  private final ClinicalAuditEventRepository auditEvents;
  private final JdbcTemplate jdbc;

  public ClinicalService(UserRepository users, HealthProfileRepository healthProfiles, ProfessionalProfileRepository professionals, PatientProfessionalAccessRepository accesses, ClinicalEncounterRepository encounters, MedicalReferenceCatalogRepository catalog, ExamCatalogRepository examCatalog, ExamOrderRepository examOrders, ExamResultRepository examResults, ClinicalAuditEventRepository auditEvents, JdbcTemplate jdbc) {
    this.users = users;
    this.healthProfiles = healthProfiles;
    this.professionals = professionals;
    this.accesses = accesses;
    this.encounters = encounters;
    this.catalog = catalog;
    this.examCatalog = examCatalog;
    this.examOrders = examOrders;
    this.examResults = examResults;
    this.auditEvents = auditEvents;
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  public ProfessionalMe professionalMe(String email) {
    ProfessionalProfile profile = professionals.findByUserEmail(email).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profil professionnel introuvable"));
    log.debug("professional identity requested userId={} roles={} professionalProfileExists=true verificationStatus={}", profile.getUser().getId(), profile.getUser().getRole(), profile.getVerificationStatus());
    return new ProfessionalMe(profile.getId(), profile.getProfessionalType().name(), profile.getSpeciality(), profile.getOrganizationName(), profile.getVerificationStatus().name());
  }

  @Transactional(readOnly = true)
  public ProfessionalIdentity identityFor(User user) {
    if (user.getRole() != Role.HEALTH_PROFESSIONAL) {
      return new ProfessionalIdentity(false, null, null, null, false);
    }
    return professionals.findByUserId(user.getId())
        .map(profile -> new ProfessionalIdentity(
            true,
            profile.getProfessionalType() == null ? null : profile.getProfessionalType().name(),
            profile.getSpeciality(),
            profile.getVerificationStatus() == null ? null : profile.getVerificationStatus().name(),
            profile.getVerificationStatus() == ProfessionalVerificationStatus.APPROVED))
        .orElseGet(() -> {
          log.debug("professional identity missing userId={} roles={} professionalProfileExists=false verificationStatus=null", user.getId(), user.getRole());
          return new ProfessionalIdentity(false, null, null, null, false);
        });
  }

  @Transactional
  public ProfessionalProfile createProfessionalProfile(User user, ProfessionalRegisterRequest request) {
    ProfessionalProfile profile = new ProfessionalProfile();
    profile.setUser(user);
    profile.setProfessionalType(request.professionalType() == null ? ProfessionalType.OTHER : request.professionalType());
    profile.setSpeciality(clean(request.speciality()));
    profile.setLicenseNumber(clean(request.licenseNumber()));
    profile.setOrganizationName(clean(request.organizationName()));
    return professionals.save(profile);
  }

  @Transactional
  public void ensureApprovedProfessionalProfile(User user, User verifiedBy) {
    if (professionals.findByUserId(user.getId()).isPresent()) return;
    ProfessionalProfile profile = new ProfessionalProfile();
    profile.setUser(user);
    profile.setProfessionalType(ProfessionalType.OTHER);
    profile.setSpeciality("Professionnel de sante");
    profile.setLicenseNumber("ADMIN-CREATED-" + user.getId());
    profile.setOrganizationName("MboloPass");
    profile.setVerificationStatus(ProfessionalVerificationStatus.APPROVED);
    profile.setVerifiedAt(Instant.now());
    profile.setVerifiedBy(verifiedBy);
    professionals.save(profile);
  }

  @Transactional
  public int backfillMissingProfessionalProfiles() {
    int created = 0;
    for (User user : users.findByRole(Role.HEALTH_PROFESSIONAL)) {
      if (professionals.findByUserId(user.getId()).isEmpty()) {
        ensureApprovedProfessionalProfile(user, null);
        created++;
      }
    }
    return created;
  }

  @Transactional
  public AccessResponse requestAccess(String email, AccessRequest request, HttpServletRequest http) {
    ProfessionalProfile professional = approvedProfessional(email);
    HealthProfile patient = healthProfiles.findById(request.healthProfileId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient introuvable"));
    PatientProfessionalAccess access = new PatientProfessionalAccess();
    access.setHealthProfile(patient);
    access.setProfessionalProfile(professional);
    access.setAccessReason(clean(request.reason()));
    access.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
    PatientProfessionalAccess saved = accesses.save(access);
    audit(professional.getUser(), professional, patient, null, "PATIENT_RECORD_ACCESS_REQUESTED", "PATIENT_PROFESSIONAL_ACCESS", saved.getId(), request.reason(), http);
    return toAccess(saved);
  }

  @Transactional(readOnly = true)
  public List<AccessResponse> professionalAccesses(String email) {
    ProfessionalProfile professional = approvedProfessional(email);
    return accesses.findByProfessionalProfileId(professional.getId()).stream().map(this::toAccess).toList();
  }

  @Transactional(readOnly = true)
  public List<AccessResponse> patientAccesses(String email) {
    return accesses.findByHealthProfileUserEmail(email).stream().map(this::toAccess).toList();
  }

  @Transactional
  public AccessResponse approveAccess(String email, UUID accessId, HttpServletRequest http) {
    User patientUser = actor(email);
    PatientProfessionalAccess access = patientOwnedAccess(email, accessId);
    access.setStatus(PatientProfessionalAccessStatus.ACTIVE);
    access.setGrantedAt(Instant.now());
    access.setGrantedBy(patientUser);
    access.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
    audit(patientUser, null, access.getHealthProfile(), null, "PATIENT_RECORD_ACCESS_GRANTED", "PATIENT_PROFESSIONAL_ACCESS", access.getId(), access.getAccessReason(), http);
    return toAccess(accesses.save(access));
  }

  @Transactional
  public AccessResponse revokeAccess(String email, UUID accessId, HttpServletRequest http) {
    User patientUser = actor(email);
    PatientProfessionalAccess access = patientOwnedAccess(email, accessId);
    access.setStatus(PatientProfessionalAccessStatus.REVOKED);
    access.setRevokedAt(Instant.now());
    audit(patientUser, null, access.getHealthProfile(), null, "PATIENT_RECORD_ACCESS_REVOKED", "PATIENT_PROFESSIONAL_ACCESS", access.getId(), access.getAccessReason(), http);
    return toAccess(accesses.save(access));
  }

  @Transactional
  public PatientRecord record(String email, UUID healthProfileId, HttpServletRequest http) {
    ProfessionalProfile professional = approvedProfessional(email);
    PatientProfessionalAccess access = activeAccess(professional, healthProfileId);
    HealthProfile patient = access.getHealthProfile();
    audit(professional.getUser(), professional, patient, null, "PATIENT_RECORD_OPENED", "HEALTH_PROFILE", patient.getId(), access.getAccessReason(), http);
    List<String> allergies = jdbc.query("select label || ' (' || severity || ')' from allergies where health_profile_id = ?", (rs, row) -> rs.getString(1), patient.getId());
    List<String> conditions = jdbc.query("select label || ' (' || status || ')' from medical_conditions where health_profile_id = ?", (rs, row) -> rs.getString(1), patient.getId());
    List<EncounterResponse> previous = encounters.findByHealthProfileId(patient.getId()).stream().map(this::toEncounter).toList();
    return new PatientRecord(patient.getId(), patient.getCardNumber(), patient.getUser().getFirstName(), patient.getUser().getLastName(), patient.getBloodType(), allergies, conditions, previous);
  }

  @Transactional
  public EncounterResponse createEncounter(String email, UUID healthProfileId, EncounterRequest request, HttpServletRequest http) {
    ProfessionalProfile professional = physician(email);
    PatientProfessionalAccess access = activeAccess(professional, healthProfileId);
    ClinicalEncounter encounter = new ClinicalEncounter();
    encounter.setHealthProfile(access.getHealthProfile());
    encounter.setProfessionalProfile(professional);
    encounter.setPatientAccess(access);
    encounter.setEncounterType(clean(request.encounterType()));
    encounter.setReason(clean(request.reason()));
    encounter.setClinicalNotes(clean(request.clinicalNotes()));
    encounter.setStatus(ClinicalEncounterStatus.IN_PROGRESS);
    encounter.setStartedAt(Instant.now());
    ClinicalEncounter saved = encounters.save(encounter);
    audit(professional.getUser(), professional, access.getHealthProfile(), saved, "ENCOUNTER_CREATED", "CLINICAL_ENCOUNTER", saved.getId(), request.reason(), http);
    return toEncounter(saved);
  }

  @Transactional(readOnly = true)
  public EncounterResponse encounter(String email, UUID encounterId) {
    ProfessionalProfile professional = approvedProfessional(email);
    ClinicalEncounter encounter = encounters.findById(encounterId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Consultation introuvable"));
    activeAccess(professional, encounter.getHealthProfile().getId());
    return toEncounter(encounter);
  }

  @Transactional
  public EncounterResponse updateEncounter(String email, UUID encounterId, EncounterRequest request, HttpServletRequest http) {
    ProfessionalProfile professional = physician(email);
    ClinicalEncounter encounter = ownedOpenEncounter(professional, encounterId);
    encounter.setClinicalNotes(clean(request.clinicalNotes()));
    encounter.setReason(clean(request.reason()));
    audit(professional.getUser(), professional, encounter.getHealthProfile(), encounter, "ENCOUNTER_UPDATED", "CLINICAL_ENCOUNTER", encounter.getId(), request.reason(), http);
    return toEncounter(encounters.save(encounter));
  }

  @Transactional
  public EncounterResponse closeEncounter(String email, UUID encounterId, HttpServletRequest http) {
    ProfessionalProfile professional = physician(email);
    ClinicalEncounter encounter = ownedOpenEncounter(professional, encounterId);
    encounter.setStatus(ClinicalEncounterStatus.CLOSED);
    encounter.setClosedAt(Instant.now());
    audit(professional.getUser(), professional, encounter.getHealthProfile(), encounter, "ENCOUNTER_CLOSED", "CLINICAL_ENCOUNTER", encounter.getId(), null, http);
    rebuildProjection(encounter.getHealthProfile().getId());
    return toEncounter(encounters.save(encounter));
  }

  @Transactional
  public ClinicalItemResponse createDiagnosis(String email, UUID encounterId, DiagnosisRequest request, HttpServletRequest http) {
    ProfessionalProfile professional = physician(email);
    ClinicalEncounter encounter = ownedOpenEncounter(professional, encounterId);
    MedicalReferenceCatalog reference = catalog.findById(request.referenceCatalogId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reference medicale introuvable"));
    if (!reference.isActive()) throw new ApiException(HttpStatus.BAD_REQUEST, "Reference inactive");
    UUID id = UUID.randomUUID();
    jdbc.update("""
        insert into medical_conditions (id, health_profile_id, label, status, notes, clinical_encounter_id, created_by_user_id, professional_profile_id, reference_catalog_id, source, verification_status, clinical_status, validated_at, validated_by_user_id)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'HEALTH_PROFESSIONAL', 'VALIDATED', ?, ?, ?)
        """, id, encounter.getHealthProfile().getId(), reference.getDisplayName(), clinicalStatusToLegacy(request.clinicalStatus()), clean(request.notes()), encounter.getId(), professional.getUser().getId(), professional.getId(), reference.getId(), request.clinicalStatus() == null ? "ACTIVE" : request.clinicalStatus().name(), Instant.now(), professional.getUser().getId());
    audit(professional.getUser(), professional, encounter.getHealthProfile(), encounter, "DIAGNOSIS_CREATED", "MEDICAL_CONDITION", id, null, http);
    rebuildProjection(encounter.getHealthProfile().getId());
    return new ClinicalItemResponse(id, reference.getDisplayName(), "HEALTH_PROFESSIONAL", "VALIDATED");
  }

  @Transactional
  public ClinicalItemResponse createAllergy(String email, UUID encounterId, AllergyRequest request, HttpServletRequest http) {
    ProfessionalProfile professional = physician(email);
    ClinicalEncounter encounter = ownedOpenEncounter(professional, encounterId);
    UUID id = UUID.randomUUID();
    jdbc.update("""
        insert into allergies (id, health_profile_id, label, severity, notes, critical, clinical_encounter_id, professional_profile_id, source, verification_status, validated_at, validated_by_user_id)
        values (?, ?, ?, ?, ?, ?, ?, ?, 'HEALTH_PROFESSIONAL', 'VALIDATED', ?, ?)
        """, id, encounter.getHealthProfile().getId(), cleanRequired(request.label()), request.level().name(), clean(request.notes()), request.critical(), encounter.getId(), professional.getId(), Instant.now(), professional.getUser().getId());
    audit(professional.getUser(), professional, encounter.getHealthProfile(), encounter, "ALLERGY_CREATED", "ALLERGY", id, null, http);
    rebuildProjection(encounter.getHealthProfile().getId());
    return new ClinicalItemResponse(id, request.label().trim(), "HEALTH_PROFESSIONAL", "VALIDATED");
  }

  @Transactional
  public ExamOrderResponse createExamOrder(String email, UUID encounterId, ExamOrderRequest request, HttpServletRequest http) {
    ProfessionalProfile professional = physician(email);
    ClinicalEncounter encounter = ownedOpenEncounter(professional, encounterId);
    ExamCatalog exam = examCatalog.findById(request.examCatalogId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    if (!exam.isActive()) throw new ApiException(HttpStatus.BAD_REQUEST, "Examen inactif");
    ExamOrder order = new ExamOrder();
    order.setHealthProfile(encounter.getHealthProfile());
    order.setClinicalEncounter(encounter);
    order.setOrderedBy(professional);
    order.setExamCatalog(exam);
    order.setPriority(request.priority() == null ? ExamPriority.ROUTINE : request.priority());
    order.setClinicalReason(clean(request.clinicalReason()));
    order.setOrderedAt(Instant.now());
    ExamOrder saved = examOrders.save(order);
    audit(professional.getUser(), professional, encounter.getHealthProfile(), encounter, "EXAM_ORDER_CREATED", "EXAM_ORDER", saved.getId(), request.clinicalReason(), http);
    return toExamOrder(saved);
  }

  @Transactional
  public ExamResultResponse enterExamResult(String email, UUID examOrderId, ExamResultRequest request, HttpServletRequest http) {
    ProfessionalProfile professional = approvedProfessional(email);
    if (professional.getProfessionalType() != ProfessionalType.LAB_TECHNICIAN && professional.getProfessionalType() != ProfessionalType.PHYSICIAN) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Professionnel non habilite pour les resultats");
    }
    ExamOrder order = examOrders.findById(examOrderId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Demande d'examen introuvable"));
    activeAccess(professional, order.getHealthProfile().getId());
    ExamResult result = new ExamResult();
    result.setExamOrder(order);
    result.setEnteredBy(professional);
    result.setResultSummary(cleanRequired(request.resultSummary()));
    result.setResultValue(clean(request.resultValue()));
    result.setUnit(clean(request.unit()));
    result.setReferenceRange(clean(request.referenceRange()));
    result.setResultedAt(Instant.now());
    order.setStatus(ExamOrderStatus.RESULT_AVAILABLE);
    ExamResult saved = examResults.save(result);
    audit(professional.getUser(), professional, order.getHealthProfile(), order.getClinicalEncounter(), "EXAM_RESULT_ENTERED", "EXAM_RESULT", saved.getId(), null, http);
    return toExamResult(saved);
  }

  @Transactional
  public ExamResultResponse validateExamResult(String email, UUID examResultId, HttpServletRequest http) {
    ProfessionalProfile professional = physician(email);
    ExamResult result = examResults.findById(examResultId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resultat introuvable"));
    activeAccess(professional, result.getExamOrder().getHealthProfile().getId());
    result.setValidatedBy(professional);
    result.setValidatedAt(Instant.now());
    result.setStatus(ExamResultStatus.VALIDATED);
    result.getExamOrder().setStatus(ExamOrderStatus.VALIDATED);
    audit(professional.getUser(), professional, result.getExamOrder().getHealthProfile(), result.getExamOrder().getClinicalEncounter(), "EXAM_RESULT_VALIDATED", "EXAM_RESULT", result.getId(), null, http);
    rebuildProjection(result.getExamOrder().getHealthProfile().getId());
    return toExamResult(examResults.save(result));
  }

  @Transactional(readOnly = true)
  public List<ProfessionalResponse> pendingProfessionals() {
    return professionals.findByVerificationStatus(ProfessionalVerificationStatus.PENDING).stream().map(this::toProfessional).toList();
  }

  @Transactional
  public ProfessionalResponse setProfessionalStatus(String adminEmail, UUID professionalId, ProfessionalVerificationStatus status, HttpServletRequest http) {
    User admin = actor(adminEmail);
    ProfessionalProfile professional = professionals.findById(professionalId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Professionnel introuvable"));
    professional.setVerificationStatus(status);
    if (status == ProfessionalVerificationStatus.APPROVED) {
      professional.setVerifiedAt(Instant.now());
      professional.setVerifiedBy(admin);
    }
    ProfessionalProfile saved = professionals.save(professional);
    audit(admin, null, null, null, status == ProfessionalVerificationStatus.APPROVED ? "PROFESSIONAL_APPROVED" : "PROFESSIONAL_SUSPENDED", "PROFESSIONAL_PROFILE", saved.getId(), null, http);
    return toProfessional(saved);
  }

  @Transactional
  public CatalogResponse createCatalog(String adminEmail, CatalogRequest request) {
    MedicalReferenceCatalog entry = new MedicalReferenceCatalog();
    entry.setCategory(request.category());
    entry.setCodeSystem(clean(request.codeSystem()) == null ? "INTERNAL" : request.codeSystem().trim());
    entry.setCode(clean(request.code()));
    entry.setDisplayName(cleanRequired(request.displayName()));
    entry.setActive(request.active() == null || request.active());
    entry.setCreatedBy(actor(adminEmail));
    return toCatalog(catalog.save(entry));
  }

  @Transactional(readOnly = true)
  public List<CatalogResponse> catalog() { return catalog.findAll().stream().map(this::toCatalog).toList(); }

  @Transactional(readOnly = true)
  public List<ExamCatalogResponse> examCatalog() { return examCatalog.findAll().stream().map(this::toExamCatalog).toList(); }

  @Transactional
  public ExamCatalogResponse createExamCatalog(ExamCatalogRequest request) {
    ExamCatalog entry = new ExamCatalog();
    entry.setCode(clean(request.code()));
    entry.setName(cleanRequired(request.name()));
    entry.setCategory(clean(request.category()));
    entry.setDescription(clean(request.description()));
    entry.setActive(request.active() == null || request.active());
    return toExamCatalog(examCatalog.save(entry));
  }

  private ProfessionalProfile professional(String email) {
    return professionals.findByUserEmail(email).orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Profil professionnel requis"));
  }
  private ProfessionalProfile approvedProfessional(String email) {
    ProfessionalProfile profile = professional(email);
    log.debug("professional access check userId={} roles={} professionalProfileExists=true verificationStatus={}", profile.getUser().getId(), profile.getUser().getRole(), profile.getVerificationStatus());
    if (!profile.getUser().isEnabled()) throw new ApiException(HttpStatus.FORBIDDEN, "Compte professionnel desactive");
    if (profile.getVerificationStatus() != ProfessionalVerificationStatus.APPROVED) throw new ApiException(HttpStatus.FORBIDDEN, "Professionnel non approuve");
    return profile;
  }
  private ProfessionalProfile physician(String email) {
    ProfessionalProfile profile = approvedProfessional(email);
    if (profile.getProfessionalType() != ProfessionalType.PHYSICIAN) throw new ApiException(HttpStatus.FORBIDDEN, "Action reservee aux medecins");
    return profile;
  }
  private PatientProfessionalAccess activeAccess(ProfessionalProfile professional, UUID healthProfileId) {
    return accesses.findFirstByHealthProfileIdAndProfessionalProfileIdAndStatusAndExpiresAtAfter(healthProfileId, professional.getId(), PatientProfessionalAccessStatus.ACTIVE, Instant.now())
        .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Acces patient inactif"));
  }
  private ClinicalEncounter ownedOpenEncounter(ProfessionalProfile professional, UUID encounterId) {
    ClinicalEncounter encounter = encounters.findById(encounterId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Consultation introuvable"));
    activeAccess(professional, encounter.getHealthProfile().getId());
    if (!encounter.getProfessionalProfile().getId().equals(professional.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Consultation non autorisee");
    if (encounter.getStatus() == ClinicalEncounterStatus.CLOSED) throw new ApiException(HttpStatus.BAD_REQUEST, "Consultation cloturee");
    return encounter;
  }
  private PatientProfessionalAccess patientOwnedAccess(String email, UUID accessId) {
    PatientProfessionalAccess access = accesses.findById(accessId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Demande introuvable"));
    if (!access.getHealthProfile().getUser().getEmail().equalsIgnoreCase(email)) throw new ApiException(HttpStatus.FORBIDDEN, "Acces refuse");
    return access;
  }
  private void rebuildProjection(UUID healthProfileId) {
    jdbc.update("update health_profiles set updated_at = ? where id = ?", Instant.now(), healthProfileId);
  }
  private String clinicalStatusToLegacy(ClinicalStatus status) {
    return status == ClinicalStatus.HISTORICAL || status == ClinicalStatus.RESOLVED ? "HISTORICAL" : "ACTIVE";
  }
  private User actor(String email) { return users.findByEmail(email.toLowerCase(Locale.ROOT)).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide")); }
  private void audit(User actor, ProfessionalProfile professional, HealthProfile patient, ClinicalEncounter encounter, String action, String resourceType, UUID resourceId, String reason, HttpServletRequest http) {
    ClinicalAuditEvent event = new ClinicalAuditEvent();
    event.setActor(actor); event.setActorProfessional(professional); event.setHealthProfile(patient); event.setClinicalEncounter(encounter);
    event.setAction(action); event.setResourceType(resourceType); event.setResourceId(resourceId); event.setAccessReason(clean(reason));
    event.setIpAddress(http == null ? null : http.getRemoteAddr()); event.setUserAgent(http == null ? null : clean(http.getHeader("User-Agent")));
    auditEvents.save(event);
  }
  private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private String cleanRequired(String value) {
    String cleaned = clean(value);
    if (cleaned == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Champ obligatoire");
    return cleaned;
  }
  private AccessResponse toAccess(PatientProfessionalAccess a) { return new AccessResponse(a.getId(), a.getHealthProfile().getId(), a.getProfessionalProfile().getId(), display(a.getProfessionalProfile().getUser()), a.getStatus().name(), a.getAccessReason(), a.getGrantedAt(), a.getExpiresAt()); }
  private EncounterResponse toEncounter(ClinicalEncounter e) { return new EncounterResponse(e.getId(), e.getHealthProfile().getId(), e.getStatus().name(), e.getReason(), e.getStartedAt(), e.getClosedAt()); }
  private ProfessionalResponse toProfessional(ProfessionalProfile p) { return new ProfessionalResponse(p.getId(), display(p.getUser()), p.getProfessionalType().name(), p.getSpeciality(), p.getOrganizationName(), p.getVerificationStatus().name()); }
  private CatalogResponse toCatalog(MedicalReferenceCatalog c) { return new CatalogResponse(c.getId(), c.getCategory().name(), c.getCodeSystem(), c.getCode(), c.getDisplayName(), c.isActive()); }
  private ExamCatalogResponse toExamCatalog(ExamCatalog e) { return new ExamCatalogResponse(e.getId(), e.getCode(), e.getName(), e.getCategory(), e.isActive()); }
  private ExamOrderResponse toExamOrder(ExamOrder o) { return new ExamOrderResponse(o.getId(), o.getClinicalEncounter().getId(), o.getExamCatalog().getName(), o.getStatus().name()); }
  private ExamResultResponse toExamResult(ExamResult r) { return new ExamResultResponse(r.getId(), r.getExamOrder().getId(), r.getResultSummary(), r.getStatus().name()); }
  private String display(User user) { return (user.getFirstName() + " " + user.getLastName()).trim(); }

  public record ProfessionalRegisterRequest(ProfessionalType professionalType, String speciality, @NotBlank String licenseNumber, String organizationName) {}
  public record ProfessionalMe(UUID id, String professionalType, String speciality, String organizationName, String verificationStatus) {}
  public record ProfessionalIdentity(boolean exists, String professionalType, String speciality, String verificationStatus, boolean isApproved) {}
  public record AccessRequest(UUID healthProfileId, String reason) {}
  public record AccessResponse(UUID id, UUID healthProfileId, UUID professionalProfileId, String professionalName, String status, String reason, Instant grantedAt, Instant expiresAt) {}
  public record PatientRecord(UUID healthProfileId, String cardNumber, String firstName, String lastName, String bloodType, List<String> allergies, List<String> conditions, List<EncounterResponse> encounters) {}
  public record EncounterRequest(String encounterType, String reason, String clinicalNotes) {}
  public record EncounterResponse(UUID id, UUID healthProfileId, String status, String reason, Instant startedAt, Instant closedAt) {}
  public record DiagnosisRequest(UUID referenceCatalogId, ClinicalStatus clinicalStatus, String notes) {}
  public record AllergyRequest(@NotBlank String label, AllergySeverity level, boolean critical, String notes) {}
  public record ClinicalItemResponse(UUID id, String label, String source, String verificationStatus) {}
  public record ExamOrderRequest(UUID examCatalogId, ExamPriority priority, String clinicalReason) {}
  public record ExamOrderResponse(UUID id, UUID encounterId, String examName, String status) {}
  public record ExamResultRequest(@NotBlank String resultSummary, String resultValue, String unit, String referenceRange) {}
  public record ExamResultResponse(UUID id, UUID examOrderId, String resultSummary, String status) {}
  public record ProfessionalResponse(UUID id, String fullName, String professionalType, String speciality, String organizationName, String verificationStatus) {}
  public record CatalogRequest(MedicalReferenceCategory category, String codeSystem, String code, @NotBlank String displayName, Boolean active) {}
  public record CatalogResponse(UUID id, String category, String codeSystem, String code, String displayName, boolean active) {}
  public record ExamCatalogRequest(String code, @NotBlank String name, String category, String description, Boolean active) {}
  public record ExamCatalogResponse(UUID id, String code, String name, String category, boolean active) {}
}
