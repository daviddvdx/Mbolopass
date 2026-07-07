package ga.cyber241.mbolopass.clinical;

import ga.cyber241.mbolopass.common.Enums.*;
import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@MappedSuperclass
abstract class ClinicalTimedEntity extends UuidEntity {
  private Instant createdAt;
  private Instant updatedAt;
  @PrePersist
  public void onCreate() {
    super.ensureId();
    createdAt = Instant.now();
    updatedAt = createdAt;
  }
  @PreUpdate public void onUpdate() { updatedAt = Instant.now(); }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

@Entity
@Table(name = "professional_profiles")
class ProfessionalProfile extends ClinicalTimedEntity {
  @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;
  @Enumerated(EnumType.STRING) private ProfessionalType professionalType;
  private String speciality;
  private String licenseNumber;
  private String organizationName;
  @Enumerated(EnumType.STRING) private ProfessionalVerificationStatus verificationStatus = ProfessionalVerificationStatus.PENDING;
  private Instant verifiedAt;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "verified_by_user_id")
  private User verifiedBy;
  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }
  public ProfessionalType getProfessionalType() { return professionalType; }
  public void setProfessionalType(ProfessionalType professionalType) { this.professionalType = professionalType; }
  public String getSpeciality() { return speciality; }
  public void setSpeciality(String speciality) { this.speciality = speciality; }
  public String getLicenseNumber() { return licenseNumber; }
  public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
  public String getOrganizationName() { return organizationName; }
  public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
  public ProfessionalVerificationStatus getVerificationStatus() { return verificationStatus; }
  public void setVerificationStatus(ProfessionalVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
  public Instant getVerifiedAt() { return verifiedAt; }
  public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
  public User getVerifiedBy() { return verifiedBy; }
  public void setVerifiedBy(User verifiedBy) { this.verifiedBy = verifiedBy; }
}

@Entity
@Table(name = "patient_professional_access")
class PatientProfessionalAccess extends ClinicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id", nullable = false)
  private HealthProfile healthProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professional_profile_id", nullable = false)
  private ProfessionalProfile professionalProfile;
  @Enumerated(EnumType.STRING) private PatientProfessionalAccessStatus status = PatientProfessionalAccessStatus.PENDING;
  private String accessReason;
  private Instant grantedAt;
  private Instant expiresAt;
  private Instant revokedAt;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "granted_by_user_id")
  private User grantedBy;
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public ProfessionalProfile getProfessionalProfile() { return professionalProfile; }
  public void setProfessionalProfile(ProfessionalProfile professionalProfile) { this.professionalProfile = professionalProfile; }
  public PatientProfessionalAccessStatus getStatus() { return status; }
  public void setStatus(PatientProfessionalAccessStatus status) { this.status = status; }
  public String getAccessReason() { return accessReason; }
  public void setAccessReason(String accessReason) { this.accessReason = accessReason; }
  public Instant getGrantedAt() { return grantedAt; }
  public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
  public User getGrantedBy() { return grantedBy; }
  public void setGrantedBy(User grantedBy) { this.grantedBy = grantedBy; }
}

@Entity
@Table(name = "clinical_encounters")
class ClinicalEncounter extends ClinicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id", nullable = false)
  private HealthProfile healthProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professional_profile_id", nullable = false)
  private ProfessionalProfile professionalProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_access_id", nullable = false)
  private PatientProfessionalAccess patientAccess;
  private String encounterType;
  private String reason;
  @Enumerated(EnumType.STRING) private ClinicalEncounterStatus status = ClinicalEncounterStatus.IN_PROGRESS;
  private String clinicalNotes;
  private Instant startedAt;
  private Instant closedAt;
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public ProfessionalProfile getProfessionalProfile() { return professionalProfile; }
  public void setProfessionalProfile(ProfessionalProfile professionalProfile) { this.professionalProfile = professionalProfile; }
  public PatientProfessionalAccess getPatientAccess() { return patientAccess; }
  public void setPatientAccess(PatientProfessionalAccess patientAccess) { this.patientAccess = patientAccess; }
  public String getEncounterType() { return encounterType; }
  public void setEncounterType(String encounterType) { this.encounterType = encounterType; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public ClinicalEncounterStatus getStatus() { return status; }
  public void setStatus(ClinicalEncounterStatus status) { this.status = status; }
  public String getClinicalNotes() { return clinicalNotes; }
  public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
  public Instant getClosedAt() { return closedAt; }
  public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}

@Entity
@Table(name = "medical_reference_catalog")
class MedicalReferenceCatalog extends ClinicalTimedEntity {
  @Enumerated(EnumType.STRING) private MedicalReferenceCategory category;
  private String codeSystem = "INTERNAL";
  private String code;
  private String displayName;
  private boolean active = true;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id")
  private User createdBy;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "validated_by_user_id")
  private User validatedBy;
  public MedicalReferenceCategory getCategory() { return category; }
  public void setCategory(MedicalReferenceCategory category) { this.category = category; }
  public String getCodeSystem() { return codeSystem; }
  public void setCodeSystem(String codeSystem) { this.codeSystem = codeSystem; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public User getCreatedBy() { return createdBy; }
  public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
  public User getValidatedBy() { return validatedBy; }
  public void setValidatedBy(User validatedBy) { this.validatedBy = validatedBy; }
}

@Entity
@Table(name = "exam_catalog")
class ExamCatalog extends ClinicalTimedEntity {
  private String code;
  private String name;
  private String category;
  private String description;
  private boolean active = true;
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
}

