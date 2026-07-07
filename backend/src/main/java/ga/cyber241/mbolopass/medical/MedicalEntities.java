package ga.cyber241.mbolopass.medical;

import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.medical.MedicalEnums.*;
import ga.cyber241.mbolopass.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@MappedSuperclass
abstract class MedicalTimedEntity extends UuidEntity {
  private Instant createdAt;
  private Instant updatedAt;
  @Version private long version;
  @PrePersist public void onCreate() { ensureId(); createdAt = Instant.now(); updatedAt = createdAt; }
  @PreUpdate public void onUpdate() { updatedAt = Instant.now(); }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public long getVersion() { return version; }
}

@Entity
@Table(name = "medical_records")
class MedicalRecord extends MedicalTimedEntity {
  @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_id", nullable = false, unique = true)
  private User patient;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by")
  private User createdBy;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
  private User updatedBy;
  public User getPatient() { return patient; }
  public void setPatient(User patient) { this.patient = patient; }
  public User getCreatedBy() { return createdBy; }
  public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
  public User getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }
}

@Entity
@Table(name = "medical_history_entries")
class MedicalHistoryEntry extends MedicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "medical_record_id", nullable = false)
  private MedicalRecord medicalRecord;
  @Enumerated(EnumType.STRING) private MedicalHistoryCategory category;
  @Column(length = 140) private String title;
  @Column(length = 2000) private String description;
  private LocalDate startDate;
  private LocalDate endDate;
  private boolean active = true;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_professional_id")
  private User createdByProfessional;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by_professional_id")
  private User updatedByProfessional;
  public MedicalRecord getMedicalRecord() { return medicalRecord; }
  public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }
  public MedicalHistoryCategory getCategory() { return category; }
  public void setCategory(MedicalHistoryCategory category) { this.category = category; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public User getCreatedByProfessional() { return createdByProfessional; }
  public void setCreatedByProfessional(User createdByProfessional) { this.createdByProfessional = createdByProfessional; }
  public User getUpdatedByProfessional() { return updatedByProfessional; }
  public void setUpdatedByProfessional(User updatedByProfessional) { this.updatedByProfessional = updatedByProfessional; }
}

@Entity
@Table(name = "medical_consultations")
class MedicalConsultation extends MedicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "medical_record_id", nullable = false)
  private MedicalRecord medicalRecord;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_professional_id", nullable = false)
  private User healthProfessional;
  private Instant consultationDate;
  @Column(length = 500) private String reason;
  @Column(length = 1200) private String diagnosis;
  @Column(length = 3000) private String notes;
  @Column(length = 1200) private String treatment;
  private LocalDate followUpDate;
  private boolean deleted = false;
  public MedicalRecord getMedicalRecord() { return medicalRecord; }
  public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }
  public User getHealthProfessional() { return healthProfessional; }
  public void setHealthProfessional(User healthProfessional) { this.healthProfessional = healthProfessional; }
  public Instant getConsultationDate() { return consultationDate; }
  public void setConsultationDate(Instant consultationDate) { this.consultationDate = consultationDate; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getDiagnosis() { return diagnosis; }
  public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public String getTreatment() { return treatment; }
  public void setTreatment(String treatment) { this.treatment = treatment; }
  public LocalDate getFollowUpDate() { return followUpDate; }
  public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
  public boolean isDeleted() { return deleted; }
  public void setDeleted(boolean deleted) { this.deleted = deleted; }
}

