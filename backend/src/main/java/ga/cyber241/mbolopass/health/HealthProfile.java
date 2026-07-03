package ga.cyber241.mbolopass.health;

import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "health_profiles")
public class HealthProfile extends UuidEntity {
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;
  private LocalDate birthDate;
  private String gender;
  private String bloodType;
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
  public String getProfilePhotoUrl() { return profilePhotoUrl; }
  public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
  public String getEmergencyNotes() { return emergencyNotes; }
  public void setEmergencyNotes(String emergencyNotes) { this.emergencyNotes = emergencyNotes; }
  public LocalDate getLastMedicalVisitDate() { return lastMedicalVisitDate; }
  public void setLastMedicalVisitDate(LocalDate lastMedicalVisitDate) { this.lastMedicalVisitDate = lastMedicalVisitDate; }
  public Instant getUpdatedAt() { return updatedAt; }
}
