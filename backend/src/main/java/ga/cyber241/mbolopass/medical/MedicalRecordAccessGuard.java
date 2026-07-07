package ga.cyber241.mbolopass.medical;

import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.clinical.ClinicalService;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.medical.MedicalEnums.MedicalAccessGrantStatus;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRecordAccessGuard {
  private final UserRepository users;
  private final MedicalRecordAccessGrantRepository grants;
  private final ClinicalService clinicalService;

  public MedicalRecordAccessGuard(UserRepository users, MedicalRecordAccessGrantRepository grants, ClinicalService clinicalService) {
    this.users = users;
    this.grants = grants;
    this.clinicalService = clinicalService;
  }

  @Transactional(readOnly = true)
  public User requireProfessional(String email) {
    User user = users.findByEmail(email.toLowerCase(Locale.ROOT)).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide"));
    if (user.getRole() != Role.HEALTH_PROFESSIONAL || !user.isEnabled()) throw new ApiException(HttpStatus.FORBIDDEN, "Acces professionnel requis");
    if (!"APPROVED".equals(clinicalService.professionalMe(user.getEmail()).verificationStatus())) throw new ApiException(HttpStatus.FORBIDDEN, "Professionnel non approuve");
    return user;
  }

  @Transactional(readOnly = true)
  public MedicalRecordAccessGrant requireActiveAccess(UUID patientId, String professionalEmail) {
    User professional = requireProfessional(professionalEmail);
    return grants.findFirstByPatientIdAndHealthProfessionalIdAndStatusAndExpiresAtAfter(patientId, professional.getId(), MedicalAccessGrantStatus.ACTIVE, Instant.now())
        .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Autorisation medicale active requise"));
  }
}
