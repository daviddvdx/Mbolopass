import { api } from './client';
import type { Alert, Summary } from '../types';

export function refreshAlerts(token: string) {
  return api<Alert[]>('/prevention/refresh', { method: 'POST', token });
}

export function listAlerts(token: string) {
  return api<Alert[]>('/prevention/alerts', { token });
}

export function dismissAlert(token: string, id: string) {
  return api<Alert>(`/prevention/alerts/${id}/dismiss`, { method: 'PATCH', token });
}

export function regenerateSummary(token: string) {
  return api<Summary>('/ai-summary/regenerate', { method: 'POST', token });
}

export function latestSummary(token: string) {
  return api<Summary>('/ai-summary/latest', { token });
}