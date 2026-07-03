import { API_BASE_URL, api } from './client';
import type { HealthItemType, Item, ItemRequest, Profile, ProfileRequest, UserProfile, UserProfileRequest } from '../types';

export function getUserProfile(token: string) {
  return api<UserProfile>('/profile/me', { token });
}

export function updateUserProfile(token: string, payload: UserProfileRequest) {
  return api<UserProfile>('/profile/me', { method: 'PUT', token, body: JSON.stringify(payload) });
}

export function getProfile(token: string) {
  return api<Profile>('/health-profile/me', { token });
}

export function updateProfile(token: string, payload: ProfileRequest) {
  return api<Profile>('/health-profile/me', { method: 'PUT', token, body: JSON.stringify(payload) });
}

export function listItems(token: string, type: HealthItemType) {
  return api<Item[]>(`/health-profile/me/${type}`, { token });
}

export function addItem(token: string, type: HealthItemType, payload: ItemRequest) {
  return api<Item>(`/health-profile/me/${type}`, { method: 'POST', token, body: JSON.stringify(payload) });
}

export function deleteItem(token: string, type: HealthItemType, id: string) {
  return api<void>(`/health-profile/me/${type}/${id}`, { method: 'DELETE', token });
}

export function uploadProfilePhoto(token: string, file: File) {
  const form = new FormData();
  form.append('file', file);
  return api<{ uploaded: boolean }>('/profile/me/photo', { method: 'POST', token, body: form });
}

export function getProfilePhoto(token: string) {
  return api<Blob>('/profile/me/photo', { token, headers: { Accept: 'image/*' } }).then((value) => value);
}

export async function fetchProfilePhoto(token: string) {
  const response = await fetch(`${API_BASE_URL}/profile/me/photo`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store'
  });
  if (!response.ok) return null;
  return response.blob();
}

export function deleteProfilePhoto(token: string) {
  return api<void>('/profile/me/photo', { method: 'DELETE', token });
}
