package ga.cyber241.mbolopass.admin.service;

import ga.cyber241.mbolopass.common.Enums.QrTokenStatus;
import ga.cyber241.mbolopass.emergency.QrToken;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AdminQrTokenRepository extends JpaRepository<QrToken, UUID> {
  long countByStatus(QrTokenStatus status);
  List<QrToken> findByHealthProfileUserIdAndStatus(UUID userId, QrTokenStatus status);
  long countByDependentProfileIsNotNullAndStatus(QrTokenStatus status);

  @Query("select q.healthProfile.user.id from QrToken q where q.healthProfile.user.id in :userIds and q.status = :status")
  Set<UUID> findHealthProfileUserIdsWithStatus(Set<UUID> userIds, QrTokenStatus status);
}
