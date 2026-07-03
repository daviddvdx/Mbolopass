package ga.cyber241.mbolopass.ai;

import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.health.HealthProfile;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_summaries")
public class AiSummary extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  private String content;
  private Instant generatedAt;
  private String sourceVersion;
  private boolean disclaimerVisible;
  @PrePersist public void onCreate() { super.ensureId(); generatedAt = Instant.now(); }
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Instant getGeneratedAt() { return generatedAt; }
  public String getSourceVersion() { return sourceVersion; }
  public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
  public boolean isDisclaimerVisible() { return disclaimerVisible; }
  public void setDisclaimerVisible(boolean disclaimerVisible) { this.disclaimerVisible = disclaimerVisible; }
}
