package ga.cyber241.mbolopass.health;

import ga.cyber241.mbolopass.health.HealthProfileService.ItemRequest;
import ga.cyber241.mbolopass.health.HealthProfileService.ItemResponse;
import ga.cyber241.mbolopass.health.HealthProfileService.PhotoFile;
import ga.cyber241.mbolopass.health.HealthProfileService.PhotoResponse;
import ga.cyber241.mbolopass.health.HealthProfileService.ProfileRequest;
import ga.cyber241.mbolopass.health.HealthProfileService.ProfileResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/health-profile/me")
public class HealthProfileController {
  private final HealthProfileService service;

  public HealthProfileController(HealthProfileService service) { this.service = service; }

  @GetMapping
  ProfileResponse me(Principal principal) { return service.toProfile(service.current(principal.getName())); }

  @PutMapping
  ProfileResponse update(Principal principal, @Valid @RequestBody ProfileRequest request) { return service.update(principal.getName(), request); }

  @GetMapping("/{type:allergies|conditions|medications|vaccinations|emergency-contacts}")
  List<ItemResponse> list(Principal principal, @PathVariable String type) { return service.list(principal.getName(), type); }

  @PostMapping("/{type:allergies|conditions|medications|vaccinations|emergency-contacts}")
  ItemResponse add(Principal principal, @PathVariable String type, @RequestBody ItemRequest request) { return service.add(principal.getName(), type, request); }

  @DeleteMapping("/{type:allergies|conditions|medications|vaccinations|emergency-contacts}/{id}")
  void delete(Principal principal, @PathVariable String type, @PathVariable UUID id) { service.delete(principal.getName(), type, id); }

  @PostMapping(path = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  PhotoResponse uploadPhoto(Principal principal, @RequestPart("file") MultipartFile file) { return service.uploadPhoto(principal.getName(), file); }

  @GetMapping("/photo")
  ResponseEntity<byte[]> photo(Principal principal) {
    PhotoFile file = service.photo(principal.getName());
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mimeType())).body(file.bytes());
  }

  @DeleteMapping("/photo")
  void deletePhoto(Principal principal) { service.deletePhoto(principal.getName()); }
}
