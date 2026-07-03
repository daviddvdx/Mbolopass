package ga.cyber241.mbolopass.health;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, UUID> {
  Optional<HealthProfile> findByUserEmail(String email);
  Optional<HealthProfile> findByUserId(UUID userId);

  @Query("select hp.user.id from HealthProfile hp where hp.user.id in :userIds")
  Set<UUID> findUserIdsWithProfile(Set<UUID> userIds);
}
