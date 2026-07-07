package ga.cyber241.mbolopass.medical;

import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileRepository;
import ga.cyber241.mbolopass.medical.MedicalDtos.*;
import ga.cyber241.mbolopass.medical.MedicalEnums.MedicalAccessGrantStatus;
import ga.cyber241.mbolopass.medical.MedicalEnums.MedicalAccessRequestStatus;
import ga.cyber241.mbolopass.notification.NotificationService;
import ga.cyber241.mbolopass.notification.NotificationType;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRecordService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
  private static final int MAX_CODE_ATTEMPTS = 5;

  private final UserRepository users;
  private final HealthProfileRepository healthProfiles;
  private final MedicalRecordRepository records;
  private final MedicalHistoryEntryRepository historyEntries;
  private final MedicalConsultationRepository consultations;
  private final MedicalRecordAccessRequestRepository accessRequests;
  private final MedicalRecordAccessGrantRepository grants;
  private final MedicalAuditEventRepository auditEvents;
  private final MedicalRecordAccessGuard accessGuard;
  private final PasswordEncoder passwordEncoder;
  private final NotificationService notifications;

  public MedicalRecordService(UserRepository users, HealthProfileRepository healthProfiles, MedicalRecordRepository records, MedicalHistoryEntryRepository historyEntries, MedicalConsultationRepository consultations, MedicalRecordAccessRequestRepository accessRequests, MedicalRecordAccessGrantRepository grants, MedicalAuditEventRepository auditEvents, MedicalRecordAccessGuard accessGuard, PasswordEncoder passwordEncoder, NotificationService notifications) {
    this.users = users;
    this.healthProfiles = healthProfiles;
    this.records = records;
    this.historyEntries = historyEntries;
    this.consultations = consultations;
    this.accessRequests = accessRequests;
    this.grants = grants;
    this.auditEvents = auditEvents;
    this.accessGuard = accessGuard;
    this.passwordEncoder = passwordEncoder;
    this.notifications = notifications;
  }

  @Transactional
  public MedicalRecord ensureForPatient(User patient) {
    if (patient.getRole() != Role.PATIENT) throw new ApiException(HttpStatus.BAD_REQUEST, "Un dossier medical ne peut etre cree que pour un patient");
    return records.findByPatientId(patient.getId()).orElseGet(() -> {
      MedicalRecord record = new MedicalRecord();
      record.setPatient(patient);
      record.setCreatedBy(patient);
      record.setUpdatedBy(patient);
      audit(patient, patient, "MEDICAL_RECORD_CREATED", "MEDICAL_RECORD", null, null);
      return records.save(record);
    });
  }

  @Transactional
  public int backfillMissingMedicalRecords() {
    int created = 0;
    for (User patient : users.findByRole(Role.PATIENT)) {
      if (!records.existsByPatientId(patient.getId())) {
        ensureForPatient(patient);
        created++;
      }
    }
    return created;
  }

  @Transactional
  public MedicalRecordDto ownRecord(String patientEmail) {
    User patient = patient(patientEmail);
    MedicalRecord record = records.findByPatientId(patient.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dossier medical introuvable"));
    audit(patient, patient, "MEDICAL_RECORD_VIEWED", "MEDICAL_RECORD", record.getId(), "self");
    return recordDto(record);
  }

  @Transactional(readOnly = true)
  public List<MedicalAccessRequestDto> patientRequests(String patientEmail) {
    return accessRequests.findByPatientEmailOrderByRequestedAtDesc(normalize(patientEmail)).stream().map(this::requestDto).toList();
  }

  @Transactional
  public MedicalAccessRequestDto approveRequest(String patientEmail, UUID requestId) {
    User patient = patient(patientEmail);
    MedicalRecordAccessRequest request = patientRequest(patientEmail, requestId);
    if (request.getStatus() != MedicalAccessRequestStatus.PENDING) throw new ApiException(HttpStatus.BAD_REQUEST, "Demande deja traitee");
    Instant now = Instant.now();
    request.setStatus(MedicalAccessRequestStatus.APPROVED);
    request.setRespondedAt(now);
    request.setApprovedAt(now);
    accessRequests.save(request);
    audit(patient, patient, "ACCESS_REQUEST_APPROVED", "MEDICAL_ACCESS_REQUEST", request.getId(), null);
    notifications.create(request.getHealthProfessional(), NotificationType.MEDICAL_ACCESS_REQUEST_APPROVED, "Demande d'acces acceptee", "Le patient a accepte votre demande. Demandez-lui maintenant son code temporaire.", "MEDICAL_ACCESS_REQUEST", request.getId());
    return requestDto(request);
  }

  @Transactional
  public MedicalAccessRequestDto denyRequest(String patientEmail, UUID requestId) {
    User patient = patient(patientEmail);
    MedicalRecordAccessRequest request = patientRequest(patientEmail, requestId);
    if (request.getStatus() != MedicalAccessRequestStatus.PENDING && request.getStatus() != MedicalAccessRequestStatus.APPROVED) throw new ApiException(HttpStatus.BAD_REQUEST, "Demande deja traitee");
    Instant now = Instant.now();
    request.setStatus(MedicalAccessRequestStatus.DENIED);
    request.setRespondedAt(now);
    request.setDeniedAt(now);
    request.setCodeHash(null);
    request.setCodeExpiresAt(null);
    accessRequests.save(request);
    audit(patient, patient, "ACCESS_REQUEST_DENIED", "MEDICAL_ACCESS_REQUEST", request.getId(), null);
    notifications.create(request.getHealthProfessional(), NotificationType.MEDICAL_ACCESS_REQUEST_DENIED, "Demande d'acces refusee", "Le patient a refuse votre demande d'acces au dossier medical.", "MEDICAL_ACCESS_REQUEST", request.getId());
    return requestDto(request);
  }

  @Transactional
  public TemporaryAccessCodeResponse generateTemporaryCode(String patientEmail, UUID requestId) {
    User patient = patient(patientEmail);
    MedicalRecordAccessRequest request = patientRequest(patientEmail, requestId);
    if (request.getStatus() != MedicalAccessRequestStatus.APPROVED && request.getStatus() != MedicalAccessRequestStatus.LOCKED) throw new ApiException(HttpStatus.BAD_REQUEST, "La demande doit etre approuvee avant de generer un code");
    String code = buildCode();
    Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
    request.setStatus(MedicalAccessRequestStatus.APPROVED);
    request.setCodeHash(passwordEncoder.encode(code));
    request.setCodeExpiresAt(expiresAt);
    request.setCodeUsedAt(null);
    request.setFailedCodeAttempts(0);
    request.setLockedUntil(null);
    accessRequests.save(request);
    audit(patient, patient, "TEMPORARY_CODE_GENERATED", "MEDICAL_ACCESS_REQUEST", request.getId(), "expiresAt=" + expiresAt);
    notifications.create(request.getHealthProfessional(), NotificationType.MEDICAL_ACCESS_CODE_GENERATED, "Code temporaire genere", "Le patient a genere un code temporaire. Il doit vous le communiquer directement.", "MEDICAL_ACCESS_REQUEST", request.getId());
    return new TemporaryAccessCodeResponse(code, expiresAt);
  }

  @Transactional
  public MedicalAccessRequestDto revokeAccess(String patientEmail, UUID requestId) {
    User patient = patient(patientEmail);
    MedicalRecordAccessRequest request = patientRequest(patientEmail, requestId);
    Instant now = Instant.now();
    request.setStatus(MedicalAccessRequestStatus.REVOKED);
    request.setRevokedAt(now);
    request.setCodeHash(null);
    grants.findByAccessRequestId(request.getId()).ifPresent(grant -> {
      grant.setStatus(MedicalAccessGrantStatus.REVOKED);
      grant.setRevokedAt(now);
      grants.save(grant);
    });
    accessRequests.save(request);
    audit(patient, patient, "ACCESS_REVOKED", "MEDICAL_ACCESS_REQUEST", request.getId(), null);
    notifications.create(request.getHealthProfessional(), NotificationType.MEDICAL_ACCESS_REVOKED, "Acces medical revoque", "Le patient a revoque votre acces a son dossier medical.", "MEDICAL_ACCESS_REQUEST", request.getId());
    return requestDto(request);
  }

  @Transactional
  public PatientSearchPage searchPatients(String professionalEmail, String query, int page, int size) {
    User professional = accessGuard.requireProfessional(professionalEmail);
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 20);
    String safeQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    Page<User> result = users.searchEnabledByRole(Role.PATIENT, safeQuery, PageRequest.of(safePage, safeSize, Sort.by("lastName").ascending()));
    audit(null, professional, "PATIENT_SEARCH", "USER", null, "queryLength=" + safeQuery.length());
    return new PatientSearchPage(result.getContent().stream().map(this::patientSearchDto).toList(), safePage, safeSize, result.getTotalElements(), result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public List<MedicalAccessRequestDto> professionalRequests(String professionalEmail) {
    accessGuard.requireProfessional(professionalEmail);
    return accessRequests.findByHealthProfessionalEmailOrderByRequestedAtDesc(normalize(professionalEmail)).stream().map(this::requestDto).toList();
  }

  @Transactional
  public MedicalAccessRequestDto createAccessRequest(String professionalEmail, CreateMedicalAccessRequest payload) {
    User professional = accessGuard.requireProfessional(professionalEmail);
    User patient = users.findById(payload.patientId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient introuvable"));
    if (patient.getRole() != Role.PATIENT || !patient.isEnabled()) throw new ApiException(HttpStatus.NOT_FOUND, "Patient introuvable");
    ensureForPatient(patient);
    Set<MedicalAccessRequestStatus> blocking = Set.of(MedicalAccessRequestStatus.PENDING, MedicalAccessRequestStatus.APPROVED, MedicalAccessRequestStatus.ACTIVE, MedicalAccessRequestStatus.LOCKED);
    if (accessRequests.existsByPatientIdAndHealthProfessionalIdAndStatusIn(patient.getId(), professional.getId(), blocking)) {
      throw new ApiException(HttpStatus.CONFLICT, "Une demande d'acces est deja en cours pour ce patient");
    }
    MedicalRecordAccessRequest request = new MedicalRecordAccessRequest();
    request.setPatient(patient);
    request.setHealthProfessional(professional);
    request.setRequestedAt(Instant.now());
    request.setReason(trim(payload.reason(), 500));
    accessRequests.save(request);
    audit(patient, professional, "ACCESS_REQUEST_CREATED", "MEDICAL_ACCESS_REQUEST", request.getId(), null);
    notifications.create(patient, NotificationType.MEDICAL_ACCESS_REQUEST_RECEIVED, "Nouvelle demande d'acces", displayName(professional) + " souhaite acceder a votre dossier medical.", "MEDICAL_ACCESS_REQUEST", request.getId());
    return requestDto(request);
  }

  @Transactional
  public MedicalAccessRequestDto activateRequest(String professionalEmail, UUID requestId, ActivateMedicalAccessRequest payload) {
    User professional = accessGuard.requireProfessional(professionalEmail);
    MedicalRecordAccessRequest request = professionalRequest(professionalEmail, requestId);
    Instant now = Instant.now();
    if (request.getStatus() == MedicalAccessRequestStatus.LOCKED && request.getLockedUntil() != null && request.getLockedUntil().isAfter(now)) {
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives. Veuillez demander un nouveau code.");
    }
    if (request.getStatus() != MedicalAccessRequestStatus.APPROVED) throw new ApiException(HttpStatus.BAD_REQUEST, "Demande non approuvee");
    if (request.getCodeHash() == null || request.getCodeExpiresAt() == null || request.getCodeExpiresAt().isBefore(now) || request.getCodeUsedAt() != null) {
      request.setStatus(MedicalAccessRequestStatus.EXPIRED);
      accessRequests.save(request);
      audit(request.getPatient(), professional, "TEMPORARY_CODE_EXPIRED", "MEDICAL_ACCESS_REQUEST", request.getId(), null);
      throw new ApiException(HttpStatus.BAD_REQUEST, "Code expire ou deja utilise");
    }
    audit(request.getPatient(), professional, "TEMPORARY_CODE_ATTEMPT", "MEDICAL_ACCESS_REQUEST", request.getId(), null);
    if (!passwordEncoder.matches(payload.code().trim(), request.getCodeHash())) {
      request.setFailedCodeAttempts(request.getFailedCodeAttempts() + 1);
      if (request.getFailedCodeAttempts() >= MAX_CODE_ATTEMPTS) {
        request.setStatus(MedicalAccessRequestStatus.LOCKED);
        request.setLockedUntil(now.plus(15, ChronoUnit.MINUTES));
        audit(request.getPatient(), professional, "TEMPORARY_CODE_LOCKED", "MEDICAL_ACCESS_REQUEST", request.getId(), null);
      } else {
        audit(request.getPatient(), professional, "TEMPORARY_CODE_FAILED", "MEDICAL_ACCESS_REQUEST", request.getId(), "attempts=" + request.getFailedCodeAttempts());
      }
      accessRequests.save(request);
      throw new ApiException(HttpStatus.BAD_REQUEST, "Code invalide");
    }
    MedicalRecordAccessGrant grant = new MedicalRecordAccessGrant();
    grant.setPatient(request.getPatient());
    grant.setHealthProfessional(professional);
    grant.setAccessRequest(request);
    grant.setActivatedAt(now);
    grant.setExpiresAt(request.getCodeExpiresAt());
    grant.setStatus(MedicalAccessGrantStatus.ACTIVE);
    request.setStatus(MedicalAccessRequestStatus.ACTIVE);
    request.setCodeUsedAt(now);
    request.setCodeHash(null);
    grants.save(grant);
    accessRequests.save(request);
    audit(request.getPatient(), professional, "ACCESS_ACTIVATED", "MEDICAL_ACCESS_GRANT", grant.getId(), null);
    notifications.create(request.getPatient(), NotificationType.MEDICAL_ACCESS_ACTIVATED, "Acces medical active", "Un professionnel a active son acces temporaire a votre dossier medical.", "MEDICAL_ACCESS_GRANT", grant.getId());
    return requestDto(request);
  }

  @Transactional
  public MedicalRecordDto professionalRecord(String professionalEmail, UUID patientId) {
    accessGuard.requireActiveAccess(patientId, professionalEmail);
    MedicalRecord record = records.findByPatientId(patientId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dossier medical introuvable"));
    User professional = accessGuard.requireProfessional(professionalEmail);
    audit(record.getPatient(), professional, "MEDICAL_RECORD_VIEWED", "MEDICAL_RECORD", record.getId(), "professional");
    return recordDto(record);
  }

  @Transactional
  public MedicalHistoryEntryDto createHistory(String professionalEmail, UUID patientId, CreateMedicalHistoryEntryRequest payload) {
    accessGuard.requireActiveAccess(patientId, professionalEmail);
    User professional = accessGuard.requireProfessional(professionalEmail);
    MedicalRecord record = records.findByPatientId(patientId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dossier medical introuvable"));
    MedicalHistoryEntry entry = new MedicalHistoryEntry();
    applyHistory(entry, payload.category(), payload.title(), payload.description(), payload.startDate(), payload.endDate(), payload.active());
    entry.setMedicalRecord(record);
    entry.setCreatedByProfessional(professional);
    entry.setUpdatedByProfessional(professional);
    historyEntries.save(entry);
    audit(record.getPatient(), professional, "MEDICAL_HISTORY_CREATED", "MEDICAL_HISTORY_ENTRY", entry.getId(), "category=" + entry.getCategory());
    return historyDto(entry);
  }

  @Transactional
  public MedicalHistoryEntryDto updateHistory(String professionalEmail, UUID patientId, UUID entryId, UpdateMedicalHistoryEntryRequest payload) {
    accessGuard.requireActiveAccess(patientId, professionalEmail);
    User professional = accessGuard.requireProfessional(professionalEmail);
    MedicalRecord record = records.findByPatientId(patientId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dossier medical introuvable"));
    MedicalHistoryEntry entry = historyEntries.findByIdAndMedicalRecordId(entryId, record.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Element introuvable"));
    applyHistory(entry, payload.category(), payload.title(), payload.description(), payload.startDate(), payload.endDate(), payload.active());
    entry.setUpdatedByProfessional(professional);
    historyEntries.save(entry);
    audit(record.getPatient(), professional, "MEDICAL_HISTORY_UPDATED", "MEDICAL_HISTORY_ENTRY", entry.getId(), "category=" + entry.getCategory());
    return historyDto(entry);
  }

  @Transactional
  public MedicalConsultationDto createConsultation(String professionalEmail, UUID patientId, CreateMedicalConsultationRequest payload) {
    accessGuard.requireActiveAccess(patientId, professionalEmail);
    User professional = accessGuard.requireProfessional(professionalEmail);
    MedicalRecord record = records.findByPatientId(patientId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dossier medical introuvable"));
    MedicalConsultation consultation = new MedicalConsultation();
    consultation.setMedicalRecord(record);
    consultation.setHealthProfessional(professional);
    applyConsultation(consultation, payload.consultationDate(), payload.reason(), payload.diagnosis(), payload.notes(), payload.treatment(), payload.followUpDate());
    consultations.save(consultation);
    audit(record.getPatient(), professional, "CONSULTATION_CREATED", "MEDICAL_CONSULTATION", consultation.getId(), null);
    return consultationDto(consultation);
  }

  @Transactional
  public MedicalConsultationDto updateConsultation(String professionalEmail, UUID patientId, UUID consultationId, UpdateMedicalConsultationRequest payload) {
    accessGuard.requireActiveAccess(patientId, professionalEmail);
    User professional = accessGuard.requireProfessional(professionalEmail);
    MedicalRecord record = records.findByPatientId(patientId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dossier medical introuvable"));
    MedicalConsultation consultation = consultations.findByIdAndMedicalRecordIdAndDeletedFalse(consultationId, record.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Consultation introuvable"));
    applyConsultation(consultation, payload.consultationDate(), payload.reason(), payload.diagnosis(), payload.notes(), payload.treatment(), payload.followUpDate());
    consultations.save(consultation);
    audit(record.getPatient(), professional, "CONSULTATION_UPDATED", "MEDICAL_CONSULTATION", consultation.getId(), null);
    return consultationDto(consultation);
  }

  private MedicalRecordAccessRequest patientRequest(String patientEmail, UUID requestId) {
    return accessRequests.findByIdAndPatientEmail(requestId, normalize(patientEmail)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Demande introuvable"));
  }

  private MedicalRecordAccessRequest professionalRequest(String professionalEmail, UUID requestId) {
    return accessRequests.findByIdAndHealthProfessionalEmail(requestId, normalize(professionalEmail)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Demande introuvable"));
  }

  private User patient(String email) {
    User user = users.findByEmail(normalize(email)).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide"));
    if (user.getRole() != Role.PATIENT || !user.isEnabled()) throw new ApiException(HttpStatus.FORBIDDEN, "Acces patient requis");
    return user;
  }

  private PatientSearchResultDto patientSearchDto(User patient) {
    HealthProfile profile = healthProfiles.findByUserId(patient.getId()).orElse(null);
    Integer birthYear = profile != null && profile.getBirthDate() != null ? profile.getBirthDate().getYear() : null;
    String gender = profile != null ? profile.getGender() : null;
    return new PatientSearchResultDto(patient.getId(), patient.getFirstName(), patient.getLastName(), gender, birthYear);
  }

  private MedicalRecordDto recordDto(MedicalRecord record) {
    User patient = record.getPatient();
    HealthProfile profile = healthProfiles.findByUserId(patient.getId()).orElse(null);
    Integer birthYear = profile != null && profile.getBirthDate() != null ? profile.getBirthDate().getYear() : null;
    String gender = profile != null ? profile.getGender() : null;
    return new MedicalRecordDto(
        record.getId(),
        patient.getId(),
        patient.getFirstName(),
        patient.getLastName(),
        gender,
        birthYear,
        historyEntries.findByMedicalRecordIdOrderByCreatedAtDesc(record.getId()).stream().map(this::historyDto).toList(),
        consultations.findByMedicalRecordIdAndDeletedFalseOrderByConsultationDateDesc(record.getId()).stream().map(this::consultationDto).toList());
  }

  private MedicalHistoryEntryDto historyDto(MedicalHistoryEntry entry) {
    return new MedicalHistoryEntryDto(entry.getId(), entry.getCategory().name(), entry.getTitle(), entry.getDescription(), entry.getStartDate(), entry.getEndDate(), entry.isActive(), entry.getCreatedAt(), entry.getUpdatedAt());
  }

  private MedicalConsultationDto consultationDto(MedicalConsultation consultation) {
    User pro = consultation.getHealthProfessional();
    return new MedicalConsultationDto(consultation.getId(), pro.getId(), displayName(pro), consultation.getConsultationDate(), consultation.getReason(), consultation.getDiagnosis(), consultation.getNotes(), consultation.getTreatment(), consultation.getFollowUpDate(), consultation.getCreatedAt(), consultation.getUpdatedAt());
  }

  private MedicalAccessRequestDto requestDto(MedicalRecordAccessRequest request) {
    MedicalAccessGrantDto grant = grants.findByAccessRequestId(request.getId()).map(this::grantDto).orElse(null);
    return new MedicalAccessRequestDto(request.getId(), request.getPatient().getId(), request.getHealthProfessional().getId(), displayName(request.getPatient()), displayName(request.getHealthProfessional()), request.getStatus().name(), request.getReason(), request.getRequestedAt(), request.getRespondedAt(), request.getCodeExpiresAt(), grant);
  }

  private MedicalAccessGrantDto grantDto(MedicalRecordAccessGrant grant) {
    return new MedicalAccessGrantDto(grant.getId(), grant.getPatient().getId(), grant.getHealthProfessional().getId(), grant.getActivatedAt(), grant.getExpiresAt(), grant.getStatus().name());
  }

  private void applyHistory(MedicalHistoryEntry entry, MedicalEnums.MedicalHistoryCategory category, String title, String description, java.time.LocalDate startDate, java.time.LocalDate endDate, Boolean active) {
    if (endDate != null && startDate != null && endDate.isBefore(startDate)) throw new ApiException(HttpStatus.BAD_REQUEST, "Date de fin incoherente");
    entry.setCategory(category);
    entry.setTitle(trimRequired(title, 140));
    entry.setDescription(trim(description, 2000));
    entry.setStartDate(startDate);
    entry.setEndDate(endDate);
    entry.setActive(active == null || active);
  }

  private void applyConsultation(MedicalConsultation consultation, Instant consultationDate, String reason, String diagnosis, String notes, String treatment, java.time.LocalDate followUpDate) {
    consultation.setConsultationDate(consultationDate == null ? Instant.now() : consultationDate);
    consultation.setReason(trimRequired(reason, 500));
    consultation.setDiagnosis(trim(diagnosis, 1200));
    consultation.setNotes(trim(notes, 3000));
    consultation.setTreatment(trim(treatment, 1200));
    consultation.setFollowUpDate(followUpDate);
  }

  private void audit(User patient, User actor, String eventType, String resourceType, UUID resourceId, String metadata) {
    MedicalAuditEvent event = new MedicalAuditEvent();
    event.setPatient(patient);
    event.setActor(actor);
    event.setActorRole(actor == null || actor.getRole() == null ? null : actor.getRole().name());
    event.setEventType(eventType);
    event.setResourceType(resourceType);
    event.setResourceId(resourceId);
    event.setMetadata(metadata);
    auditEvents.save(event);
  }

  private String buildCode() {
    StringBuilder code = new StringBuilder(8);
    for (int i = 0; i < 8; i++) code.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
    return code.toString();
  }

  private String trimRequired(String value, int max) {
    String clean = trim(value, max);
    if (clean == null || clean.length() < 2) throw new ApiException(HttpStatus.BAD_REQUEST, "Libelle requis");
    return clean;
  }

  private String trim(String value, int max) {
    if (value == null) return null;
    String clean = value.trim();
    if (clean.isEmpty()) return null;
    return clean.length() > max ? clean.substring(0, max) : clean;
  }

  private String normalize(String email) {
    return email.toLowerCase(Locale.ROOT).trim();
  }

  private String displayName(User user) {
    return ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
  }
}
