package ga.cyber241.mbolopass.admin.service;

import ga.cyber241.mbolopass.admin.audit.AdminAuditLog;
import ga.cyber241.mbolopass.admin.audit.AdminAuditLogRepository;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.AdminUserDetail;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.AdminUserSummary;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.AuditLogResponse;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.CreateUserRequest;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.DashboardResponse;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.PageResponse;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.QrCardSummary;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.RoleRequest;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.StatusRequest;
import ga.cyber241.mbolopass.common.Enums.QrTokenStatus;
import ga.cyber241.mbolopass.common.Enums.Role;
import ga.cyber241.mbolopass.dependent.DependentService;
import ga.cyber241.mbolopass.document.MedicalDocumentService;
import ga.cyber241.mbolopass.emergency.QrToken;
import ga.cyber241.mbolopass.exception.ApiException;
import ga.cyber241.mbolopass.health.HealthProfileService;
import ga.cyber241.mbolopass.health.HealthProfileRepository;
import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private final UserRepository users;
  private final HealthProfileRepository profiles;
  private final HealthProfileService healthProfiles;
  private final AdminQrTokenRepository qrTokens;
  private final AdminAuditLogRepository auditLogs;
  private final DependentService dependents;
  private final MedicalDocumentService documents;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;

  public AdminService(UserRepository users, HealthProfileRepository profiles, HealthProfileService healthProfiles, AdminQrTokenRepository qrTokens, AdminAuditLogRepository auditLogs, DependentService dependents, MedicalDocumentService documents, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
    this.users = users;
    this.profiles = profiles;
    this.healthProfiles = healthProfiles;
    this.qrTokens = qrTokens;
    this.auditLogs = auditLogs;
    this.dependents = dependents;
    this.documents = documents;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard() {
    long total = users.count();
    long active = users.countByEnabledTrue();
    return new DashboardResponse(
        total,
        active,
        total - active,
        users.countByRole(Role.PATIENT),
        users.countByRole(Role.PROFESSIONAL),
        users.countByRole(Role.ADMIN),
        qrTokens.countByStatus(QrTokenStatus.ACTIVE),
        qrTokens.countByStatus(QrTokenStatus.REVOKED),
        qrTokens.countByStatus(QrTokenStatus.EXPIRED),
        emergencyAccessesLast24Hours(),
        dependents.countEnabled(),
        documents.countActive(),
        qrTokens.countByDependentProfileIsNotNullAndStatus(QrTokenStatus.ACTIVE));
  }

  @Transactional(readOnly = true)
  public PageResponse<AdminUserSummary> users(int page, int size, String search, String role, String status) {
    List<User> filtered = users.findAll().stream()
        .filter(user -> matchesSearch(user, search))
        .filter(user -> role == null || role.isBlank() || user.getRole().name().equalsIgnoreCase(role))
        .filter(user -> status == null || status.isBlank() || matchesStatus(user, status))
        .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    int safeSize = safeSize(size);
    int safePage = Math.max(page, 0);
    List<User> visibleUsers = slice(filtered, safePage, safeSize);
    Set<UUID> visibleIds = visibleUsers.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
    Set<UUID> profileUserIds = visibleIds.isEmpty() ? Set.of() : profiles.findUserIdsWithProfile(visibleIds);
    Set<UUID> activeQrUserIds = visibleIds.isEmpty() ? Set.of() : qrTokens.findHealthProfileUserIdsWithStatus(visibleIds, QrTokenStatus.ACTIVE);
    List<AdminUserSummary> content = visibleUsers.stream()
        .map(user -> toSummary(user, profileUserIds, activeQrUserIds))
        .toList();
    return pageFromContent(content, safePage, safeSize, filtered.size());
  }

  @Transactional(readOnly = true)
  public AdminUserDetail user(UUID userId) {
    return toDetail(findUser(userId));
  }

  @Transactional
  public AdminUserDetail createUser(String actorEmail, CreateUserRequest request, String sourceIp) {
    Role role = request.role() == null ? Role.PATIENT : request.role();
    if (role == Role.ADMIN) throw new ApiException(HttpStatus.BAD_REQUEST, "Creation ADMIN interdite par cette route");
    String email = request.email().toLowerCase(Locale.ROOT).trim();
    if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Email deja utilise");
    User user = new User();
    user.setEmail(email);
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setRole(role);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    users.save(user);
    if (role == Role.PATIENT) healthProfiles.ensureFor(user);
    audit(actorEmail, "USER_CREATED", "USER", user.getId(), "role=" + role, sourceIp);
    return toDetail(user);
  }

  @Transactional
  public AdminUserDetail updateStatus(String actorEmail, UUID userId, StatusRequest request, String sourceIp) {
    User actor = actor(actorEmail);
    User target = findUser(userId);
    if (actor.getId().equals(target.getId())) throw new ApiException(HttpStatus.BAD_REQUEST, "Un administrateur ne peut pas bloquer son propre compte");
    if (target.getRole() == Role.ADMIN && target.isEnabled() && !request.enabled() && activeAdmins() <= 1) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Impossible de bloquer le dernier ADMIN actif");
    }
    target.setEnabled(request.enabled());
    users.save(target);
    audit(actorEmail, request.enabled() ? "USER_UNBLOCKED" : "USER_BLOCKED", "USER", target.getId(), safeReason(request.reason()), sourceIp);
    return toDetail(target);
  }

  @Transactional
  public AdminUserDetail updateRole(String actorEmail, UUID userId, RoleRequest request, String sourceIp) {
    User target = findUser(userId);
    Role previous = target.getRole();
    Role next = request.role();
    if (previous == Role.ADMIN && next != Role.ADMIN && target.isEnabled() && activeAdmins() <= 1) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Impossible de retirer le role du dernier ADMIN actif");
    }
    target.setRole(next);
    users.save(target);
    if (next == Role.PATIENT) healthProfiles.ensureFor(target);
    audit(actorEmail, "USER_ROLE_CHANGED", "USER", target.getId(), previous + "->" + next, sourceIp);
    return toDetail(target);
  }

  @Transactional(readOnly = true)
  public PageResponse<QrCardSummary> qrCards(int page, int size, String status, String search) {
    List<QrToken> filtered = qrTokens.findAll().stream()
        .filter(token -> status == null || status.isBlank() || token.getStatus().name().equalsIgnoreCase(status))
        .filter(token -> matchesOwner(token, search))
        .sorted(Comparator.comparing(QrToken::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    int safePage = Math.max(page, 0);
    int safeSize = safeSize(size);
    List<QrCardSummary> content = slice(filtered, safePage, safeSize).stream().map(this::toQrSummary).toList();
    return pageFromContent(content, safePage, safeSize, filtered.size());
  }

  @Transactional
  public QrCardSummary revokeQr(String actorEmail, UUID qrTokenId, String reason, String sourceIp) {
    QrToken token = qrTokens.findById(qrTokenId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "QR introuvable"));
    if (token.getStatus() != QrTokenStatus.REVOKED) token.revoke();
    qrTokens.save(token);
    audit(actorEmail, "QR_REVOKED", "QR_TOKEN", token.getId(), safeReason(reason), sourceIp);
    return toQrSummary(token);
  }

  @Transactional(readOnly = true)
  public PageResponse<AuditLogResponse> auditLogs(int page, int size, String action, UUID actorId) {
    List<AdminAuditLog> filtered = auditLogs.findAll().stream()
        .filter(log -> action == null || action.isBlank() || log.getAction().equalsIgnoreCase(action))
        .filter(log -> actorId == null || log.getActor().getId().equals(actorId))
        .sorted(Comparator.comparing(AdminAuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    int safePage = Math.max(page, 0);
    int safeSize = safeSize(size);
    List<AuditLogResponse> content = slice(filtered, safePage, safeSize).stream()
        .map(log -> new AuditLogResponse(log.getId(), displayName(log.getActor()), log.getAction(), log.getTargetType(), log.getTargetId(), log.getDetails(), log.getCreatedAt()))
        .toList();
    return pageFromContent(content, safePage, safeSize, filtered.size());
  }

  private void audit(String actorEmail, String action, String targetType, UUID targetId, String details, String sourceIp) {
    AdminAuditLog log = new AdminAuditLog();
    log.setActor(actor(actorEmail));
    log.setAction(action);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDetails(details);
    log.setSourceIpHash(sourceIp == null ? null : sha256(sourceIp));
    auditLogs.save(log);
  }

  private User actor(String email) {
    return users.findByEmail(email.toLowerCase(Locale.ROOT)).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide"));
  }

  private User findUser(UUID id) {
    return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
  }

  private AdminUserSummary toSummary(User user) {
    return new AdminUserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt(), user.getUpdatedAt(), profileExists(user), activeQrExists(user));
  }

  private AdminUserSummary toSummary(User user, Set<UUID> profileUserIds, Set<UUID> activeQrUserIds) {
    return new AdminUserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt(), user.getUpdatedAt(), profileUserIds.contains(user.getId()), activeQrUserIds.contains(user.getId()));
  }

  private AdminUserDetail toDetail(User user) {
    return new AdminUserDetail(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt(), user.getUpdatedAt(), profileExists(user), activeQrExists(user));
  }

  private QrCardSummary toQrSummary(QrToken token) {
    if (token.getDependentProfile() != null) {
      User guardian = token.getDependentProfile().getGuardian();
      String display = (token.getDependentProfile().getFirstName() + " " + token.getDependentProfile().getLastName()).trim();
      return new QrCardSummary(token.getId(), guardian.getId(), "DEPENDENT", display, token.getTokenPrefix(), token.getStatus(), token.getCreatedAt(), token.getExpiresAt(), token.getRevokedAt(), scanCount(token.getId()), lastAccess(token.getId()));
    }
    User owner = token.getHealthProfile().getUser();
    return new QrCardSummary(token.getId(), owner.getId(), "PATIENT", displayName(owner), token.getTokenPrefix(), token.getStatus(), token.getCreatedAt(), token.getExpiresAt(), token.getRevokedAt(), scanCount(token.getId()), lastAccess(token.getId()));
  }

  private boolean profileExists(User user) { return profiles.findByUserId(user.getId()).isPresent(); }
  private boolean activeQrExists(User user) { return !qrTokens.findByHealthProfileUserIdAndStatus(user.getId(), QrTokenStatus.ACTIVE).isEmpty(); }
  private long activeAdmins() { return users.countByRoleAndEnabledTrue(Role.ADMIN); }

  private boolean matchesSearch(User user, String search) {
    if (search == null || search.isBlank()) return true;
    String value = search.toLowerCase(Locale.ROOT);
    return (user.getEmail() != null && user.getEmail().contains(value))
        || (user.getFirstName() != null && user.getFirstName().toLowerCase(Locale.ROOT).contains(value))
        || (user.getLastName() != null && user.getLastName().toLowerCase(Locale.ROOT).contains(value));
  }

  private boolean matchesStatus(User user, String status) {
    return switch (status.toUpperCase(Locale.ROOT)) {
      case "ACTIVE", "ENABLED" -> user.isEnabled();
      case "BLOCKED", "DISABLED" -> !user.isEnabled();
      default -> true;
    };
  }

  private boolean matchesOwner(QrToken token, String search) {
    if (search == null || search.isBlank()) return true;
    String value = search.toLowerCase(Locale.ROOT);
    if (token.getDependentProfile() != null) {
      String dependentName = (token.getDependentProfile().getFirstName() + " " + token.getDependentProfile().getLastName()).toLowerCase(Locale.ROOT);
      return dependentName.contains(value);
    }
    User owner = token.getHealthProfile().getUser();
    return displayName(owner).toLowerCase(Locale.ROOT).contains(value) || owner.getEmail().contains(value);
  }

  private long scanCount(UUID qrTokenId) {
    Long count = jdbcTemplate.queryForObject("select count(*) from emergency_access_logs where qr_token_id = ?", Long.class, qrTokenId);
    return count == null ? 0 : count;
  }

  private Instant lastAccess(UUID qrTokenId) {
    List<Instant> values = jdbcTemplate.query("select accessed_at from emergency_access_logs where qr_token_id = ? order by accessed_at desc limit 1", (rs, rowNum) -> rs.getTimestamp(1).toInstant(), qrTokenId);
    return values.isEmpty() ? null : values.get(0);
  }

  private long emergencyAccessesLast24Hours() {
    Long count = jdbcTemplate.queryForObject("select count(*) from emergency_access_logs where accessed_at >= ?", Long.class, Instant.now().minus(24, ChronoUnit.HOURS));
    return count == null ? 0 : count;
  }

  private String displayName(User user) {
    return ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
  }

  private String safeReason(String reason) {
    if (reason == null || reason.isBlank()) return null;
    return reason.length() > 500 ? reason.substring(0, 500) : reason;
  }

  private <T> PageResponse<T> page(List<T> items, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = safeSize(size);
    return pageFromContent(slice(items, safePage, safeSize), safePage, safeSize, items.size());
  }

  private int safeSize(int size) {
    return Math.min(Math.max(size, 1), 100);
  }

  private <T> List<T> slice(List<T> items, int safePage, int safeSize) {
    int from = Math.min(safePage * safeSize, items.size());
    int to = Math.min(from + safeSize, items.size());
    return items.subList(from, to);
  }

  private <T> PageResponse<T> pageFromContent(List<T> content, int safePage, int safeSize, long totalElements) {
    int totalPages = (int) Math.ceil(totalElements / (double) safeSize);
    return new PageResponse<>(content, safePage, safeSize, totalElements, totalPages);
  }

  private String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Hash impossible", ex);
    }
  }
}
