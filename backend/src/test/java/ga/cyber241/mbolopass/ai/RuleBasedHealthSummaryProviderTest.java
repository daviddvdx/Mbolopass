package ga.cyber241.mbolopass.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.health.HealthProfileService.ItemResponse;
import ga.cyber241.mbolopass.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RuleBasedHealthSummaryProviderTest {
  @Test
  void generatesFrenchSummaryWithoutDiagnosis() {
    HealthProfileService healthProfiles = mock(HealthProfileService.class);
    RuleBasedHealthSummaryProvider provider = new RuleBasedHealthSummaryProvider(healthProfiles);
    User user = new User();
    user.setEmail("demo@example.test");
    HealthProfile profile = new HealthProfile();
    profile.setUser(user);
    profile.setBloodType("O+");
    profile.setLastMedicalVisitDate(LocalDate.now().minusMonths(2));
    ItemResponse allergy = new ItemResponse(UUID.randomUUID(), "Profil de demonstration", "HIGH", null, null, null, null, false, null, null, null, null);

    when(healthProfiles.current("demo@example.test")).thenReturn(profile);
    when(healthProfiles.list("demo@example.test", "allergies")).thenReturn(List.of(allergy));
    when(healthProfiles.list("demo@example.test", "conditions")).thenReturn(List.of());
    when(healthProfiles.list("demo@example.test", "medications")).thenReturn(List.of());
    when(healthProfiles.list("demo@example.test", "vaccinations")).thenReturn(List.of());

    String summary = provider.generate("demo@example.test");

    assertThat(summary).contains("Resume sante assiste MboloPass", "O+");
    assertThat(summary.toLowerCase()).doesNotContain("diagnostic", "prescription", "dosage recommande");
  }
}
