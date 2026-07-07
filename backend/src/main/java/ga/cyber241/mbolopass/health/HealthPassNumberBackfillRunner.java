package ga.cyber241.mbolopass.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class HealthPassNumberBackfillRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(HealthPassNumberBackfillRunner.class);
  private final HealthProfileService healthProfiles;

  public HealthPassNumberBackfillRunner(HealthProfileService healthProfiles) {
    this.healthProfiles = healthProfiles;
  }

  @Override
  public void run(ApplicationArguments args) {
    int updated = healthProfiles.backfillMissingCardNumbers();
    if (updated > 0) log.info("Numeros de carte MboloPass generes pour {} profils existants", updated);
  }
}
