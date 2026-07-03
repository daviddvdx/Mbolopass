package ga.cyber241.mbolopass.prevention;

import ga.cyber241.mbolopass.common.Enums.AlertSeverity;
import ga.cyber241.mbolopass.common.Enums.AlertStatus;
import ga.cyber241.mbolopass.common.Enums.AlertType;
import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.health.HealthProfile;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "prevention_alerts")
public class PreventionAlert extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  @Enumerated(EnumType.STRING) private AlertType type;
  @Enumerated(EnumType.STRING) private AlertSeverity severity;
  private String title;
  private String message;
  @Enumerated(EnumType.STRING) private AlertStatus status = AlertStatus.ACTIVE;
  private Instant createdAt;
  private LocalDate dueDate;
  @PrePersist public void onCreate() { super.ensureId(); createdAt = Instant.now(); }
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public AlertType getType() { return type; }
  public void setType(AlertType type) { this.type = type; }
  public AlertSeverity getSeverity() { return severity; }
  public void setSeverity(AlertSeverity severity) { this.severity = severity; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public AlertStatus getStatus() { return status; }
  public void setStatus(AlertStatus status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public LocalDate getDueDate() { return dueDate; }
  public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
