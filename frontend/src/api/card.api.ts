import { api } from './client';
import type { CardInfo, QrTokenResponse } from '../types';

export function getMyCard() {
  return api<CardInfo>('/card/me');
}

export function generateMyQrToken() {
  return api<QrTokenResponse>('/card/me/qr-token', { method: 'POST' });
}

export function revokeMyQrToken() {
  return api<void>('/card/me/qr-token/revoke', { method: 'POST' });
}
