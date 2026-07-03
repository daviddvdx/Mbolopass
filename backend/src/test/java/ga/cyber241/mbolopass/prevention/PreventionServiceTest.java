package ga.cyber241.mbolopass.prevention;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ga.cyber241.mbolopass.common.Enums.AlertStatus;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.health.HealthProfileService.ItemResponse;
import ga.cyber241.mbolopass.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreventionServiceTest {
  @Test
  void refreshCreatesProfileIncompleteAlerts() {
    HealthProfileService healthProfiles = mock(HealthProfileService.class);
    PreventionRepository repository = mock(PreventionRepository.class);
    PreventionService service = new PreventionService(healthProfiles, repository);
    User user = new User();
    user.setEmail("demo@example.test");
    HealthProfile profile = new HealthProfile();
    profile.setId(UUID.randomUUID());
    profile.setUser(user);

    when(healthProfiles.current("demo@example.test")).thenReturn(profile);
    when(healthProfiles.list("demo@example.test", "emergency-contacts")).thenReturn(List.of());
    when(healthProfiles.list("demo@example.test", "vaccinations")).thenReturn(List.of());
    when(healthProfiles.list("demo@example.test", "medications")).thenReturn(List.of());
    when(repository.findByHealthProfileIdOrderByCreatedAtDesc(profile.getId())).thenReturn(List.of());

    service.refresh("demo@example.test");

    verify(repository).deleteByHealthProfileIdAndStatus(profile.getId(), AlertStatus.ACTIVE);
    verify(repository, org.mockito.Mockito.atLeast(2)).save(any(PreventionAlert.class));
  }

  @Test
  void refreshCreatesUpcomingVaccinationAlert() {
    HealthProfileService healthProfiles = mock(HealthProfileService.class);
    PreventionRepository repository = mock(PreventionRepository.class);
    PreventionService service = new PreventionService(healthProfiles, repository);
    User user = new User();
    user.setEmail("demo@example.test");
    HealthProfile profile = new HealthProfile();
    profile.setId(UUID.randomUUID());
    profile.setUser(user);
    profile.setBloodType("O+");
    ItemResponse contact = new ItemResponse(UUID.randomUUID(), "Contact demo", "Famille", null, null, null, null, true, null, null, "Demo", null);
    ItemResponse vaccine = new ItemResponse(UUID.randomUUID(), "Vaccin demo", "UPCOMING", null, null, null, null, false, null, LocalDate.now().plusDays(10), null, null);

    when(healthProfiles.current("demo@example.test")).thenReturn(profile);
    when(healthProfiles.list("demo@example.test", "emergency-contacts")).thenReturn(List.of(contact));
    when(healthProfiles.list("demo@example.test", "vaccinations")).thenReturn(List.of(vaccine));
    when(healthProfiles.list("demo@example.test", "medications")).thenReturn(List.of());
    when(repository.findByHealthProfileIdOrderByCreatedAtDesc(profile.getId())).thenReturn(List.of());

    service.refresh("demo@example.test");

    verify(repository, org.mockito.Mockito.atLeastOnce()).save(any(PreventionAlert.class));
  }
}
