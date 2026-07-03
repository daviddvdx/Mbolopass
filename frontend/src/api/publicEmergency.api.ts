import { api } from './client';
import type { EmergencyInfo } from '../types';

export function getEmergencyInfo(token: string) {
  return api<EmergencyInfo>(`/public/emergency/${encodeURIComponent(token)}`);
}