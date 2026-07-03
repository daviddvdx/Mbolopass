package ga.cyber241.mbolopass.ai;

import ga.cyber241.mbolopass.ai.AiSummaryService.SummaryResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-summary")
public class AiSummaryController {
  private final AiSummaryService service;
  public AiSummaryController(AiSummaryService service) { this.service = service; }
  @PostMapping("/regenerate") SummaryResponse regenerate(Principal principal) { return service.regenerate(principal.getName()); }
  @GetMapping("/latest") SummaryResponse latest(Principal principal) { return service.latest(principal.getName()); }
}
