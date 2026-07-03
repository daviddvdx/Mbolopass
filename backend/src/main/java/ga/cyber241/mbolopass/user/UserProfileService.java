package ga.cyber241.mbolopass.user;

import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfile;
import ga.cyber241.mbolopass.health.HealthProfileService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {
  private static final String PHOTO_ENDPOINT = "/api/v1/profile/me/photo";
  private final UserRepository users;
  private final HealthProfileService healthProfiles;

  public UserProfileService(UserRepository users, HealthProfileService healthProfiles) {
    this.users = users;
    this.healthProfiles = healthProfiles;
  }

  @Transactional
  public UserProfileResponse current(String email) {
    User user = actor(email);
    HealthProfile profile = healthProfiles.ensureFor(user);
    return toResponse(user, profile);
  }

  @Transactional
  public UserProfileResponse update(String email, UpdateUserProfileRequest request) {
    User user = actor(email);
    HealthProfile profile = healthProfiles.ensureFor(user);
    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    profile.setBirthDate(request.birthDate());
    profile.setGender(blankToNull(request.gender()));
    return toResponse(users.save(user), profile);
  }

  private User actor(String email) {
    return users.findByEmail(email.toLowerCase()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide"));
  }

  private UserProfileResponse toResponse(User user, HealthProfile profile) {
    return new UserProfileResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        null,
        profile.getBirthDate(),
        profile.getGender(),
        profile.getProfilePhotoUrl() == null || profile.getProfilePhotoUrl().isBlank() ? null : PHOTO_ENDPOINT);
  }

  private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

  public record UserProfileResponse(UUID id, String firstName, String lastName, String email, String phone, LocalDate birthDate, String gender, String profilePhotoUrl) {}
  public record UpdateUserProfileRequest(
      @NotBlank @Size(max = 80) String firstName,
      @NotBlank @Size(max = 80) String lastName,
      @Pattern(regexp = "^[0-9+() .-]{6,30}$") String phone,
      LocalDate birthDate,
      @Size(max = 30) String gender) {}
}
