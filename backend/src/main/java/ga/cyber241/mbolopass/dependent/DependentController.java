package ga.cyber241.mbolopass.dependent;

import ga.cyber241.mbolopass.dependent.DependentService.DependentRequest;
import ga.cyber241.mbolopass.dependent.DependentService.DependentResponse;
import ga.cyber241.mbolopass.dependent.DependentService.ItemRequest;
import ga.cyber241.mbolopass.dependent.DependentService.ItemResponse;
import ga.cyber241.mbolopass.dependent.DependentService.PhotoFile;
import ga.cyber241.mbolopass.dependent.DependentService.PhotoResponse;
import ga.cyber241.mbolopass.dependent.DependentService.StatusRequest;
import ga.cyber241.mbolopass.emergency.CardService;
import ga.cyber241.mbolopass.emergency.CardService.QrTokenResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/dependents")
public class DependentController {
  private final DependentService service;
  private final CardService cards;

  public DependentController(DependentService service, CardService cards) {
    this.service = service;
    this.cards = cards;
  }

  @GetMapping
  List<DependentResponse> list(Principal principal) { return service.list(principal.getName()); }

  @PostMapping
  DependentResponse create(Principal principal, @Valid @RequestBody DependentRequest request) { return service.create(principal.getName(), request); }

  @GetMapping("/{dependentId}")
  DependentResponse get(Principal principal, @PathVariable UUID dependentId) { return service.get(principal.getName(), dependentId); }

  @PutMapping("/{dependentId}")
  DependentResponse update(Principal principal, @PathVariable UUID dependentId, @Valid @RequestBody DependentRequest request) { return service.update(principal.getName(), dependentId, request); }

  @PatchMapping("/{dependentId}/status")
  DependentResponse status(Principal principal, @PathVariable UUID dependentId, @RequestBody StatusRequest request) { return service.status(principal.getName(), dependentId, request); }

  @DeleteMapping("/{dependentId}")
  void delete(Principal principal, @PathVariable UUID dependentId) { service.disable(principal.getName(), dependentId); }

  @GetMapping("/{dependentId}/{type:allergies|conditions|medications|emergency-contacts}")
  List<ItemResponse> listItems(Principal principal, @PathVariable UUID dependentId, @PathVariable String type) { return service.listItems(principal.getName(), dependentId, type); }

  @PostMapping("/{dependentId}/{type:allergies|conditions|medications|emergency-contacts}")
  ItemResponse addItem(Principal principal, @PathVariable UUID dependentId, @PathVariable String type, @RequestBody ItemRequest request) { return service.addItem(principal.getName(), dependentId, type, request); }

  @DeleteMapping("/{dependentId}/{type:allergies|conditions|medications|emergency-contacts}/{itemId}")
  void deleteItem(Principal principal, @PathVariable UUID dependentId, @PathVariable String type, @PathVariable UUID itemId) { service.deleteItem(principal.getName(), dependentId, type, itemId); }

  @PostMapping(path = "/{dependentId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  PhotoResponse uploadPhoto(Principal principal, @PathVariable UUID dependentId, @RequestPart("file") MultipartFile file) { return service.uploadPhoto(principal.getName(), dependentId, file); }

  @GetMapping("/{dependentId}/photo")
  ResponseEntity<byte[]> photo(Principal principal, @PathVariable UUID dependentId) {
    PhotoFile file = service.photo(principal.getName(), dependentId);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mimeType())).body(file.bytes());
  }

  @DeleteMapping("/{dependentId}/photo")
  void deletePhoto(Principal principal, @PathVariable UUID dependentId) { service.deletePhoto(principal.getName(), dependentId); }

  @PostMapping("/{dependentId}/qr-token")
  QrTokenResponse qrToken(Principal principal, @PathVariable UUID dependentId) { return cards.regenerateDependent(principal.getName(), dependentId); }
}
