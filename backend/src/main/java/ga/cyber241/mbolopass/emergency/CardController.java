package ga.cyber241.mbolopass.emergency;

import ga.cyber241.mbolopass.emergency.CardService.CardResponse;
import ga.cyber241.mbolopass.emergency.CardService.EmergencyResponse;
import ga.cyber241.mbolopass.emergency.CardService.QrTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CardController {
  private final CardService service;
  public CardController(CardService service) { this.service = service; }

  @GetMapping("/api/v1/card/me")
  CardResponse card(Principal principal) { return service.card(principal.getName()); }

  @PostMapping("/api/v1/card/me/qr-token")
  QrTokenResponse token(Principal principal) { return service.regenerate(principal.getName()); }

  @PostMapping("/api/v1/card/me/qr-token/revoke")
  void revoke(Principal principal) { service.revoke(principal.getName()); }

  @GetMapping("/api/v1/public/emergency/{token}")
  EmergencyResponse emergency(@PathVariable String token, HttpServletRequest request) {
    return service.emergency(token, request.getRemoteAddr());
  }
}
