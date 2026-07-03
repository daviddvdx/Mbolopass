package ga.cyber241.mbolopass.common;

public final class Enums {
  private Enums() {}

  public enum Role { PATIENT, PROFESSIONAL, ADMIN }
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
}
