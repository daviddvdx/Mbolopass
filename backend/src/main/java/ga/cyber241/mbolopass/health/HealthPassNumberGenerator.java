package ga.cyber241.mbolopass.health;

import ga.cyber241.mbolopass.exception.ApiException;
import java.security.SecureRandom;
import java.time.Year;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HealthPassNumberGenerator {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
  private static final int MAX_ATTEMPTS = 20;
  private final HealthProfileRepository profiles;

  public HealthPassNumberGenerator(HealthProfileRepository profiles) {
    this.profiles = profiles;
  }

  public String generateUniqueCardNumber() {
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      String candidate = buildCandidate();
      if (!profiles.existsByCardNumber(candidate)) return candidate;
    }
    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de generer un numero de carte unique");
  }

  private String buildCandidate() {
    return "MBP-" + Year.now(java.time.Clock.systemUTC()).getValue() + "-" + group() + "-" + group();
  }

  private String group() {
    StringBuilder value = new StringBuilder(4);
    for (int index = 0; index < 4; index++) {
      value.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
    }
    return value.toString();
  }
}
