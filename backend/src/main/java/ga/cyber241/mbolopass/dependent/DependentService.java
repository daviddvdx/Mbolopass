package ga.cyber241.mbolopass.dependent;

import ga.cyber241.mbolopass.common.Enums.AllergySeverity;
import ga.cyber241.mbolopass.common.Enums.MedicalConditionStatus;
import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.common.PrivateFileStorageService;
import ga.cyber241.mbolopass.common.PrivateFileStorageService.StoredFile;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DependentService {
  private final DependentProfileRepository dependents;
  private final DependentAllergyRepository allergies;
  private final DependentMedicalConditionRepository conditions;
  private final DependentMedicationRepository medications;
  private final DependentEmergencyContactRepository contacts;
  private final UserRepository users;
  private final PrivateFileStorageService storage;

  public DependentService(DependentProfileRepository dependents, DependentAllergyRepository allergies, DependentMedicalConditionRepository conditions, DependentMedicationRepository medications, DependentEmergencyContactRepository contacts, UserRepository users, PrivateFileStorageService storage) {
    this.dependents = dependents;
    this.allergies = allergies;
    this.conditions = conditions;
    this.medications = medications;
    this.contacts = contacts;
    this.users = users;
    this.storage = storage;
  }

  @Transactional(readOnly = true)
  public List<DependentResponse> list(String email) {
    User guardian = actor(email);
    return dependents.findByGuardianIdAndEnabledTrue(guardian.getId()).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public DependentResponse get(String email, UUID dependentId) { return toResponse(owned(email, dependentId)); }

  @Transactional
  public DependentResponse create(String email, DependentRequest request) {
    DependentProfile dependent = new DependentProfile();
    dependent.setGuardian(actor(email));
    apply(dependent, request);
    return toResponse(dependents.save(dependent));
  }

  @Transactional
  public DependentResponse update(String email, UUID dependentId, DependentRequest request) {
    DependentProfile dependent = owned(email, dependentId);
    apply(dependent, request);
    return toResponse(dependents.save(dependent));
  }

  @Transactional
  public DependentResponse status(String email, UUID dependentId, StatusRequest request) {
    DependentProfile dependent = owned(email, dependentId);
    dependent.setEnabled(request.enabled());
    return toResponse(dependents.save(dependent));
  }

  @Transactional
  public void disable(String email, UUID dependentId) {
    DependentProfile dependent = owned(email, dependentId);
    dependent.setEnabled(false);
    dependents.save(dependent);
  }

  @Transactional(readOnly = true)
  public List<ItemResponse> listItems(String email, UUID dependentId, String type) {
    DependentProfile dependent = owned(email, dependentId);
    return listItems(dependent, type);
  }

  public List<ItemResponse> listItems(DependentProfile dependent, String type) {
    UUID id = dependent.getId();
    return switch (type) {
      case "allergies" -> allergies.findByDependentProfileId(id).stream().map(a -> new ItemResponse(a.getId(), a.getLabel(), a.getSeverity().name(), a.getNotes(), null, null, null, false, null)).toList();
      case "conditions" -> conditions.findByDependentProfileId(id).stream().map(c -> new ItemResponse(c.getId(), c.getLabel(), c.getStatus().name(), c.getNotes(), null, null, null, false, null)).toList();
      case "medications" -> medications.findByDependentProfileId(id).stream().map(m -> new ItemResponse(m.getId(), m.getName(), null, m.getNotes(), m.getDosage(), m.getFrequency(), m.getEndDate(), m.isCritical(), null)).toList();
      case "emergency-contacts" -> contacts.findByDependentProfileId(id).stream().map(c -> new ItemResponse(c.getId(), c.getFullName(), c.getRelationship(), null, null, null, null, c.isPrimary(), c.getPhone())).toList();
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Type invalide");
    };
  }

  @Transactional
  public ItemResponse addItem(String email, UUID dependentId, String type, ItemRequest request) {
    DependentProfile dependent = owned(email, dependentId);
    return switch (type) {
      case "allergies" -> {
        DependentAllergy item = new DependentAllergy();
        item.setDependentProfile(dependent); item.setLabel(request.label()); item.setSeverity(AllergySeverity.valueOf(nvl(request.level(), "LOW"))); item.setNotes(request.notes());
        DependentAllergy saved = allergies.save(item);
        yield new ItemResponse(saved.getId(), saved.getLabel(), saved.getSeverity().name(), saved.getNotes(), null, null, null, false, null);
      }
      case "conditions" -> {
        DependentMedicalCondition item = new DependentMedicalCondition();
        item.setDependentProfile(dependent); item.setLabel(request.label()); item.setStatus(MedicalConditionStatus.valueOf(nvl(request.level(), "ACTIVE"))); item.setNotes(request.notes());
        DependentMedicalCondition saved = conditions.save(item);
        yield new ItemResponse(saved.getId(), saved.getLabel(), saved.getStatus().name(), saved.getNotes(), null, null, null, false, null);
      }
      case "medications" -> {
        DependentMedication item = new DependentMedication();
        item.setDependentProfile(dependent); item.setName(request.label()); item.setDosage(request.dosage()); item.setFrequency(request.frequency()); item.setEndDate(request.endDate()); item.setCritical(request.critical()); item.setNotes(request.notes());
        DependentMedication saved = medications.save(item);
        yield new ItemResponse(saved.getId(), saved.getName(), null, saved.getNotes(), saved.getDosage(), saved.getFrequency(), saved.getEndDate(), saved.isCritical(), null);
      }
      case "emergency-contacts" -> {
        DependentEmergencyContact item = new DependentEmergencyContact();
        item.setDependentProfile(dependent); item.setFullName(request.label()); item.setRelationship(request.level()); item.setPhone(request.phone()); item.setPrimary(request.critical());
        DependentEmergencyContact saved = contacts.save(item);
        yield new ItemResponse(saved.getId(), saved.getFullName(), saved.getRelationship(), null, null, null, null, saved.isPrimary(), saved.getPhone());
      }
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Type invalide");
    };
  }

  @Transactional
  public void deleteItem(String email, UUID dependentId, String type, UUID itemId) {
    DependentProfile dependent = owned(email, dependentId);
    boolean exists = listItems(dependent, type).stream().anyMatch(item -> item.id().equals(itemId));
    if (!exists) throw new ApiException(HttpStatus.NOT_FOUND, "Element introuvable");
    switch (type) {
      case "allergies" -> allergies.deleteById(itemId);
      case "conditions" -> conditions.deleteById(itemId);
      case "medications" -> medications.deleteById(itemId);
      case "emergency-contacts" -> contacts.deleteById(itemId);
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Type invalide");
    }
  }

  @Transactional
  public PhotoResponse uploadPhoto(String email, UUID dependentId, MultipartFile file) {
    DependentProfile dependent = owned(email, dependentId);
    StoredFile stored = storage.savePhoto(file, "dependent-photo");
    storage.deleteQuietly(dependent.getPhotoStorageKey());
    dependent.setPhotoStorageKey(stored.storageKey());
    dependents.save(dependent);
    return new PhotoResponse(true);
  }

  public PhotoFile photo(String email, UUID dependentId) {
    DependentProfile dependent = owned(email, dependentId);
    if (dependent.getPhotoStorageKey() == null || dependent.getPhotoStorageKey().isBlank()) throw new ApiException(HttpStatus.NOT_FOUND, "Photo introuvable");
    return new PhotoFile(storage.read(dependent.getPhotoStorageKey()), mimeFromKey(dependent.getPhotoStorageKey()));
  }

  @Transactional
  public void deletePhoto(String email, UUID dependentId) {
    DependentProfile dependent = owned(email, dependentId);
    storage.deleteQuietly(dependent.getPhotoStorageKey());
    dependent.setPhotoStorageKey(null);
    dependents.save(dependent);
  }

  @Transactional(readOnly = true)
  public DependentProfile owned(String email, UUID dependentId) {
    User user = actor(email);
    DependentProfile dependent = dependents.findById(dependentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profil introuvable"));
    if (user.getRole() != Role.HEALTH_ADMIN && !dependent.getGuardian().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Acces refuse");
    return dependent;
  }

  public DependentProfile findForEmergency(UUID dependentId) {
    return dependents.findById(dependentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profil introuvable"));
  }

  public long countEnabled() { return dependents.countByEnabledTrue(); }

  private User actor(String email) {
    return users.findByEmail(email.toLowerCase()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide"));
  }

  private void apply(DependentProfile dependent, DependentRequest request) {
    dependent.setFirstName(request.firstName());
    dependent.setLastName(request.lastName());
    dependent.setRelationship(request.relationship());
    dependent.setBirthDate(request.birthDate());
    dependent.setGender(request.gender());
    dependent.setBloodType(request.bloodType());
    dependent.setEmergencyNotes(request.emergencyNotes());
  }

  private DependentResponse toResponse(DependentProfile dependent) {
    return new DependentResponse(dependent.getId(), dependent.getFirstName(), dependent.getLastName(), dependent.getRelationship(), dependent.getBirthDate(), dependent.getGender(), dependent.getBloodType(), dependent.getEmergencyNotes(), dependent.isEnabled(), dependent.getCreatedAt(), dependent.getUpdatedAt(), dependent.getPhotoStorageKey() != null);
  }

  private String nvl(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
  private String mimeFromKey(String key) {
    if (key.endsWith(".png")) return "image/png";
    if (key.endsWith(".webp")) return "image/webp";
    return "image/jpeg";
  }

  public record DependentRequest(@NotBlank String firstName, @NotBlank String lastName, String relationship, LocalDate birthDate, String gender, String bloodType, String emergencyNotes) {}
  public record StatusRequest(boolean enabled) {}
  public record DependentResponse(UUID id, String firstName, String lastName, String relationship, LocalDate birthDate, String gender, String bloodType, String emergencyNotes, boolean enabled, Instant createdAt, Instant updatedAt, boolean hasPhoto) {}
  public record ItemRequest(@NotBlank String label, String level, String notes, String dosage, String frequency, LocalDate endDate, boolean critical, String phone) {}
  public record ItemResponse(UUID id, String label, String level, String notes, String dosage, String frequency, LocalDate endDate, boolean critical, String phone) {}
  public record PhotoResponse(boolean uploaded) {}
  public record PhotoFile(byte[] bytes, String mimeType) {}
}
