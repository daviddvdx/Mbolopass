import { api, API_BASE_URL } from './client';
import type { Dependent, DependentRequest, HealthItemType, Item, ItemRequest, QrTokenResponse } from '../types';

export function listDependents(token: string) {
  return api<Dependent[]>('/dependents', { token });
}

export function createDependent(token: string, payload: DependentRequest) {
  return api<Dependent>('/dependents', { method: 'POST', token, body: JSON.stringify(payload) });
}

export function updateDependent(token: string, id: string, payload: DependentRequest) {
  return api<Dependent>(`/dependents/${id}`, { method: 'PUT', token, body: JSON.stringify(payload) });
}

export function listDependentItems(token: string, dependentId: string, type: Exclude<HealthItemType, 'vaccinations'>) {
  return api<Item[]>(`/dependents/${dependentId}/${type}`, { token });
}

export function addDependentItem(token: string, dependentId: string, type: Exclude<HealthItemType, 'vaccinations'>, payload: ItemRequest) {
  return api<Item>(`/dependents/${dependentId}/${type}`, { method: 'POST', token, body: JSON.stringify(payload) });
}

export function deleteDependentItem(token: string, dependentId: string, type: Exclude<HealthItemType, 'vaccinations'>, itemId: string) {
  return api<void>(`/dependents/${dependentId}/${type}/${itemId}`, { method: 'DELETE', token });
}

export function generateDependentQr(token: string, dependentId: string) {
  return api<QrTokenResponse>(`/dependents/${dependentId}/qr-token`, { method: 'POST', token });
}

export function uploadDependentPhoto(token: string, dependentId: string, file: File) {
  const form = new FormData();
  form.append('file', file);
  return api<{ uploaded: boolean }>(`/dependents/${dependentId}/photo`, { method: 'POST', token, body: form });
}

export async function fetchDependentPhoto(token: string, dependentId: string) {
  const response = await fetch(`${API_BASE_URL}/dependents/${dependentId}/photo`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store'
  });
  if (!response.ok) return null;
  return response.blob();
}
