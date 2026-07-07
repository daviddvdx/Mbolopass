package ga.cyber241.mbolopass.auth;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenRevocationService {
  private final RevokedTokenRepository revokedTokens;

  public TokenRevocationService(RevokedTokenRepository revokedTokens) {
    this.revokedTokens = revokedTokens;
  }

  @Transactional
  public void revoke(String token, JwtService jwtService) {
    String jti = jwtService.jti(token);
    if (jti == null || jti.isBlank() || revokedTokens.existsById(jti)) return;
    RevokedToken revoked = new RevokedToken();
    revoked.setJti(jti);
    revoked.setExpiresAt(jwtService.expiresAt(token));
    revoked.setRevokedAt(Instant.now());
    revokedTokens.save(revoked);
  }

  @Transactional(readOnly = true)
  public boolean isRevoked(String token, JwtService jwtService) {
    String jti = jwtService.jti(token);
    return jti != null && revokedTokens.existsById(jti);
  }
}
