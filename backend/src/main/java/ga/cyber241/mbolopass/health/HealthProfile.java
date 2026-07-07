package ga.cyber241.mbolopass.health;

import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "health_profiles")
public class HealthProfile extends UuidEntity {
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;
  private LocalDate birthDate;
  private String gender;
  private String bloodType;
  @Column(name = "card_number", unique = true, updatable = false, length = 32)
  private String cardNumber;
  @Column(name = "emergency_qr_public_id", unique = true)
  private UUID emergencyQrPublicId;
  @Column(name = "emergency_qr_reference", unique = true, length = 32)
  private String emergencyQrReference;
  @Column(name = "emergency_qr_payload", columnDefinition = "TEXT")
  private String emergencyQrPayload;
  private Instant emergencyQrGeneratedAt;
  private Instant emergencyQrCreatedAt;
  private Instant emergencyQrRevokedAt;
  private Boolean emergencyQrEnabled = true;
  private Integer emergencyQrVersion = 1;
  private String profilePhotoUrl;
  private String emergencyNotes;
  private LocalDate lastMedicalVisitDate;
  private Instant createdAt;
  private Instant updatedAt;

  @PrePersist
  public void onCreate() {
    super.ensureId();
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  public void onUpdate() { updatedAt = Instant.now(); }

  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }
  public LocalDate getBirthDate() { return birthDate; }
  public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }
  public String getBloodType() { return bloodType; }
  public void setBloodType(String bloodType) { this.bloodType = bloodType; }
  public String getCardNumber() { return cardNumber; }
  public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
  public UUID getEmergencyQrPublicId() { return emergencyQrPublicId; }
  public void setEmergencyQrPublicId(UUID emergencyQrPublicId) { this.emergencyQrPublicId = emergencyQrPublicId; }
  public String getEmergencyQrReference() { return emergencyQrReference; }
  public void setEmergencyQrReference(String emergencyQrReference) { this.emergencyQrReference = emergencyQrReference; }
  public String getEmergencyQrPayload() { return emergencyQrPayload; }
  public void setEmergencyQrPayload(String emergencyQrPayload) { this.emergencyQrPayload = emergencyQrPayload; }
  public Instant getEmergencyQrGeneratedAt() { return emergencyQrGeneratedAt; }
  public void setEmergencyQrGeneratedAt(Instant emergencyQrGeneratedAt) { this.emergencyQrGeneratedAt = emergencyQrGeneratedAt; }
  public Instant getEmergencyQrCreatedAt() { return emergencyQrCreatedAt; }
  public void setEmergencyQrCreatedAt(Instant emergencyQrCreatedAt) { this.emergencyQrCreatedAt = emergencyQrCreatedAt; }
  public Instant getEmergencyQrRevokedAt() { return emergencyQrRevokedAt; }
  public void setEmergencyQrRevokedAt(Instant emergencyQrRevokedAt) { this.emergencyQrRevokedAt = emergencyQrRevokedAt; }
  public boolean isEmergencyQrEnabled() { return !Boolean.FALSE.equals(emergencyQrEnabled); }
  public void setEmergencyQrEnabled(boolean emergencyQrEnabled) { this.emergencyQrEnabled = emergencyQrEnabled; }
  public Integer getEmergencyQrVersion() { return emergencyQrVersion; }
  public void setEmergencyQrVersion(Integer emergencyQrVersion) { this.emergencyQrVersion = emergencyQrVersion; }
  public String getProfilePhotoUrl() { return profilePhotoUrl; }
  public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
  public String getEmergencyNotes() { return emergencyNotes; }
  public void setEmergencyNotes(String emergencyNotes) { this.emergencyNotes = emergencyNotes; }
  public LocalDate getLastMedicalVisitDate() { return lastMedicalVisitDate; }
  public void setLastMedicalVisitDate(LocalDate lastMedicalVisitDate) { this.lastMedicalVisitDate = lastMedicalVisitDate; }
  public Instant getUpdatedAt() { return updatedAt; }
}
