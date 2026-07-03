package ga.cyber241.mbolopass.dependent;

import ga.cyber241.mbolopass.common.UuidEntity;
import ga.cyber241.mbolopass.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "dependent_profiles")
public class DependentProfile extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "guardian_user_id", nullable = false)
  private User guardian;
  private String firstName;
  private String lastName;
  private String relationship;
  private LocalDate birthDate;
  private String gender;
  private String bloodType;
  private String emergencyNotes;
  private String photoStorageKey;
  private boolean enabled = true;
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

  public User getGuardian() { return guardian; }
  public void setGuardian(User guardian) { this.guardian = guardian; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getRelationship() { return relationship; }
  public void setRelationship(String relationship) { this.relationship = relationship; }
  public LocalDate getBirthDate() { return birthDate; }
  public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }
  public String getBloodType() { return bloodType; }
  public void setBloodType(String bloodType) { this.bloodType = bloodType; }
  public String getEmergencyNotes() { return emergencyNotes; }
  public void setEmergencyNotes(String emergencyNotes) { this.emergencyNotes = emergencyNotes; }
  public String getPhotoStorageKey() { return photoStorageKey; }
  public void setPhotoStorageKey(String photoStorageKey) { this.photoStorageKey = photoStorageKey; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
