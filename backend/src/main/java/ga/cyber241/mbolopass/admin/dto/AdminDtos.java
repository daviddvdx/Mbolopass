package ga.cyber241.mbolopass.admin.dto;

import ga.cyber241.mbolopass.common.Enums.QrTokenStatus;
import ga.cyber241.mbolopass.common.Enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {
  private AdminDtos() {}

  public record DashboardResponse(long totalUsers, long activeUsers, long blockedUsers, long patients, long professionals, long administrators, long activeQrCards, long revokedQrCards, long expiredQrCards, long emergencyAccessesLast24Hours, long dependentProfiles, long medicalDocuments, long activeDependentQrCards) {}
  public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
  public record AdminUserSummary(UUID id, String firstName, String lastName, String email, Role role, boolean enabled, Instant createdAt, Instant updatedAt, boolean healthProfileExists, boolean activeQrCardExists) {}
  public record AdminUserDetail(UUID id, String firstName, String lastName, String email, Role role, boolean enabled, Instant createdAt, Instant updatedAt, boolean healthProfileExists, boolean activeQrCardExists) {}
  public record CreateUserRequest(@NotBlank String firstName, @NotBlank String lastName, @Email @NotBlank String email, @Size(min = 8) String password, Role role) {}
  public record StatusRequest(@NotNull Boolean enabled, String reason) {}
  public record RoleRequest(@NotNull Role role) {}
  public record QrCardSummary(UUID qrTokenId, UUID ownerUserId, String ownerType, String ownerDisplayName, String tokenPrefix, QrTokenStatus status, Instant createdAt, Instant expiresAt, Instant revokedAt, long scanCount, Instant lastEmergencyAccessAt) {}
  public record ReasonRequest(String reason) {}
  public record AuditLogResponse(UUID id, String actorName, String action, String targetType, UUID targetId, String details, Instant createdAt) {}
}
