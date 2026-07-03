package ga.cyber241.mbolopass.admin.audit;

import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id", nullable = false)
  private User actor;
  @Column(nullable = false, length = 80)
  private String action;
  @Column(nullable = false, length = 80)
  private String targetType;
  private UUID targetId;
  @Column(length = 1000)
  private String details;
  @Column(nullable = false)
  private Instant createdAt;
  @Column(length = 255)
  private String sourceIpHash;

  @PrePersist
  public void onCreate() {
    super.ensureId();
    if (createdAt == null) createdAt = Instant.now();
  }

  public User getActor() { return actor; }
  public void setActor(User actor) { this.actor = actor; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getTargetType() { return targetType; }
  public void setTargetType(String targetType) { this.targetType = targetType; }
  public UUID getTargetId() { return targetId; }
  public void setTargetId(UUID targetId) { this.targetId = targetId; }
  public String getDetails() { return details; }
  public void setDetails(String details) { this.details = details; }
  public Instant getCreatedAt() { return createdAt; }
  public String getSourceIpHash() { return sourceIpHash; }
  public void setSourceIpHash(String sourceIpHash) { this.sourceIpHash = sourceIpHash; }
}
