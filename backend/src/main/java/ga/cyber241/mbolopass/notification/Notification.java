package ga.cyber241.mbolopass.notification;

import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_user_id", nullable = false)
  private User recipient;
  @Enumerated(EnumType.STRING)
  private NotificationType type;
  private String title;
  private String message;
  private String referenceType;
  private UUID referenceId;
  private boolean read;
  private Instant readAt;
  private Instant createdAt;

  @PrePersist
  public void onCreate() {
    ensureId();
    createdAt = Instant.now();
  }

  public User getRecipient() { return recipient; }
  public void setRecipient(User recipient) { this.recipient = recipient; }
  public NotificationType getType() { return type; }
  public void setType(NotificationType type) { this.type = type; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public String getReferenceType() { return referenceType; }
  public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
  public UUID getReferenceId() { return referenceId; }
  public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }
  public boolean isRead() { return read; }
  public void setRead(boolean read) { this.read = read; }
  public Instant getReadAt() { return readAt; }
  public void setReadAt(Instant readAt) { this.readAt = readAt; }
  public Instant getCreatedAt() { return createdAt; }
}
