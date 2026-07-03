package ga.cyber241.mbolopass.ai;

import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSummaryService {
  public static final String DISCLAIMER = "Cette information est fournie à titre d'assistance et ne remplace pas l'avis d'un professionnel de santé.";
  private final HealthProfileService healthProfiles;
  private final HealthSummaryProvider provider;
  private final AiSummaryRepository summaries;

  public AiSummaryService(HealthProfileService healthProfiles, HealthSummaryProvider provider, AiSummaryRepository summaries) {
    this.healthProfiles = healthProfiles;
    this.provider = provider;
    this.summaries = summaries;
  }

  @Transactional
  public SummaryResponse regenerate(String email) {
    HealthProfile profile = healthProfiles.current(email);
    AiSummary summary = new AiSummary();
    summary.setHealthProfile(profile);
    summary.setContent(provider.generate(email) + "\n\n" + DISCLAIMER);
    summary.setSourceVersion("rule-based-v1");
    summary.setDisclaimerVisible(true);
    return toResponse(summaries.save(summary));
  }

  public SummaryResponse latest(String email) {
    HealthProfile profile = healthProfiles.current(email);
    return summaries.findFirstByHealthProfileIdOrderByGeneratedAtDesc(profile.getId()).map(this::toResponse)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucun resume genere"));
  }

  private SummaryResponse toResponse(AiSummary summary) {
    return new SummaryResponse(summary.getId(), summary.getContent(), summary.getGeneratedAt(), summary.getSourceVersion(), summary.isDisclaimerVisible(), DISCLAIMER);
  }

  public record SummaryResponse(UUID id, String content, Instant generatedAt, String sourceVersion, boolean disclaimerVisible, String disclaimer) {}
}
