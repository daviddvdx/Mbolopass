package ga.cyber241.mbolopass.auth;

import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.clinical.ClinicalService;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.medical.MedicalRecordService;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
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
  private final ClinicalService clinicalService;
  private final MedicalRecordService medicalRecords;
  private final TokenRevocationService tokenRevocationService;

  public AuthController(UserRepository users, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, HealthProfileService healthProfiles, ClinicalService clinicalService, MedicalRecordService medicalRecords, TokenRevocationService tokenRevocationService) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.healthProfiles = healthProfiles;
    this.clinicalService = clinicalService;
    this.medicalRecords = medicalRecords;
    this.tokenRevocationService = tokenRevocationService;
  }

  @PostMapping("/register-professional")
  @Transactional
  public AuthResponse registerProfessional(@Valid @RequestBody ProfessionalRegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Email deja utilise");
    User user = new User();
    user.setEmail(email);
    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setRole(Role.HEALTH_PROFESSIONAL);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    users.save(user);
    clinicalService.createProfessionalProfile(user, new ClinicalService.ProfessionalRegisterRequest(request.professionalType(), request.speciality(), request.licenseNumber(), request.organizationName()));
    return response(user);
  }

  @PostMapping("/register")
  @Transactional
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Email deja utilise");
    User user = new User();
    user.setEmail(email);
    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setRole(Role.PATIENT);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    users.save(user);
    healthProfiles.ensureFor(user);
    medicalRecords.ensureForPatient(user);
    return response(user);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    String email = normalizeEmail(request.email());
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
    return response(users.findByEmail(email).orElseThrow());
  }

  @GetMapping("/me")
  public MeResponse me(Principal principal) {
    return meResponse(users.findByEmail(principal.getName()).orElseThrow());
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      tokenRevocationService.revoke(header.substring(7), jwtService);
    }
  }

  private AuthResponse response(User user) {
    return new AuthResponse(jwtService.generate(user), "Bearer", jwtService.expirationMinutes(), meResponse(user));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private MeResponse meResponse(User user) {
    return new MeResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), Set.of(JwtService.authority(user.getRole())), clinicalService.identityFor(user));
  }
}
