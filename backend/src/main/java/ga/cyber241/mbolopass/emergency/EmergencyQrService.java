package ga.cyber241.mbolopass.emergency;

import ga.cyber241.mbolopass.common.Enums.EmergencyAccessOutcome;
import ga.cyber241.mbolopass.common.Enums.EmergencyAccessType;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileRepository;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.health.HealthProfileService.ItemResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmergencyQrService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final char[] REFERENCE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
  private static final int MAX_PAYLOAD_LENGTH = 900;
  private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final HealthProfileService healthProfiles;
  private final HealthProfileRepository profiles;
  private final EmergencyAccessLogRepository logs;

  public EmergencyQrService(HealthProfileService healthProfiles, HealthProfileRepository profiles, EmergencyAccessLogRepository logs) {
    this.healthProfiles = healthProfiles;
    this.profiles = profiles;
    this.logs = logs;
  }

  @Transactional
  public EmergencyQrResponse getOrCreate(String email) {
    HealthProfile profile = healthProfiles.current(email);
    if (hasPayload(profile)) {
      return toResponse(profile);
    }
    ensureReference(profile);
    profile.setEmergencyQrVersion(1);
    profile.setEmergencyQrPayload(buildEmergencyQrPayload(profile));
    profile.setEmergencyQrGeneratedAt(Instant.now());
    profile.setEmergencyQrCreatedAt(profile.getEmergencyQrGeneratedAt());
    profile = profiles.save(profile);
    logs.save(accessLog(profile, EmergencyAccessOutcome.GRANTED));
    return toResponse(profile);
  }

  @Transactional
  public EmergencyQrResponse refresh(String email) {
    HealthProfile profile = healthProfiles.current(email);
    ensureReference(profile);
    int nextVersion = profile.getEmergencyQrVersion() == null ? 1 : profile.getEmergencyQrVersion() + 1;
    profile.setEmergencyQrVersion(nextVersion);
    profile.setEmergencyQrPayload(buildEmergencyQrPayload(profile));
    profile.setEmergencyQrGeneratedAt(Instant.now());
    if (profile.getEmergencyQrCreatedAt() == null) profile.setEmergencyQrCreatedAt(profile.getEmergencyQrGeneratedAt());
    profile = profiles.save(profile);
    logs.save(accessLog(profile, EmergencyAccessOutcome.GRANTED));
    return toResponse(profile);
  }

  private boolean hasPayload(HealthProfile profile) {
    return profile.getEmergencyQrPayload() != null && !profile.getEmergencyQrPayload().isBlank();
  }

  private void ensureReference(HealthProfile profile) {
    if (profile.getEmergencyQrReference() != null && !profile.getEmergencyQrReference().isBlank()) return;
    String reference;
    do {
      reference = "EMG-" + randomReference();
    } while (profiles.existsByEmergencyQrReference(reference));
    profile.setEmergencyQrReference(reference);
  }

  private String randomReference() {
    StringBuilder value = new StringBuilder(8);
    for (int index = 0; index < 8; index++) {
      value.append(REFERENCE_ALPHABET[SECURE_RANDOM.nextInt(REFERENCE_ALPHABET.length)]);
    }
    return value.toString();
  }

  private String buildEmergencyQrPayload(HealthProfile profile) {
    String email = profile.getUser().getEmail();
    List<ItemResponse> allergies = healthProfiles.list(email, "allergies").stream()
        .filter(item -> item.critical() || "HIGH".equals(item.level()) || "CRITICAL".equals(item.level()))
        .toList();
    List<ItemResponse> conditions = healthProfiles.list(email, "conditions").stream()
        .filter(item -> "ACTIVE".equals(item.level()))
        .toList();
    List<ItemResponse> contacts = healthProfiles.list(email, "emergency-contacts").stream().toList();

    ItemResponse contactOne = contacts.stream().filter(ItemResponse::critical).findFirst().orElse(contacts.isEmpty() ? null : contacts.get(0));
    ItemResponse contactTwo = contacts.stream()
        .filter(contact -> contactOne == null || !contact.id().equals(contactOne.id()))
        .findFirst()
        .orElse(null);

    String allergiesText = joinLabels(allergies, 160);
    String conditionsText = joinLabels(conditions, 160);
    LocalDate birthDate = profile.getBirthDate();
    Integer age = calculateAge(birthDate);
    LocalDate generatedDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();

    String payload = String.join("\n",
        "MBOLOPASS - INFORMATIONS D'URGENCE",
        "Carte: " + cleanEmergencyValue(profile.getCardNumber(), "Non renseigne", 40),
        "Reference: " + cleanEmergencyValue(profile.getEmergencyQrReference(), "Non renseigne", 32),
        "",
        "Nom: " + fullName(profile),
        "Date de naissance: " + formatDate(birthDate),
        "Age: " + (age == null ? "Non renseigne" : age + " ans"),
        "Telephone patient: " + cleanEmergencyValue(profile.getUser().getPhone(), "Non renseigne", 30),
        "Groupe sanguin: " + cleanEmergencyValue(profile.getBloodType(), "Non renseigne", 16),
        "",
        "Allergies: " + allergiesText,
        "Pathologies: " + conditionsText,
        "",
        "Urgence 1: " + contactName(contactOne),
        "Tel 1: " + contactPhone(contactOne),
        "",
        "Urgence 2: " + contactName(contactTwo),
        "Tel 2: " + contactPhone(contactTwo),
        "",
        "QR genere le: " + generatedDate.format(DISPLAY_DATE),
        "Informations d'urgence uniquement.",
        "Le dossier medical complet reste protege.");

    return payload.length() <= MAX_PAYLOAD_LENGTH ? payload : payload.substring(0, MAX_PAYLOAD_LENGTH - 3) + "...";
  }

  private EmergencyQrResponse toResponse(HealthProfile profile) {
    return new EmergencyQrResponse(
        profile.getCardNumber(),
        profile.getEmergencyQrReference(),
        profile.getEmergencyQrPayload(),
        profile.getEmergencyQrGeneratedAt(),
        profile.getEmergencyQrVersion() == null ? 1 : profile.getEmergencyQrVersion());
  }

  private String fullName(HealthProfile profile) {
    String firstName = cleanEmergencyValue(profile.getUser().getFirstName(), "", 80);
    String lastName = cleanEmergencyValue(profile.getUser().getLastName(), "", 80);
    return cleanEmergencyValue((firstName + " " + lastName).trim(), "Non renseigne", 120);
  }

  private String joinLabels(List<ItemResponse> items, int maxLength) {
    String joined = String.join(", ", items.stream().map(ItemResponse::label).toList());
    return cleanEmergencyValue(joined, "Non renseigne", maxLength);
  }

  private String contactName(ItemResponse contact) {
    return cleanEmergencyValue(contact == null ? null : contact.label(), "Non renseigne", 80);
  }

  private String contactPhone(ItemResponse contact) {
    return cleanEmergencyValue(contact == null ? null : contact.phone(), "Non renseigne", 30);
  }

  private String formatDate(LocalDate date) {
    return date == null ? "Non renseigne" : date.format(DISPLAY_DATE);
  }

  private Integer calculateAge(LocalDate dateOfBirth) {
    if (dateOfBirth == null) return null;
    return Period.between(dateOfBirth, LocalDate.now()).getYears();
  }

  private String cleanEmergencyValue(String value, String fallback, int maxLength) {
    if (value == null || value.isBlank()) return fallback;
    String cleaned = value
        .replaceAll("[\\p{Cntrl}&&[^\\n]]+", " ")
        .replaceAll("[\\r\\n\\t]+", " ")
        .replaceAll("\\s{2,}", " ")
        .trim();
    if (cleaned.isBlank()) return fallback;
    return cleaned.length() > maxLength ? cleaned.substring(0, Math.max(0, maxLength - 3)) + "..." : cleaned;
  }

  private EmergencyAccessLog accessLog(HealthProfile profile, EmergencyAccessOutcome outcome) {
    EmergencyAccessLog log = new EmergencyAccessLog();
    log.setHealthProfile(profile);
    log.setAccessType(EmergencyAccessType.QR_EMERGENCY);
    log.setOutcome(outcome);
    return log;
  }

  public record EmergencyQrResponse(String cardNumber, String reference, String payload, Instant generatedAt, int version) {}
}
