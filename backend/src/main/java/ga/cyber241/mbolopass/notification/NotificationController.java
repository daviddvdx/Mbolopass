package ga.cyber241.mbolopass.notification;

import ga.cyber241.mbolopass.admin.dto.AdminDtos.PageResponse;
import ga.cyber241.mbolopass.notification.NotificationService.NotificationDto;
import ga.cyber241.mbolopass.notification.NotificationService.UnreadCountDto;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
  private final NotificationService notifications;

  public NotificationController(NotificationService notifications) {
    this.notifications = notifications;
  }

  @GetMapping
  public PageResponse<NotificationDto> list(Principal principal, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return notifications.list(principal.getName(), page, size);
  }

  @GetMapping("/unread-count")
  public UnreadCountDto unreadCount(Principal principal) {
    return notifications.unreadCount(principal.getName());
  }

  @PatchMapping("/{notificationId}/read")
  public NotificationDto read(Principal principal, @PathVariable UUID notificationId) {
    return notifications.markRead(principal.getName(), notificationId);
  }

  @PatchMapping("/read-all")
  public void readAll(Principal principal) {
    notifications.markAllRead(principal.getName());
  }
}
