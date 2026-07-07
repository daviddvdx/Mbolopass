package ga.cyber241.mbolopass.auth;

import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.common.Enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expirationMinutes;

  public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMinutes = expirationMinutes;
  }

  public String generate(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(user.getEmail())
        .claim("role", user.getRole().name())
        .claim("roles", List.of(authority(user.getRole())))
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
        .signWith(key)
        .compact();
  }

  public String subject(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  public String jti(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getId();
  }

  public Instant expiresAt(String token) {
    Date expiration = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getExpiration();
    return expiration.toInstant();
  }

  public long expirationMinutes() {
    return expirationMinutes;
  }

  public static String authority(Role role) {
    return "ROLE_" + role.name();
  }
}
