import { api } from './client';
import type { AdminAuditLog, AdminDashboard, AdminQrCard, AdminUserSummary, CreateAdminUserRequest, PageResponse, Role } from '../types';

function params(values: Record<string, string | number | undefined>) {
  const search = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value));
  });
  const qs = search.toString();
  return qs ? `?${qs}` : '';
}

export function getAdminDashboard(token: string) {
  return api<AdminDashboard>('/admin/dashboard', { token });
}

export function listAdminUsers(token: string, query: { page?: number; size?: number; search?: string; role?: string; status?: string }) {
  return api<PageResponse<AdminUserSummary>>(`/admin/users${params(query)}`, { token });
}

export function createAdminUser(token: string, payload: CreateAdminUserRequest) {
  return api<AdminUserSummary>('/admin/users', { method: 'POST', token, body: JSON.stringify(payload) });
}

export function updateAdminUserStatus(token: string, userId: string, enabled: boolean, reason: string) {
  return api<AdminUserSummary>(`/admin/users/${userId}/status`, { method: 'PATCH', token, body: JSON.stringify({ enabled, reason }) });
}

export function updateAdminUserRole(token: string, userId: string, role: Role) {
  return api<AdminUserSummary>(`/admin/users/${userId}/role`, { method: 'PATCH', token, body: JSON.stringify({ role }) });
}

export function listAdminQrCards(token: string, query: { page?: number; size?: number; status?: string; search?: string }) {
  return api<PageResponse<AdminQrCard>>(`/admin/qr-cards${params(query)}`, { token });
}

export function revokeAdminQrCard(token: string, qrTokenId: string, reason: string) {
  return api<AdminQrCard>(`/admin/qr-cards/${qrTokenId}/revoke`, { method: 'PATCH', token, body: JSON.stringify({ reason }) });
}

export function listAdminAuditLogs(token: string, query: { page?: number; size?: number; action?: string; actorId?: string }) {
  return api<PageResponse<AdminAuditLog>>(`/admin/audit-logs${params(query)}`, { token });
}