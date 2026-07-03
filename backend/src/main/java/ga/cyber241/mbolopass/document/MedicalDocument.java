package ga.cyber241.mbolopass.document;

import ga.cyber241.mbolopass.common.Enums.DocumentOwnerType;
import ga.cyber241.mbolopass.common.Enums.MedicalDocumentCategory;
import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.dependent.DependentProfile;
import ga.cyber241.mbolopass.health.HealthProfile;
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
import java.time.LocalDate;

@Entity
@Table(name = "medical_documents")
public class MedicalDocument extends UuidEntity {
  @Enumerated(EnumType.STRING)
  private DocumentOwnerType ownerType;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dependent_profile_id")
  private DependentProfile dependentProfile;
  @Enumerated(EnumType.STRING)
  private MedicalDocumentCategory category;
  private String title;
  private String originalFilename;
  private String storageKey;
  private String mimeType;
  private long sizeBytes;
  private LocalDate issuedDate;
  private Instant uploadedAt;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "uploaded_by_user_id", nullable = false)
  private User uploadedBy;
  private boolean archived;

  @PrePersist
  public void onCreate() {
    super.ensureId();
    if (uploadedAt == null) uploadedAt = Instant.now();
  }

  public DocumentOwnerType getOwnerType() { return ownerType; }
  public void setOwnerType(DocumentOwnerType ownerType) { this.ownerType = ownerType; }
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public DependentProfile getDependentProfile() { return dependentProfile; }
  public void setDependentProfile(DependentProfile dependentProfile) { this.dependentProfile = dependentProfile; }
  public MedicalDocumentCategory getCategory() { return category; }
  public void setCategory(MedicalDocumentCategory category) { this.category = category; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getOriginalFilename() { return originalFilename; }
  public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
  public String getStorageKey() { return storageKey; }
  public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
  public String getMimeType() { return mimeType; }
  public void setMimeType(String mimeType) { this.mimeType = mimeType; }
  public long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
  public LocalDate getIssuedDate() { return issuedDate; }
  public void setIssuedDate(LocalDate issuedDate) { this.issuedDate = issuedDate; }
  public Instant getUploadedAt() { return uploadedAt; }
  public User getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
  public boolean isArchived() { return archived; }
  public void setArchived(boolean archived) { this.archived = archived; }
}
