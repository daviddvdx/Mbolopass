package ga.cyber241.mbolopass.prevention;

import ga.cyber241.mbolopass.common.Enums.AlertStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PreventionRepository extends JpaRepository<PreventionAlert, UUID> {
  List<PreventionAlert> findByHealthProfileIdOrderByCreatedAtDesc(UUID healthProfileId);
  void deleteByHealthProfileIdAndStatus(UUID healthProfileId, AlertStatus status);
}
