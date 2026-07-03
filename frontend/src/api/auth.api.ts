import { api } from './client';
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../types';

export function registerPatient(payload: RegisterRequest) {
  return api<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(payload) });
}

export function login(payload: LoginRequest) {
  return api<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify(payload) });
}

export function me(token: string) {
  return api<User>('/auth/me', { token });
}