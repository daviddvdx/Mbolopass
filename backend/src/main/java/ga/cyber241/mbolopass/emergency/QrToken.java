package ga.cyber241.mbolopass.emergency;

import ga.cyber241.mbolopass.common.Enums.EmergencyAccessOutcome;
import ga.cyber241.mbolopass.common.Enums.EmergencyAccessType;
import ga.cyber241.mbolopass.common.Enums.QrTokenStatus;
import ga.cyber241.mbolopass.dependent.DependentProfile;
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

@Entity
@Table(name = "qr_tokens")
public class QrToken extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dependent_profile_id")
  private DependentProfile dependentProfile;
  private String tokenHash;
  private String tokenPrefix;
  @Enumerated(EnumType.STRING)
  private QrTokenStatus status = QrTokenStatus.ACTIVE;
  private Instant expiresAt;
  private Instant createdAt;
  private Instant revokedAt;
  @PrePersist public void onCreate() { super.ensureId(); createdAt = Instant.now(); }
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public DependentProfile getDependentProfile() { return dependentProfile; }
  public void setDependentProfile(DependentProfile dependentProfile) { this.dependentProfile = dependentProfile; }
  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
  public String getTokenPrefix() { return tokenPrefix; }
  public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; }
  public QrTokenStatus getStatus() { return status; }
  public void setStatus(QrTokenStatus status) { this.status = status; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public void revoke() { status = QrTokenStatus.REVOKED; revokedAt = Instant.now(); }
}

@Entity
@Table(name = "emergency_access_logs")
class EmergencyAccessLog extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dependent_profile_id")
  private DependentProfile dependentProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "qr_token_id")
  private QrToken qrToken;
  @Enumerated(EnumType.STRING)
  private EmergencyAccessType accessType;
  private String sourceIpHash;
  private Instant accessedAt;
  @Enumerated(EnumType.STRING)
  private EmergencyAccessOutcome outcome;
  @PrePersist public void onCreate() { super.ensureId(); accessedAt = Instant.now(); }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public void setDependentProfile(DependentProfile dependentProfile) { this.dependentProfile = dependentProfile; }
  public void setQrToken(QrToken qrToken) { this.qrToken = qrToken; }
  public void setAccessType(EmergencyAccessType accessType) { this.accessType = accessType; }
  public void setSourceIpHash(String sourceIpHash) { this.sourceIpHash = sourceIpHash; }
  public void setOutcome(EmergencyAccessOutcome outcome) { this.outcome = outcome; }
}
