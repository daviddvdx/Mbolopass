package ga.cyber241.mbolopass.health;

import ga.cyber241.mbolopass.common.Enums.AllergySeverity;
import ga.cyber241.mbolopass.common.Enums.MedicalConditionStatus;
import ga.cyber241.mbolopass.common.Enums.VaccinationStatus;
import ga.cyber241.mbolopass.common.PrivateFileStorageService;
import ga.cyber241.mbolopass.common.PrivateFileStorageService.StoredFile;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HealthProfileService {
  private final HealthProfileRepository profiles;
  private final AllergyRepository allergies;
  private final MedicalConditionRepository conditions;
  private final MedicationRepository medications;
  private final VaccinationRepository vaccinations;
  private final EmergencyContactRepository contacts;
  private final PrivateFileStorageService storage;
  private final HealthPassNumberGenerator cardNumbers;

  public HealthProfileService(HealthProfileRepository profiles, AllergyRepository allergies, MedicalConditionRepository conditions, MedicationRepository medications, VaccinationRepository vaccinations, EmergencyContactRepository contacts, PrivateFileStorageService storage, HealthPassNumberGenerator cardNumbers) {
    this.profiles = profiles;
    this.allergies = allergies;
    this.conditions = conditions;
    this.medications = medications;
    this.vaccinations = vaccinations;
    this.contacts = contacts;
    this.storage = storage;
    this.cardNumbers = cardNumbers;
  }

  @Transactional
  public HealthProfile ensureFor(User user) {
    HealthProfile profile = profiles.findByUserId(user.getId()).orElseGet(() -> {
      HealthProfile created = new HealthProfile();
      created.setUser(user);
      created.setCardNumber(cardNumbers.generateUniqueCardNumber());
      return profiles.save(created);
    });
    if (profile.getCardNumber() == null || profile.getCardNumber().isBlank()) {
      profiles.assignMissingCardNumber(profile.getId(), cardNumbers.generateUniqueCardNumber());
      profile = profiles.findById(profile.getId()).orElseThrow();
    }
    return profile;
  }

  public HealthProfile current(String email) {
    return profiles.findByUserEmail(email).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profil sante introuvable"));
  }

  @Transactional
  public ProfileResponse update(String email, ProfileRequest request) {
    HealthProfile profile = current(email);
    profile.setBirthDate(request.birthDate());
    profile.setGender(request.gender());
    profile.setBloodType(request.bloodType());
    profile.setEmergencyNotes(request.emergencyNotes());
    profile.setLastMedicalVisitDate(request.lastMedicalVisitDate());
    return toProfile(profiles.save(profile));
  }

  public ProfileResponse toProfile(HealthProfile profile) {
    return new ProfileResponse(profile.getId(), profile.getCardNumber(), profile.getBirthDate(), profile.getGender(), profile.getBloodType(), profile.getEmergencyNotes(), profile.getLastMedicalVisitDate(), profile.getUpdatedAt());
  }

  @Transactional
  public int backfillMissingCardNumbers() {
    int updated = 0;
    for (UUID id : profiles.findIdsMissingCardNumber()) {
      updated += profiles.assignMissingCardNumber(id, cardNumbers.generateUniqueCardNumber());
    }
    return updated;
  }

  public int completion(HealthProfile profile) {
    int points = 0;
    UUID id = profile.getId();
    if (profile.getBloodType() != null && !profile.getBloodType().isBlank()) points += 25;
    if (profile.getLastMedicalVisitDate() != null) points += 25;
    if (contacts.existsByHealthProfileId(id)) points += 25;
    if (allergies.existsByHealthProfileId(id) || conditions.existsByHealthProfileId(id) || medications.existsByHealthProfileId(id)) points += 25;
    return points;
  }

  @Transactional
  public PhotoResponse uploadPhoto(String email, MultipartFile file) {
    HealthProfile profile = current(email);
    StoredFile stored = storage.savePhoto(file, "profile-photo");
    storage.deleteQuietly(profile.getProfilePhotoUrl());
    profile.setProfilePhotoUrl(stored.storageKey());
    profiles.save(profile);
    return new PhotoResponse(true);
  }

  public PhotoFile photo(String email) {
    HealthProfile profile = current(email);
    if (profile.getProfilePhotoUrl() == null || profile.getProfilePhotoUrl().isBlank()) throw new ApiException(HttpStatus.NOT_FOUND, "Photo introuvable");
    byte[] bytes = storage.read(profile.getProfilePhotoUrl());
    return new PhotoFile(bytes, mimeFromKey(profile.getProfilePhotoUrl()));
  }

  @Transactional
  public void deletePhoto(String email) {
    HealthProfile profile = current(email);
    storage.deleteQuietly(profile.getProfilePhotoUrl());
    profile.setProfilePhotoUrl(null);
    profiles.save(profile);
  }

  public ProfileBundle bundle(HealthProfile profile) {
    UUID id = profile.getId();
    return new ProfileBundle(profile, allergies.findByHealthProfileId(id), conditions.findByHealthProfileId(id), medications.findByHealthProfileId(id), vaccinations.findByHealthProfileId(id), contacts.findByHealthProfileId(id));
  }

  public List<ItemResponse> list(String email, String type) {
    HealthProfile profile = current(email);
    return switch (type) {
      case "allergies" -> allergies.findByHealthProfileId(profile.getId()).stream().map(a -> new ItemResponse(a.getId(), a.getLabel(), a.getSeverity().name(), a.getNotes(), null, null, null, a.isCritical(), null, null, null, null)).toList();
      case "conditions" -> conditions.findByHealthProfileId(profile.getId()).stream().map(c -> new ItemResponse(c.getId(), c.getLabel(), c.getStatus().name(), c.getNotes(), null, null, null, false, null, null, null, null)).toList();
      case "medications" -> medications.findByHealthProfileId(profile.getId()).stream().map(m -> new ItemResponse(m.getId(), m.getName(), null, m.getNotes(), m.getDosage(), m.getFrequency(), m.getEndDate(), m.isCritical(), null, null, null, null)).toList();
      case "vaccinations" -> vaccinations.findByHealthProfileId(profile.getId()).stream().map(v -> new ItemResponse(v.getId(), v.getVaccineName(), v.getStatus().name(), null, null, null, null, false, v.getAdministeredOn(), v.getNextDueDate(), null, null)).toList();
      case "emergency-contacts" -> contacts.findByHealthProfileId(profile.getId()).stream().map(c -> new ItemResponse(c.getId(), c.getFullName(), c.getRelationship(), null, null, null, null, c.isPrimary(), null, null, c.getPhone(), null)).toList();
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Type invalide");
    };
  }

  @Transactional
  public ItemResponse add(String email, String type, ItemRequest request) {
    HealthProfile profile = current(email);
    return switch (type) {
      case "allergies" -> {
        String label = validateLabel(request.label());
        if (allergies.existsByHealthProfileIdAndLabelIgnoreCase(profile.getId(), label)) {
          throw new ApiException(HttpStatus.CONFLICT, "Cette allergie existe deja");
        }
        Allergy a = new Allergy();
        a.setHealthProfile(profile); a.setLabel(label); a.setSeverity(parseAllergySeverity(request.level())); a.setCritical(request.critical()); a.setNotes(trimToNull(request.notes())); a.setSource("PATIENT_REPORTED"); a.setVerificationStatus("DRAFT");
        yield listOne(allergies.save(a));
      }
      case "conditions" -> {
        String label = validateLabel(request.label());
        if (conditions.existsByHealthProfileIdAndLabelIgnoreCase(profile.getId(), label)) {
          throw new ApiException(HttpStatus.CONFLICT, "Cette maladie ou condition existe deja");
        }
        MedicalCondition c = new MedicalCondition();
        c.setHealthProfile(profile); c.setLabel(label); c.setStatus(parseConditionStatus(request.level())); c.setNotes(trimToNull(request.notes())); c.setSource("PATIENT_REPORTED"); c.setVerificationStatus("DRAFT"); c.setClinicalStatus(c.getStatus().name());
        yield new ItemResponse(conditions.save(c).getId(), c.getLabel(), c.getStatus().name(), c.getNotes(), null, null, null, false, null, null, null, null);
      }
      case "medications" -> {
        Medication m = new Medication();
        m.setHealthProfile(profile); m.setName(validateLabel(request.label())); m.setDosage(trimToNull(request.dosage())); m.setFrequency(trimToNull(request.frequency())); m.setEndDate(request.endDate()); m.setCritical(request.critical()); m.setNotes(trimToNull(request.notes()));
        Medication saved = medications.save(m);
        yield new ItemResponse(saved.getId(), saved.getName(), null, saved.getNotes(), saved.getDosage(), saved.getFrequency(), saved.getEndDate(), saved.isCritical(), null, null, null, null);
      }
      case "vaccinations" -> {
        Vaccination v = new Vaccination();
        v.setHealthProfile(profile); v.setVaccineName(validateLabel(request.label())); v.setAdministeredOn(request.administeredOn()); v.setNextDueDate(request.nextDueDate()); v.setStatus(parseVaccinationStatus(request.level()));
        Vaccination saved = vaccinations.save(v);
        yield new ItemResponse(saved.getId(), saved.getVaccineName(), saved.getStatus().name(), null, null, null, null, false, saved.getAdministeredOn(), saved.getNextDueDate(), null, null);
      }
      case "emergency-contacts" -> {
        EmergencyContact c = new EmergencyContact();
        c.setHealthProfile(profile); c.setFullName(validateLabel(request.label())); c.setRelationship(trimToNull(request.level())); c.setPhone(trimToNull(request.phone())); c.setPrimary(request.critical());
        EmergencyContact saved = contacts.save(c);
        yield new ItemResponse(saved.getId(), saved.getFullName(), saved.getRelationship(), null, null, null, null, saved.isPrimary(), null, null, saved.getPhone(), null);
      }
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Type invalide");
    };
  }

  @Transactional
  public void delete(String email, String type, UUID itemId) {
    HealthProfile profile = current(email);
    boolean owned = switch (type) {
      case "allergies" -> allergies.existsByIdAndHealthProfileId(itemId, profile.getId());
      case "conditions" -> conditions.existsByIdAndHealthProfileId(itemId, profile.getId());
      case "medications" -> medications.existsByIdAndHealthProfileId(itemId, profile.getId());
      case "vaccinations" -> vaccinations.existsByIdAndHealthProfileId(itemId, profile.getId());
      case "emergency-contacts" -> contacts.existsByIdAndHealthProfileId(itemId, profile.getId());
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Type invalide");
    };
    if (!owned) throw new ApiException(HttpStatus.NOT_FOUND, "Element introuvable");
    switch (type) {
      case "allergies" -> allergies.deleteById(itemId);
      case "conditions" -> conditions.deleteById(itemId);
      case "medications" -> medications.deleteById(itemId);
      case "vaccinations" -> vaccinations.deleteById(itemId);
      case "emergency-contacts" -> contacts.deleteById(itemId);
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Type invalide");
    }
  }

  private ItemResponse listOne(Allergy a) {
    return new ItemResponse(a.getId(), a.getLabel(), a.getSeverity().name(), a.getNotes(), null, null, null, a.isCritical(), null, null, null, null);
  }

  private String validateLabel(String value) {
    String label = trimToNull(value);
    if (label == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Le libelle est obligatoire");
    if (label.length() < 2) throw new ApiException(HttpStatus.BAD_REQUEST, "Le libelle doit contenir au moins 2 caracteres");
    if (label.length() > 120) throw new ApiException(HttpStatus.BAD_REQUEST, "Le libelle doit contenir au maximum 120 caracteres");
    return label;
  }

  private AllergySeverity parseAllergySeverity(String value) {
    try {
      return AllergySeverity.valueOf(nvl(value, "LOW").trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Severite d'allergie invalide");
    }
  }

  private MedicalConditionStatus parseConditionStatus(String value) {
    try {
      return MedicalConditionStatus.valueOf(nvl(value, "ACTIVE").trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Statut de condition invalide");
    }
  }

  private VaccinationStatus parseVaccinationStatus(String value) {
    try {
      return VaccinationStatus.valueOf(nvl(value, "UPCOMING").trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Statut de vaccination invalide");
    }
  }

  private String nvl(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
  private String mimeFromKey(String key) {
    if (key.endsWith(".png")) return "image/png";
    if (key.endsWith(".webp")) return "image/webp";
    return "image/jpeg";
  }

  public record ProfileResponse(UUID id, String cardNumber, LocalDate birthDate, String gender, String bloodType, String emergencyNotes, LocalDate lastMedicalVisitDate, Instant updatedAt) {}
  public record ProfileRequest(LocalDate birthDate, String gender, String bloodType, String emergencyNotes, LocalDate lastMedicalVisitDate) {}
  public record ItemRequest(@NotBlank @Size(min = 2, max = 120) String label, String level, String notes, String dosage, String frequency, LocalDate endDate, boolean critical, LocalDate administeredOn, LocalDate nextDueDate, String phone) {}
  public record ItemResponse(UUID id, String label, String level, String notes, String dosage, String frequency, LocalDate endDate, boolean critical, LocalDate administeredOn, LocalDate nextDueDate, String phone, String extra) {}
  public record ProfileBundle(HealthProfile profile, List<Allergy> allergies, List<MedicalCondition> conditions, List<Medication> medications, List<Vaccination> vaccinations, List<EmergencyContact> contacts) {}
  public record PhotoResponse(boolean uploaded) {}
  public record PhotoFile(byte[] bytes, String mimeType) {}
}
