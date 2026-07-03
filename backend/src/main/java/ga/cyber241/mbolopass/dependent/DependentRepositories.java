package ga.cyber241.mbolopass.dependent;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DependentProfileRepository extends JpaRepository<DependentProfile, UUID> {
  List<DependentProfile> findByGuardianIdAndEnabledTrue(UUID guardianId);
  long countByEnabledTrue();
}

interface DependentAllergyRepository extends JpaRepository<DependentAllergy, UUID> {
  List<DependentAllergy> findByDependentProfileId(UUID dependentId);
}

interface DependentMedicalConditionRepository extends JpaRepository<DependentMedicalCondition, UUID> {
  List<DependentMedicalCondition> findByDependentProfileId(UUID dependentId);
}

interface DependentMedicationRepository extends JpaRepository<DependentMedication, UUID> {
  List<DependentMedication> findByDependentProfileId(UUID dependentId);
}

interface DependentEmergencyContactRepository extends JpaRepository<DependentEmergencyContact, UUID> {
  List<DependentEmergencyContact> findByDependentProfileId(UUID dependentId);
}
