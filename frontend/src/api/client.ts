import type { ApiErrorBody } from '../types';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';
export const UNAUTHORIZED_EVENT = 'mbolopass:unauthorized';
export const FORBIDDEN_EVENT = 'mbolopass:forbidden';

let memoryAuthToken: string | null = null;

export function setApiAuthToken(token: string | null) {
  memoryAuthToken = token;
}

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

type ApiOptions = RequestInit & {
  token?: string | null;
};

export async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const token = options.token ?? memoryAuthToken;
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    cache: 'no-store'
  });

  if (!response.ok) {
    if (response.status === 401 && token) window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    if (response.status === 403 && token) window.dispatchEvent(new Event(FORBIDDEN_EVENT));
    const body = await response.json().catch(() => ({} as ApiErrorBody));
    throw new ApiError(body.message ?? 'Requete refusee', response.status);
  }

  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
