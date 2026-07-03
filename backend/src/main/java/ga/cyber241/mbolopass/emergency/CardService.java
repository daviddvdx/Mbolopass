package ga.cyber241.mbolopass.emergency;

import ga.cyber241.mbolopass.common.Enums.EmergencyAccessOutcome;
import ga.cyber241.mbolopass.common.Enums.EmergencySubjectType;
import ga.cyber241.mbolopass.common.Enums.EmergencyAccessType;
import ga.cyber241.mbolopass.common.Enums.QrTokenStatus;
import ga.cyber241.mbolopass.dependent.DependentProfile;
import ga.cyber241.mbolopass.dependent.DependentService;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.health.HealthProfileService.ItemResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CardService {
  private final HealthProfileService healthProfiles;
  private final DependentService dependents;
  private final QrTokenRepository tokens;
  private final EmergencyAccessLogRepository logs;
  private final SecureRandom secureRandom = new SecureRandom();
  private static final long QR_TOKEN_TTL_DAYS = 30;
  private final String publicBaseUrl;

  public CardService(HealthProfileService healthProfiles, DependentService dependents, QrTokenRepository tokens, EmergencyAccessLogRepository logs, @Value("${app.public-base-url}") String publicBaseUrl) {
    this.healthProfiles = healthProfiles;
    this.dependents = dependents;
    this.tokens = tokens;
    this.logs = logs;
    this.publicBaseUrl = publicBaseUrl;
  }

  @Transactional(readOnly = true)
  public CardResponse card(String email) {
    HealthProfile profile = healthProfiles.current(email);
    String fullName = displayFullName(profile);
    String qrStatus = tokens.findByHealthProfileIdAndStatus(profile.getId(), QrTokenStatus.ACTIVE).stream().findFirst()
        .map(t -> t.getStatus().name())
        .orElse("MISSING");
    return new CardResponse(cardId(profile), fullName, profile.getBloodType(), healthProfiles.completion(profile), qrStatus, profile.getUpdatedAt());
  }

  @Transactional
  public QrTokenResponse regenerate(String email) {
    HealthProfile profile = healthProfiles.current(email);
    tokens.findByHealthProfileIdAndStatus(profile.getId(), QrTokenStatus.ACTIVE).forEach(QrToken::revoke);
    byte[] random = new byte[32];
    secureRandom.nextBytes(random);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    QrToken token = new QrToken();
    token.setHealthProfile(profile);
    token.setTokenHash(hash(raw));
    token.setTokenPrefix(raw.substring(0, Math.min(10, raw.length())));
    token.setStatus(QrTokenStatus.ACTIVE);
    token.setExpiresAt(Instant.now().plus(QR_TOKEN_TTL_DAYS, ChronoUnit.DAYS));
    tokens.save(token);
    return new QrTokenResponse(publicBaseUrl + "/emergency/" + raw, token.getExpiresAt(), token.getStatus().name());
  }

  @Transactional
  public QrTokenResponse regenerateDependent(String email, UUID dependentId) {
    DependentProfile dependent = dependents.owned(email, dependentId);
    tokens.findByDependentProfileIdAndStatus(dependent.getId(), QrTokenStatus.ACTIVE).forEach(QrToken::revoke);
    byte[] random = new byte[32];
    secureRandom.nextBytes(random);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    QrToken token = new QrToken();
    token.setDependentProfile(dependent);
    token.setTokenHash(hash(raw));
    token.setTokenPrefix(raw.substring(0, Math.min(10, raw.length())));
    token.setStatus(QrTokenStatus.ACTIVE);
    token.setExpiresAt(Instant.now().plus(QR_TOKEN_TTL_DAYS, ChronoUnit.DAYS));
    tokens.save(token);
    return new QrTokenResponse(publicBaseUrl + "/emergency/" + raw, token.getExpiresAt(), token.getStatus().name());
  }

  @Transactional
  public void revoke(String email) {
    HealthProfile profile = healthProfiles.current(email);
    tokens.findByHealthProfileIdAndStatus(profile.getId(), QrTokenStatus.ACTIVE).forEach(QrToken::revoke);
  }

  @Transactional
  public EmergencyResponse emergency(String rawToken, String ip) {
    String rawHash = hash(rawToken);
    QrToken token = tokens.findByTokenHash(rawHash).orElse(null);
    if (token == null) throw logAndThrow(null, null, null, EmergencyAccessOutcome.DENIED, ip, HttpStatus.NOT_FOUND);
    if (token.getStatus() == QrTokenStatus.REVOKED) throw logAndThrow(token.getHealthProfile(), token.getDependentProfile(), token, EmergencyAccessOutcome.REVOKED, ip, HttpStatus.GONE);
    if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
      token.setStatus(QrTokenStatus.EXPIRED);
      throw logAndThrow(token.getHealthProfile(), token.getDependentProfile(), token, EmergencyAccessOutcome.EXPIRED, ip, HttpStatus.GONE);
    }
    if (token.getDependentProfile() != null) return dependentEmergency(token, ip);
    HealthProfile profile = token.getHealthProfile();
    EmergencyAccessLog log = accessLog(profile, null, token, EmergencyAccessOutcome.GRANTED, ip);
    logs.save(log);
    List<CriticalAllergyResponse> allergies = healthProfiles.list(profile.getUser().getEmail(), "allergies").stream()
        .filter(a -> "HIGH".equals(a.level()) || "CRITICAL".equals(a.level()))
        .map(a -> new CriticalAllergyResponse(a.label(), a.level()))
        .toList();
    List<String> conditions = healthProfiles.list(profile.getUser().getEmail(), "conditions").stream().filter(c -> "ACTIVE".equals(c.level())).map(ItemResponse::label).toList();
    List<String> medications = healthProfiles.list(profile.getUser().getEmail(), "medications").stream().filter(ItemResponse::critical).map(ItemResponse::label).toList();
    ItemResponse primary = healthProfiles.list(profile.getUser().getEmail(), "emergency-contacts").stream().filter(ItemResponse::critical).findFirst().orElse(null);
    EmergencyContactResponse contact = primary == null ? null : new EmergencyContactResponse(primary.label(), primary.level(), primary.phone());
    return new EmergencyResponse(EmergencySubjectType.PATIENT.name(), profile.getBloodType(), allergies, conditions, medications, profile.getEmergencyNotes(), contact);
  }

  private EmergencyResponse dependentEmergency(QrToken token, String ip) {
    DependentProfile dependent = token.getDependentProfile();
    logs.save(accessLog(null, dependent, token, EmergencyAccessOutcome.GRANTED, ip));
    List<CriticalAllergyResponse> allergies = dependents.listItems(dependent, "allergies").stream()
        .filter(a -> "HIGH".equals(a.level()) || "CRITICAL".equals(a.level()))
        .map(a -> new CriticalAllergyResponse(a.label(), a.level()))
        .toList();
    List<String> conditions = dependents.listItems(dependent, "conditions").stream().filter(c -> "ACTIVE".equals(c.level())).map(DependentService.ItemResponse::label).toList();
    List<String> medications = dependents.listItems(dependent, "medications").stream().filter(DependentService.ItemResponse::critical).map(DependentService.ItemResponse::label).toList();
    DependentService.ItemResponse primary = dependents.listItems(dependent, "emergency-contacts").stream().filter(DependentService.ItemResponse::critical).findFirst().orElse(null);
    EmergencyContactResponse contact = primary == null ? null : new EmergencyContactResponse(primary.label(), primary.level(), primary.phone());
    return new EmergencyResponse(EmergencySubjectType.DEPENDENT.name(), dependent.getBloodType(), allergies, conditions, medications, dependent.getEmergencyNotes(), contact);
  }

  private ResponseStatusException logAndThrow(HealthProfile profile, DependentProfile dependent, QrToken token, EmergencyAccessOutcome outcome, String ip, HttpStatus status) {
    logs.save(accessLog(profile, dependent, token, outcome, ip));
    return new ResponseStatusException(status, "Jeton urgence invalide");
  }

  private EmergencyAccessLog accessLog(HealthProfile profile, DependentProfile dependent, QrToken token, EmergencyAccessOutcome outcome, String ip) {
    EmergencyAccessLog log = new EmergencyAccessLog();
    log.setHealthProfile(profile);
    log.setDependentProfile(dependent);
    log.setQrToken(token);
    log.setAccessType(EmergencyAccessType.QR_EMERGENCY);
    log.setSourceIpHash(ip == null ? null : hash(ip));
    log.setOutcome(outcome);
    return log;
  }

  private String displayFullName(HealthProfile profile) {
    String lastName = profile.getUser().getLastName();
    String initial = lastName == null || lastName.isBlank() ? "" : " " + lastName.substring(0, 1).toUpperCase() + ".";
    return profile.getUser().getFirstName() + initial;
  }

  private String cardId(HealthProfile profile) {
    return "MBP-" + profile.getId().toString().replace("-", "").substring(0, 6).toUpperCase();
  }

  private String hash(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Hash impossible", ex);
    }
  }

  public record CardResponse(String cardId, String fullName, String bloodType, int profileCompletionPercentage, String qrStatus, Instant lastUpdatedAt) {}
  public record QrTokenResponse(String emergencyUrl, Instant expiresAt, String status) {}
  public record CriticalAllergyResponse(String label, String severity) {}
  public record EmergencyContactResponse(String fullName, String relationship, String phone) {}
  public record EmergencyResponse(String subjectType, String bloodType, List<CriticalAllergyResponse> allergies, List<String> criticalConditions, List<String> criticalMedications, String emergencyNotes, EmergencyContactResponse emergencyContact) {}
}
