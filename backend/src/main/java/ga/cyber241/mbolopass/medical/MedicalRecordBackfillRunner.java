package ga.cyber241.mbolopass.medical;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MedicalRecordBackfillRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(MedicalRecordBackfillRunner.class);
  private final MedicalRecordService medicalRecords;

  public MedicalRecordBackfillRunner(MedicalRecordService medicalRecords) {
    this.medicalRecords = medicalRecords;
  }

  @Override
  public void run(ApplicationArguments args) {
    int created = medicalRecords.backfillMissingMedicalRecords();
    if (created > 0) log.info("Medical records backfilled: {}", created);
  }
}
