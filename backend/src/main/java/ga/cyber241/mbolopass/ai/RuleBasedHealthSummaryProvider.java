package ga.cyber241.mbolopass.ai;

import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.health.HealthProfileService.ItemResponse;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedHealthSummaryProvider implements HealthSummaryProvider {
  private final HealthProfileService healthProfiles;
  public RuleBasedHealthSummaryProvider(HealthProfileService healthProfiles) { this.healthProfiles = healthProfiles; }

  @Override
  public String generate(String email) {
    HealthProfile profile = healthProfiles.current(email);
    String allergies = healthProfiles.list(email, "allergies").stream().filter(a -> "HIGH".equals(a.level()) || "CRITICAL".equals(a.level())).map(ItemResponse::label).collect(Collectors.joining(", "));
    String conditions = healthProfiles.list(email, "conditions").stream().filter(c -> "ACTIVE".equals(c.level())).map(ItemResponse::label).collect(Collectors.joining(", "));
    String medications = healthProfiles.list(email, "medications").stream().filter(ItemResponse::critical).map(ItemResponse::label).collect(Collectors.joining(", "));
    String overdueVaccines = healthProfiles.list(email, "vaccinations").stream().filter(v -> v.nextDueDate() != null && v.nextDueDate().isBefore(LocalDate.now())).map(ItemResponse::label).collect(Collectors.joining(", "));
    return String.join("\n",
        "Resume sante assiste MboloPass.",
        "Groupe sanguin: " + value(profile.getBloodType()),
        "Allergies importantes signalees: " + value(allergies),
        "Pathologies actives renseignees: " + value(conditions),
        "Traitements critiques renseignes: " + value(medications),
        "Derniere visite medicale: " + value(profile.getLastMedicalVisitDate() == null ? null : profile.getLastMedicalVisitDate().toString()),
        "Vaccinations en retard: " + value(overdueVaccines));
  }

  private String value(String value) { return value == null || value.isBlank() ? "non renseigne" : value; }
}
