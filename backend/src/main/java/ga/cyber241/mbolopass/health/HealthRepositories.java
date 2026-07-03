package ga.cyber241.mbolopass.health;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {
  List<EmergencyContact> findByHealthProfileId(UUID profileId);
  Optional<EmergencyContact> findFirstByHealthProfileIdAndIsPrimaryTrue(UUID profileId);
  boolean existsByHealthProfileId(UUID profileId);
}

interface AllergyRepository extends JpaRepository<Allergy, UUID> {
  List<Allergy> findByHealthProfileId(UUID profileId);
  boolean existsByHealthProfileId(UUID profileId);
}

interface MedicalConditionRepository extends JpaRepository<MedicalCondition, UUID> {
  List<MedicalCondition> findByHealthProfileId(UUID profileId);
  boolean existsByHealthProfileId(UUID profileId);
}

interface MedicationRepository extends JpaRepository<Medication, UUID> {
  List<Medication> findByHealthProfileId(UUID profileId);
  boolean existsByHealthProfileId(UUID profileId);
}

interface VaccinationRepository extends JpaRepository<Vaccination, UUID> {
  List<Vaccination> findByHealthProfileId(UUID profileId);
}
