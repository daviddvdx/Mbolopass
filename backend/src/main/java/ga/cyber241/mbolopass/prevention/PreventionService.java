package ga.cyber241.mbolopass.prevention;

import ga.cyber241.mbolopass.common.Enums.AlertSeverity;
import ga.cyber241.mbolopass.common.Enums.AlertStatus;
import ga.cyber241.mbolopass.common.Enums.AlertType;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.health.HealthProfileService.ItemResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreventionService {
  private final HealthProfileService healthProfiles;
  private final PreventionRepository alerts;

  public PreventionService(HealthProfileService healthProfiles, PreventionRepository alerts) {
    this.healthProfiles = healthProfiles;
    this.alerts = alerts;
  }

  @Transactional
  public List<AlertResponse> refresh(String email) {
    HealthProfile profile = healthProfiles.current(email);
    alerts.deleteByHealthProfileIdAndStatus(profile.getId(), AlertStatus.ACTIVE);
    LocalDate today = LocalDate.now();
    if (healthProfiles.list(email, "emergency-contacts").stream().noneMatch(ItemResponse::critical)) {
      create(profile, AlertType.PROFILE_INCOMPLETE, AlertSeverity.WARNING, "Contact d'urgence manquant", "Ajoutez un contact principal pour le mode urgence.", null);
    }
    if (profile.getBloodType() == null || profile.getBloodType().isBlank()) {
      create(profile, AlertType.PROFILE_INCOMPLETE, AlertSeverity.WARNING, "Groupe sanguin absent", "Completez le groupe sanguin dans votre profil.", null);
    }
    for (ItemResponse vaccination : healthProfiles.list(email, "vaccinations")) {
      if (vaccination.nextDueDate() != null && vaccination.nextDueDate().isBefore(today)) {
        create(profile, AlertType.VACCINATION_DUE, AlertSeverity.WARNING, "Vaccination en retard", "Une echeance de vaccination est depassee.", vaccination.nextDueDate());
      } else if (vaccination.nextDueDate() != null && !vaccination.nextDueDate().isAfter(today.plusDays(30))) {
        create(profile, AlertType.VACCINATION_DUE, AlertSeverity.INFO, "Vaccination a prevoir", "Une vaccination arrive a echeance dans les 30 jours.", vaccination.nextDueDate());
      }
    }
    for (ItemResponse medication : healthProfiles.list(email, "medications")) {
      if (medication.endDate() != null && !medication.endDate().isBefore(today) && !medication.endDate().isAfter(today.plusDays(7))) {
        create(profile, AlertType.MEDICATION_RENEWAL, AlertSeverity.WARNING, "Renouvellement a verifier", "Un traitement arrive a echeance prochainement.", medication.endDate());
      }
    }
    if (profile.getLastMedicalVisitDate() == null || profile.getLastMedicalVisitDate().isBefore(today.minusMonths(6))) {
      create(profile, AlertType.MEDICAL_FOLLOW_UP, AlertSeverity.INFO, "Suivi medical", "Pensez a verifier votre suivi avec un professionnel de sante.", null);
    }
    return list(email);
  }

  public List<AlertResponse> list(String email) {
    return alerts.findByHealthProfileIdOrderByCreatedAtDesc(healthProfiles.current(email).getId()).stream().map(this::toResponse).toList();
  }

  @Transactional
  public AlertResponse dismiss(String email, UUID id) {
    HealthProfile profile = healthProfiles.current(email);
    PreventionAlert alert = alerts.findById(id).filter(a -> a.getHealthProfile().getId().equals(profile.getId())).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alerte introuvable"));
    alert.setStatus(AlertStatus.DISMISSED);
    return toResponse(alert);
  }

  private void create(HealthProfile profile, AlertType type, AlertSeverity severity, String title, String message, LocalDate dueDate) {
    PreventionAlert alert = new PreventionAlert();
    alert.setHealthProfile(profile);
    alert.setType(type);
    alert.setSeverity(severity);
    alert.setTitle(title);
    alert.setMessage(message);
    alert.setDueDate(dueDate);
    alerts.save(alert);
  }

  private AlertResponse toResponse(PreventionAlert alert) {
    return new AlertResponse(alert.getId(), alert.getType().name(), alert.getSeverity().name(), alert.getTitle(), alert.getMessage(), alert.getStatus().name(), alert.getDueDate());
  }

  public record AlertResponse(UUID id, String type, String severity, String title, String message, String status, LocalDate dueDate) {}
}
