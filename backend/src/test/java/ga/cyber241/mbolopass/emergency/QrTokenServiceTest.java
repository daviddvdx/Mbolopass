package ga.cyber241.mbolopass.emergency;

import static org.assertj.core.api.Assertions.assertThat;

import ga.cyber241.mbolopass.common.Enums.QrTokenStatus;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class QrTokenServiceTest {
  @Autowired CardService cards;
  @Autowired QrTokenRepository tokens;
  @Autowired UserRepository users;
  @Autowired HealthProfileService healthProfiles;

  @Test
  void generatedQrStoresOnlySha256HashAndRevokesPreviousActiveToken() {
    String email = "qr-" + UUID.randomUUID() + "@example.test";
    User user = new User();
    user.setEmail(email);
    user.setFirstName("Amina");
    user.setLastName("N.");
    user.setPasswordHash("bcrypt-placeholder");
    users.save(user);
    HealthProfile profile = healthProfiles.ensureFor(user);

    CardService.QrTokenResponse first = cards.regenerate(email);
    String firstRaw = tokenFrom(first.emergencyUrl());
    String firstHash = sha256(firstRaw);
    QrToken firstStored = tokens.findByTokenHash(firstHash).orElseThrow();

    assertThat(firstRaw).hasSizeGreaterThanOrEqualTo(40);
    assertThat(firstStored.getTokenHash()).isEqualTo(firstHash);
    assertThat(firstStored.getTokenHash()).isNotEqualTo(firstRaw);
    assertThat(firstRaw).startsWith(firstStored.getTokenPrefix());
    assertThat(firstStored.getTokenPrefix()).isNotEqualTo(firstRaw);

    CardService.QrTokenResponse second = cards.regenerate(email);
    QrToken firstAfterSecondGeneration = tokens.findById(firstStored.getId()).orElseThrow();

    assertThat(firstAfterSecondGeneration.getStatus()).isEqualTo(QrTokenStatus.REVOKED);
    assertThat(tokens.findByHealthProfileIdAndStatus(profile.getId(), QrTokenStatus.ACTIVE)).hasSize(1);
    assertThat(tokens.findByTokenHash(sha256(tokenFrom(second.emergencyUrl())))).isPresent();
  }

  private String tokenFrom(String emergencyUrl) {
    return emergencyUrl.substring(emergencyUrl.lastIndexOf('/') + 1);
  }

  private String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}