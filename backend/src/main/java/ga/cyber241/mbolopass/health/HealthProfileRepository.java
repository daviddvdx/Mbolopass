package ga.cyber241.mbolopass.health;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, UUID> {
  Optional<HealthProfile> findByUserEmail(String email);
  Optional<HealthProfile> findByUserId(UUID userId);
  Optional<HealthProfile> findByEmergencyQrPublicId(UUID emergencyQrPublicId);
  boolean existsByCardNumber(String cardNumber);
  boolean existsByEmergencyQrPublicId(UUID emergencyQrPublicId);
  boolean existsByEmergencyQrReference(String emergencyQrReference);
  long countByCardNumberIsNull();

  @Query("select hp.user.id from HealthProfile hp where hp.user.id in :userIds")
  Set<UUID> findUserIdsWithProfile(Set<UUID> userIds);

  @Query("select hp.id from HealthProfile hp where hp.cardNumber is null")
  Set<UUID> findIdsMissingCardNumber();

  @Modifying
  @Query("update HealthProfile hp set hp.cardNumber = :cardNumber where hp.id = :id and hp.cardNumber is null")
  int assignMissingCardNumber(@Param("id") UUID id, @Param("cardNumber") String cardNumber);
}
