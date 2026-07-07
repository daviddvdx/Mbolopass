package ga.cyber241.mbolopass.common;

public final class Enums {
  private Enums() {}

  public enum Role { PATIENT, HEALTH_PROFESSIONAL, HEALTH_ADMIN }
  public enum AllergySeverity { LOW, MEDIUM, HIGH, CRITICAL }
  public enum MedicalConditionStatus { ACTIVE, HISTORICAL }
  public enum VaccinationStatus { COMPLETED, UPCOMING, OVERDUE }
  public enum QrTokenStatus { ACTIVE, REVOKED, EXPIRED }
  public enum EmergencyAccessType { QR_EMERGENCY, NFC_EMERGENCY, PROFESSIONAL_ACCESS }
  public enum EmergencyAccessOutcome { GRANTED, DENIED, EXPIRED, REVOKED }
  public enum AlertType { PROFILE_INCOMPLETE, VACCINATION_DUE, MEDICATION_RENEWAL, MEDICAL_FOLLOW_UP, ALLERGY_MEDICATION_WARNING }
  public enum AlertSeverity { INFO, WARNING, CRITICAL }
  public enum AlertStatus { ACTIVE, DISMISSED, RESOLVED }
  public enum EmergencySubjectType { PATIENT, DEPENDENT }
  public enum DocumentOwnerType { PATIENT, DEPENDENT }
  public enum MedicalDocumentCategory { PRESCRIPTION, MEDICAL_RECORD, LAB_RESULT, CERTIFICATE, OTHER }
  public enum ProfessionalType { PHYSICIAN, NURSE, LAB_TECHNICIAN, PHARMACIST, OTHER }
  public enum ProfessionalVerificationStatus { PENDING, APPROVED, REJECTED, SUSPENDED }
  public enum PatientProfessionalAccessStatus { PENDING, ACTIVE, REVOKED, EXPIRED, EMERGENCY }
  public enum ClinicalEncounterStatus { DRAFT, IN_PROGRESS, CLOSED, CANCELLED }
  public enum ClinicalDataSource { PATIENT_REPORTED, HEALTH_PROFESSIONAL }
  public enum ClinicalVerificationStatus { DRAFT, VALIDATED, REJECTED, AMENDED }
  public enum ClinicalStatus { ACTIVE, HISTORICAL, RESOLVED, SUSPECTED }
  public enum MedicalReferenceCategory { DISEASE, PATHOLOGY, CONDITION, ALLERGY, EXAM }
  public enum ExamOrderStatus { DRAFT, ORDERED, SCHEDULED, COLLECTED, RESULT_AVAILABLE, VALIDATED, CANCELLED }
  public enum ExamPriority { ROUTINE, URGENT }
  public enum ExamResultStatus { DRAFT, ENTERED, VALIDATED, REJECTED }
  public enum ClinicalProtocolStatus { DRAFT, ACTIVE, RETIRED }
  public enum ClinicalProtocolRunStatus { IN_PROGRESS, COMPLETED, CANCELLED }
}
