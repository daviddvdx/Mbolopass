package ga.cyber241.mbolopass.emergency;

import ga.cyber241.mbolopass.common.Enums.QrTokenStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface QrTokenRepository extends JpaRepository<QrToken, UUID> {
  Optional<QrToken> findByTokenHash(String tokenHash);
  List<QrToken> findByHealthProfileIdAndStatus(UUID profileId, QrTokenStatus status);
  List<QrToken> findByDependentProfileIdAndStatus(UUID dependentId, QrTokenStatus status);
}

interface EmergencyAccessLogRepository extends JpaRepository<EmergencyAccessLog, UUID> {}