@Entity
@Table(name = "medical_record_access_requests")
class MedicalRecordAccessRequest extends MedicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_id", nullable = false)
  private User patient;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_professional_id", nullable = false)
  private User healthProfessional;
  @Enumerated(EnumType.STRING) private MedicalAccessRequestStatus status = MedicalAccessRequestStatus.PENDING;
  @Column(length = 500) private String reason;
  private Instant requestedAt;
  private Instant respondedAt;
  private Instant approvedAt;
  private Instant deniedAt;
  private Instant revokedAt;
  @Column(length = 120) private String codeHash;
  private Instant codeExpiresAt;
  private Instant codeUsedAt;
  private int failedCodeAttempts;
  private Instant lockedUntil;
  public User getPatient() { return patient; }
  public void setPatient(User patient) { this.patient = patient; }
  public User getHealthProfessional() { return healthProfessional; }
  public void setHealthProfessional(User healthProfessional) { this.healthProfessional = healthProfessional; }
  public MedicalAccessRequestStatus getStatus() { return status; }
  public void setStatus(MedicalAccessRequestStatus status) { this.status = status; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Instant getRequestedAt() { return requestedAt; }
  public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
  public Instant getRespondedAt() { return respondedAt; }
  public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
  public Instant getApprovedAt() { return approvedAt; }
  public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
  public Instant getDeniedAt() { return deniedAt; }
  public void setDeniedAt(Instant deniedAt) { this.deniedAt = deniedAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
  public String getCodeHash() { return codeHash; }
  public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
  public Instant getCodeExpiresAt() { return codeExpiresAt; }
  public void setCodeExpiresAt(Instant codeExpiresAt) { this.codeExpiresAt = codeExpiresAt; }
  public Instant getCodeUsedAt() { return codeUsedAt; }
  public void setCodeUsedAt(Instant codeUsedAt) { this.codeUsedAt = codeUsedAt; }
  public int getFailedCodeAttempts() { return failedCodeAttempts; }
  public void setFailedCodeAttempts(int failedCodeAttempts) { this.failedCodeAttempts = failedCodeAttempts; }
  public Instant getLockedUntil() { return lockedUntil; }
  public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
}

@Entity
@Table(name = "medical_record_access_grants")
class MedicalRecordAccessGrant extends MedicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_id", nullable = false)
  private User patient;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_professional_id", nullable = false)
  private User healthProfessional;
  @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "access_request_id", nullable = false, unique = true)
  private MedicalRecordAccessRequest accessRequest;
  private Instant activatedAt;
  private Instant expiresAt;
  private Instant revokedAt;
  @Enumerated(EnumType.STRING) private MedicalAccessGrantStatus status = MedicalAccessGrantStatus.ACTIVE;
  public User getPatient() { return patient; }
  public void setPatient(User patient) { this.patient = patient; }
  public User getHealthProfessional() { return healthProfessional; }
  public void setHealthProfessional(User healthProfessional) { this.healthProfessional = healthProfessional; }
  public MedicalRecordAccessRequest getAccessRequest() { return accessRequest; }
  public void setAccessRequest(MedicalRecordAccessRequest accessRequest) { this.accessRequest = accessRequest; }
  public Instant getActivatedAt() { return activatedAt; }
  public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
  public MedicalAccessGrantStatus getStatus() { return status; }
  public void setStatus(MedicalAccessGrantStatus status) { this.status = status; }
}

@Entity
@Table(name = "medical_audit_events")
class MedicalAuditEvent extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_id") private User patient;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_user_id") private User actor;
  private String actorRole;
  private String eventType;
  private String resourceType;
  private java.util.UUID resourceId;
  @Column(length = 1000) private String metadata;
  private Instant createdAt;
  private String ipAddress;
  private String userAgent;
  @PrePersist public void onCreate() { ensureId(); createdAt = Instant.now(); }
  public void setPatient(User patient) { this.patient = patient; }
  public void setActor(User actor) { this.actor = actor; }
  public void setActorRole(String actorRole) { this.actorRole = actorRole; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public void setResourceType(String resourceType) { this.resourceType = resourceType; }
  public void setResourceId(java.util.UUID resourceId) { this.resourceId = resourceId; }
  public void setMetadata(String metadata) { this.metadata = metadata; }
  public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
  public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
  public Instant getCreatedAt() { return createdAt; }
}
