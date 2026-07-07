package ga.cyber241.mbolopass.clinical;

import ga.cyber241.mbolopass.common.Enums.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, UUID> {
  Optional<ProfessionalProfile> findByUserEmail(String email);
  Optional<ProfessionalProfile> findByUserId(UUID userId);
  List<ProfessionalProfile> findByVerificationStatus(ProfessionalVerificationStatus status);
}

interface PatientProfessionalAccessRepository extends JpaRepository<PatientProfessionalAccess, UUID> {
  List<PatientProfessionalAccess> findByProfessionalProfileId(UUID professionalProfileId);
  List<PatientProfessionalAccess> findByHealthProfileUserEmail(String email);
  Optional<PatientProfessionalAccess> findFirstByHealthProfileIdAndProfessionalProfileIdAndStatusAndExpiresAtAfter(UUID healthProfileId, UUID professionalProfileId, PatientProfessionalAccessStatus status, Instant now);
}

interface ClinicalEncounterRepository extends JpaRepository<ClinicalEncounter, UUID> {
  List<ClinicalEncounter> findByHealthProfileId(UUID healthProfileId);
}

interface MedicalReferenceCatalogRepository extends JpaRepository<MedicalReferenceCatalog, UUID> {
  List<MedicalReferenceCatalog> findByActiveTrue();
}

interface ExamCatalogRepository extends JpaRepository<ExamCatalog, UUID> {
  List<ExamCatalog> findByActiveTrue();
}

interface ExamOrderRepository extends JpaRepository<ExamOrder, UUID> {
  List<ExamOrder> findByClinicalEncounterId(UUID encounterId);
}

interface ExamResultRepository extends JpaRepository<ExamResult, UUID> {
  List<ExamResult> findByExamOrderId(UUID examOrderId);
}

interface ClinicalAuditEventRepository extends JpaRepository<ClinicalAuditEvent, UUID> {
  long countByAction(String action);
}

interface ClinicalProtocolRepository extends JpaRepository<ClinicalProtocol, UUID> {}
interface ClinicalProtocolStepRepository extends JpaRepository<ClinicalProtocolStep, UUID> {}
interface ClinicalProtocolRunRepository extends JpaRepository<ClinicalProtocolRun, UUID> {}
