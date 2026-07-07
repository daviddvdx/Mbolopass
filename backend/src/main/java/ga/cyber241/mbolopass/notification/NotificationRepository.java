package ga.cyber241.mbolopass.notification;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String email, Pageable pageable);
  long countByRecipientEmailAndReadFalse(String email);
  Optional<Notification> findByIdAndRecipientEmail(UUID id, String email);

  @Modifying
  @Query("update Notification n set n.read = true, n.readAt = :readAt where n.recipient.email = :email and n.read = false")
  int markAllRead(@Param("email") String email, @Param("readAt") Instant readAt);
}
