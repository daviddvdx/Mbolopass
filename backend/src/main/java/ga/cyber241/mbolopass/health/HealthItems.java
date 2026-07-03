package ga.cyber241.mbolopass.health;

import ga.cyber241.mbolopass.common.Enums.AllergySeverity;
import ga.cyber241.mbolopass.common.Enums.MedicalConditionStatus;
import ga.cyber241.mbolopass.common.Enums.VaccinationStatus;
import ga.cyber241.mbolopass.common.UuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "emergency_contacts")
class EmergencyContact extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  private String fullName;
  private String relationship;
  private String phone;
  private boolean isPrimary;
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }
  public String getRelationship() { return relationship; }
  public void setRelationship(String relationship) { this.relationship = relationship; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public boolean isPrimary() { return isPrimary; }
  public void setPrimary(boolean primary) { isPrimary = primary; }
}

@Entity
@Table(name = "allergies")
class Allergy extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  private String label;
  @Enumerated(EnumType.STRING)
  private AllergySeverity severity;
  private String notes;
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public AllergySeverity getSeverity() { return severity; }
  public void setSeverity(AllergySeverity severity) { this.severity = severity; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}

@Entity
@Table(name = "medical_conditions")
class MedicalCondition extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  private String label;
  @Enumerated(EnumType.STRING)
  private MedicalConditionStatus status;
  private String notes;
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public MedicalConditionStatus getStatus() { return status; }
  public void setStatus(MedicalConditionStatus status) { this.status = status; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}

@Entity
@Table(name = "medications")
class Medication extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  private String name;
  private String dosage;
  private String frequency;
  private LocalDate startDate;
  private LocalDate endDate;
  private boolean isCritical;
  private String notes;
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDosage() { return dosage; }
  public void setDosage(String dosage) { this.dosage = dosage; }
  public String getFrequency() { return frequency; }
  public void setFrequency(String frequency) { this.frequency = frequency; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
  public boolean isCritical() { return isCritical; }
  public void setCritical(boolean critical) { isCritical = critical; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}

@Entity
@Table(name = "vaccinations")
class Vaccination extends UuidEntity {
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "health_profile_id")
  private HealthProfile healthProfile;
  private String vaccineName;
  private LocalDate administeredOn;
  private LocalDate nextDueDate;
  @Enumerated(EnumType.STRING)
  private VaccinationStatus status;
  public HealthProfile getHealthProfile() { return healthProfile; }
  public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
  public String getVaccineName() { return vaccineName; }
  public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
  public LocalDate getAdministeredOn() { return administeredOn; }
  public void setAdministeredOn(LocalDate administeredOn) { this.administeredOn = administeredOn; }
  public LocalDate getNextDueDate() { return nextDueDate; }
  public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
  public VaccinationStatus getStatus() { return status; }
  public void setStatus(VaccinationStatus status) { this.status = status; }
}
