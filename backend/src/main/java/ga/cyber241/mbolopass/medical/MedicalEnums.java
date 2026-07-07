package ga.cyber241.mbolopass.medical;

public final class MedicalEnums {
  private MedicalEnums() {}

  public enum MedicalHistoryCategory { PERSONAL_HISTORY, FAMILY_HISTORY, ALLERGY, CHRONIC_CONDITION, SURGERY, MEDICATION, OTHER }
  public enum MedicalAccessRequestStatus { PENDING, APPROVED, DENIED, ACTIVE, EXPIRED, REVOKED, LOCKED }
  public enum MedicalAccessGrantStatus { ACTIVE, EXPIRED, REVOKED }
}
