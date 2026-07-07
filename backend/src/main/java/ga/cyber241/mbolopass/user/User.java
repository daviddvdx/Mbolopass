package ga.cyber241.mbolopass.user;

import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.common.UuidEntity;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends UuidEntity {
  private String email;
  private String passwordHash;
  private String firstName;
  private String lastName;
  private String phone;
  @Convert(converter = RoleConverter.class)
  private Role role = Role.PATIENT;
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

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email == null ? null : email.trim().toLowerCase(); }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone == null || phone.isBlank() ? null : phone.trim(); }
  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
