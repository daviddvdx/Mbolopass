package ga.cyber241.mbolopass.admin.controller;

import ga.cyber241.mbolopass.admin.dto.AdminDtos.AdminUserDetail;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.AdminUserSummary;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.AuditLogResponse;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.CreateUserRequest;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.DashboardResponse;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.PageResponse;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.QrCardSummary;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.ReasonRequest;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.RoleRequest;
import ga.cyber241.mbolopass.admin.dto.AdminDtos.StatusRequest;
import ga.cyber241.mbolopass.admin.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final AdminService service;

  public AdminController(AdminService service) { this.service = service; }

  @GetMapping("/dashboard")
  DashboardResponse dashboard() { return service.dashboard(); }

  @GetMapping("/users")
  PageResponse<AdminUserSummary> users(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String role, @RequestParam(defaultValue = "") String status) {
    return service.users(page, size, search, role, status);
  }

  @GetMapping("/users/{userId}")
  AdminUserDetail user(@PathVariable UUID userId) { return service.user(userId); }

  @PostMapping("/users")
  AdminUserDetail createUser(Principal principal, HttpServletRequest request, @Valid @RequestBody CreateUserRequest body) {
    return service.createUser(principal.getName(), body, request.getRemoteAddr());
  }

  @PatchMapping("/users/{userId}/status")
  AdminUserDetail status(Principal principal, HttpServletRequest request, @PathVariable UUID userId, @Valid @RequestBody StatusRequest body) {
    return service.updateStatus(principal.getName(), userId, body, request.getRemoteAddr());
  }

  @PatchMapping("/users/{userId}/role")
  AdminUserDetail role(Principal principal, HttpServletRequest request, @PathVariable UUID userId, @Valid @RequestBody RoleRequest body) {
    return service.updateRole(principal.getName(), userId, body, request.getRemoteAddr());
  }

  @GetMapping("/qr-cards")
  PageResponse<QrCardSummary> qrCards(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "") String search) {
    return service.qrCards(page, size, status, search);
  }

  @PatchMapping("/qr-cards/{qrTokenId}/revoke")
  QrCardSummary revokeQr(Principal principal, HttpServletRequest request, @PathVariable UUID qrTokenId, @RequestBody(required = false) ReasonRequest body) {
    return service.revokeQr(principal.getName(), qrTokenId, body == null ? null : body.reason(), request.getRemoteAddr());
  }

  @GetMapping("/audit-logs")
  PageResponse<AuditLogResponse> auditLogs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size, @RequestParam(defaultValue = "") String action, @RequestParam(required = false) UUID actorId) {
    return service.auditLogs(page, size, action, actorId);
  }
}