@Entity
@Table(name = "exam_orders")
class ExamOrder extends ClinicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id", nullable = false) private HealthProfile healthProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "clinical_encounter_id", nullable = false) private ClinicalEncounter clinicalEncounter;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ordered_by_professional_id", nullable = false) private ProfessionalProfile orderedBy;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "exam_catalog_id", nullable = false) private ExamCatalog examCatalog;
  @Enumerated(EnumType.STRING) private ExamOrderStatus status = ExamOrderStatus.ORDERED;
  @Enumerated(EnumType.STRING) private ExamPriority priority = ExamPriority.ROUTINE;
  private String clinicalReason;
  private Instant orderedAt;
  private Instant scheduledAt;
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public ClinicalEncounter getClinicalEncounter() { return clinicalEncounter; }
  public void setClinicalEncounter(ClinicalEncounter clinicalEncounter) { this.clinicalEncounter = clinicalEncounter; }
  public ProfessionalProfile getOrderedBy() { return orderedBy; }
  public void setOrderedBy(ProfessionalProfile orderedBy) { this.orderedBy = orderedBy; }
  public ExamCatalog getExamCatalog() { return examCatalog; }
  public void setExamCatalog(ExamCatalog examCatalog) { this.examCatalog = examCatalog; }
  public ExamOrderStatus getStatus() { return status; }
  public void setStatus(ExamOrderStatus status) { this.status = status; }
  public ExamPriority getPriority() { return priority; }
  public void setPriority(ExamPriority priority) { this.priority = priority; }
  public String getClinicalReason() { return clinicalReason; }
  public void setClinicalReason(String clinicalReason) { this.clinicalReason = clinicalReason; }
  public Instant getOrderedAt() { return orderedAt; }
  public void setOrderedAt(Instant orderedAt) { this.orderedAt = orderedAt; }
  public Instant getScheduledAt() { return scheduledAt; }
  public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
}

@Entity
@Table(name = "exam_results")
class ExamResult extends ClinicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "exam_order_id", nullable = false) private ExamOrder examOrder;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "entered_by_professional_id", nullable = false) private ProfessionalProfile enteredBy;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "validated_by_professional_id") private ProfessionalProfile validatedBy;
  private String resultSummary;
  private String resultValue;
  private String unit;
  private String referenceRange;
  @Enumerated(EnumType.STRING) private ExamResultStatus status = ExamResultStatus.ENTERED;
  private Instant resultedAt;
  private Instant validatedAt;
  public ExamOrder getExamOrder() { return examOrder; }
  public void setExamOrder(ExamOrder examOrder) { this.examOrder = examOrder; }
  public ProfessionalProfile getEnteredBy() { return enteredBy; }
  public void setEnteredBy(ProfessionalProfile enteredBy) { this.enteredBy = enteredBy; }
  public ProfessionalProfile getValidatedBy() { return validatedBy; }
  public void setValidatedBy(ProfessionalProfile validatedBy) { this.validatedBy = validatedBy; }
  public String getResultSummary() { return resultSummary; }
  public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
  public String getResultValue() { return resultValue; }
  public void setResultValue(String resultValue) { this.resultValue = resultValue; }
  public String getUnit() { return unit; }
  public void setUnit(String unit) { this.unit = unit; }
  public String getReferenceRange() { return referenceRange; }
  public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
  public ExamResultStatus getStatus() { return status; }
  public void setStatus(ExamResultStatus status) { this.status = status; }
  public Instant getResultedAt() { return resultedAt; }
  public void setResultedAt(Instant resultedAt) { this.resultedAt = resultedAt; }
  public Instant getValidatedAt() { return validatedAt; }
  public void setValidatedAt(Instant validatedAt) { this.validatedAt = validatedAt; }
}

@Entity
@Table(name = "clinical_audit_events")
class ClinicalAuditEvent extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_user_id") private User actor;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_professional_profile_id") private ProfessionalProfile actorProfessional;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id") private HealthProfile healthProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "clinical_encounter_id") private ClinicalEncounter clinicalEncounter;
  private String action;
  private String resourceType;
  private java.util.UUID resourceId;
  private String accessReason;
  private String ipAddress;
  private String userAgent;
  private Instant createdAt;
  @PrePersist public void onCreate() { ensureId(); createdAt = Instant.now(); }
  public void setActor(User actor) { this.actor = actor; }
  public void setActorProfessional(ProfessionalProfile actorProfessional) { this.actorProfessional = actorProfessional; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public void setClinicalEncounter(ClinicalEncounter clinicalEncounter) { this.clinicalEncounter = clinicalEncounter; }
  public void setAction(String action) { this.action = action; }
  public void setResourceType(String resourceType) { this.resourceType = resourceType; }
  public void setResourceId(java.util.UUID resourceId) { this.resourceId = resourceId; }
  public void setAccessReason(String accessReason) { this.accessReason = accessReason; }
  public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
  public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
  public Instant getCreatedAt() { return createdAt; }
}

@Entity @Table(name = "clinical_protocols")
class ClinicalProtocol extends ClinicalTimedEntity {
  private String name; private String description; private String targetCategory; private String version;
  @Enumerated(EnumType.STRING) private ClinicalProtocolStatus status = ClinicalProtocolStatus.DRAFT;
}
@Entity @Table(name = "clinical_protocol_steps")
class ClinicalProtocolStep extends ClinicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "clinical_protocol_id") private ClinicalProtocol protocol;
  private int stepOrder; private String title; private String description; private boolean required;
}
@Entity @Table(name = "clinical_protocol_runs")
class ClinicalProtocolRun extends ClinicalTimedEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "clinical_protocol_id") private ClinicalProtocol protocol;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "clinical_encounter_id") private ClinicalEncounter encounter;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id") private HealthProfile healthProfile;
  @Enumerated(EnumType.STRING) private ClinicalProtocolRunStatus status = ClinicalProtocolRunStatus.IN_PROGRESS;
  private Instant startedAt; private Instant completedAt;
}
