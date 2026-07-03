package ga.cyber241.mbolopass.ai;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AiSummaryRepository extends JpaRepository<AiSummary, UUID> {
  Optional<AiSummary> findFirstByHealthProfileIdOrderByGeneratedAtDesc(UUID healthProfileId);
}
