package ga.cyber241.mbolopass.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "revoked_jwt_tokens")
public class RevokedToken {
  @Id
  private String jti;
  private Instant expiresAt;
  private Instant revokedAt;

  public String getJti() { return jti; }
  public void setJti(String jti) { this.jti = jti; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
