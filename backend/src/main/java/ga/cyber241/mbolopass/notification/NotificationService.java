package ga.cyber241.mbolopass.notification;

import ga.cyber241.mbolopass.admin.dto.AdminDtos.PageResponse;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository notifications;
  private final UserRepository users;

  public NotificationService(NotificationRepository notifications, UserRepository users) {
    this.notifications = notifications;
    this.users = users;
  }

  @Transactional
  public void create(User recipient, NotificationType type, String title, String message, String referenceType, UUID referenceId) {
    Notification notification = new Notification();
    notification.setRecipient(recipient);
    notification.setType(type);
    notification.setTitle(title);
    notification.setMessage(message);
    notification.setReferenceType(referenceType);
    notification.setReferenceId(referenceId);
    notifications.save(notification);
  }

  @Transactional(readOnly = true)
  public PageResponse<NotificationDto> list(String email, int page, int size) {
    User user = currentUser(email);
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 25);
    Page<Notification> result = notifications.findByRecipientEmailOrderByCreatedAtDesc(user.getEmail(), PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending()));
    return new PageResponse<>(result.getContent().stream().map(this::dto).toList(), safePage, safeSize, result.getTotalElements(), result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public UnreadCountDto unreadCount(String email) {
    User user = currentUser(email);
    return new UnreadCountDto(notifications.countByRecipientEmailAndReadFalse(user.getEmail()));
  }

  @Transactional
  public NotificationDto markRead(String email, UUID notificationId) {
    User user = currentUser(email);
    Notification notification = notifications.findByIdAndRecipientEmail(notificationId, user.getEmail()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification introuvable"));
    if (!notification.isRead()) {
      notification.setRead(true);
      notification.setReadAt(Instant.now());
      notifications.save(notification);
    }
    return dto(notification);
  }

  @Transactional
  public void markAllRead(String email) {
    User user = currentUser(email);
    notifications.markAllRead(user.getEmail(), Instant.now());
  }

  private User currentUser(String email) {
    return users.findByEmail(email.toLowerCase(Locale.ROOT)).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide"));
  }

  private NotificationDto dto(Notification notification) {
    return new NotificationDto(notification.getId(), notification.getType().name(), notification.getTitle(), notification.getMessage(), notification.getReferenceType(), notification.getReferenceId(), notification.isRead(), notification.getReadAt(), notification.getCreatedAt());
  }

  public record NotificationDto(UUID id, String type, String title, String message, String referenceType, UUID referenceId, boolean read, Instant readAt, Instant createdAt) {}
  public record UnreadCountDto(long count) {}
}
