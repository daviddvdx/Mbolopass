package ga.cyber241.mbolopass.clinical;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalProfileBackfillRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(ProfessionalProfileBackfillRunner.class);
  private final ClinicalService clinicalService;

  public ProfessionalProfileBackfillRunner(ClinicalService clinicalService) {
    this.clinicalService = clinicalService;
  }

  @Override
  public void run(ApplicationArguments args) {
    int created = clinicalService.backfillMissingProfessionalProfiles();
    if (created > 0) log.info("Profils professionnels crees pour {} utilisateurs HEALTH_PROFESSIONAL existants", created);
  }
}
