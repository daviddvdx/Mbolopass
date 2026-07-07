import { api } from './client';
import type { CardInfo, EmergencyQrResponse, QrTokenResponse } from '../types';

export function getMyCard() {
  return api<CardInfo>('/card/me');
}

export function generateMyQrToken() {
  return api<QrTokenResponse>('/card/me/qr-token', { method: 'POST' });
}

export function revokeMyQrToken() {
  return api<void>('/card/me/qr-token/revoke', { method: 'POST' });
}

export function getEmergencyQr(token?: string | null) {
  return api<EmergencyQrResponse>('/cards/me/emergency-qr', { token });
}

export function refreshEmergencyQr(token?: string | null) {
  return api<EmergencyQrResponse>('/cards/me/emergency-qr/refresh', { method: 'POST', token });
}
