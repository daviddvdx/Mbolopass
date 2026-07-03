package ga.cyber241.mbolopass.auth;

import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final HealthProfileService healthProfiles;

  public AuthController(UserRepository users, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, HealthProfileService healthProfiles) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.healthProfiles = healthProfiles;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    if (users.existsByEmail(request.email().toLowerCase())) throw new ApiException(HttpStatus.CONFLICT, "Email deja utilise");
    User user = new User();
    user.setEmail(request.email());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setRole(Role.PATIENT);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    users.save(user);
    healthProfiles.ensureFor(user);
    return response(user);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
    return response(users.findByEmail(request.email().toLowerCase()).orElseThrow());
  }

  @GetMapping("/me")
  public MeResponse me(Principal principal) {
    return meResponse(users.findByEmail(principal.getName()).orElseThrow());
  }

  private AuthResponse response(User user) {
    return new AuthResponse(jwtService.generate(user), "Bearer", jwtService.expirationMinutes(), meResponse(user));
  }

  private MeResponse meResponse(User user) {
    return new MeResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), Set.of("ROLE_" + user.getRole().name()));
  }
}
