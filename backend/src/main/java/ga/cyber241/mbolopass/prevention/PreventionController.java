package ga.cyber241.mbolopass.prevention;

import ga.cyber241.mbolopass.prevention.PreventionService.AlertResponse;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prevention")
public class PreventionController {
  private final PreventionService service;
  public PreventionController(PreventionService service) { this.service = service; }
  @PostMapping("/refresh") List<AlertResponse> refresh(Principal principal) { return service.refresh(principal.getName()); }
  @GetMapping("/alerts") List<AlertResponse> alerts(Principal principal) { return service.list(principal.getName()); }
  @PatchMapping("/alerts/{id}/dismiss") AlertResponse dismiss(Principal principal, @PathVariable UUID id) { return service.dismiss(principal.getName(), id); }
}
