package ga.cyber241.mbolopass.user;

import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.health.HealthProfileService.PhotoFile;
import ga.cyber241.mbolopass.health.HealthProfileService.PhotoResponse;
import ga.cyber241.mbolopass.user.UserProfileService.UpdateUserProfileRequest;
import ga.cyber241.mbolopass.user.UserProfileService.UserProfileResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile/me")
public class UserProfileController {
  private final UserProfileService profiles;
  private final HealthProfileService healthProfiles;

  public UserProfileController(UserProfileService profiles, HealthProfileService healthProfiles) {
    this.profiles = profiles;
    this.healthProfiles = healthProfiles;
  }

  @GetMapping
  UserProfileResponse me(Principal principal) { return profiles.current(principal.getName()); }

  @PutMapping
  UserProfileResponse update(Principal principal, @Valid @RequestBody UpdateUserProfileRequest request) {
    return profiles.update(principal.getName(), request);
  }

  @PostMapping(path = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  PhotoResponse uploadPhoto(Principal principal, @RequestPart("file") MultipartFile file) {
    return healthProfiles.uploadPhoto(principal.getName(), file);
  }

  @GetMapping("/photo")
  ResponseEntity<byte[]> photo(Principal principal) {
    PhotoFile file = healthProfiles.photo(principal.getName());
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mimeType())).body(file.bytes());
  }

  @DeleteMapping("/photo")
  void deletePhoto(Principal principal) { healthProfiles.deletePhoto(principal.getName()); }
}
