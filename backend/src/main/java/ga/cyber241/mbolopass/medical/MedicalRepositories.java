package ga.cyber241.mbolopass.medical;

import ga.cyber241.mbolopass.medical.MedicalEnums.MedicalAccessGrantStatus;
import ga.cyber241.mbolopass.medical.MedicalEnums.MedicalAccessRequestStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
  Optional<MedicalRecord> findByPatientId(UUID patientId);
  Optional<MedicalRecord> findByPatientEmail(String email);
  boolean existsByPatientId(UUID patientId);
}

interface MedicalHistoryEntryRepository extends JpaRepository<MedicalHistoryEntry, UUID> {
  List<MedicalHistoryEntry> findByMedicalRecordIdOrderByCreatedAtDesc(UUID medicalRecordId);
  Optional<MedicalHistoryEntry> findByIdAndMedicalRecordId(UUID id, UUID medicalRecordId);
}

interface MedicalConsultationRepository extends JpaRepository<MedicalConsultation, UUID> {
  List<MedicalConsultation> findByMedicalRecordIdAndDeletedFalseOrderByConsultationDateDesc(UUID medicalRecordId);
  Optional<MedicalConsultation> findByIdAndMedicalRecordIdAndDeletedFalse(UUID id, UUID medicalRecordId);
}

interface MedicalRecordAccessRequestRepository extends JpaRepository<MedicalRecordAccessRequest, UUID> {
  List<MedicalRecordAccessRequest> findByPatientEmailOrderByRequestedAtDesc(String email);
  List<MedicalRecordAccessRequest> findByHealthProfessionalEmailOrderByRequestedAtDesc(String email);
  Optional<MedicalRecordAccessRequest> findByIdAndPatientEmail(UUID id, String email);
  Optional<MedicalRecordAccessRequest> findByIdAndHealthProfessionalEmail(UUID id, String email);
  boolean existsByPatientIdAndHealthProfessionalIdAndStatusIn(UUID patientId, UUID professionalId, Collection<MedicalAccessRequestStatus> statuses);
}

interface MedicalRecordAccessGrantRepository extends JpaRepository<MedicalRecordAccessGrant, UUID> {
  Optional<MedicalRecordAccessGrant> findFirstByPatientIdAndHealthProfessionalIdAndStatusAndExpiresAtAfter(UUID patientId, UUID professionalId, MedicalAccessGrantStatus status, Instant now);
  Optional<MedicalRecordAccessGrant> findByAccessRequestId(UUID accessRequestId);
  List<MedicalRecordAccessGrant> findByPatientEmailOrderByCreatedAtDesc(String email);
}

interface MedicalAuditEventRepository extends JpaRepository<MedicalAuditEvent, UUID> {}
