import { api } from './client';
import type { PageResponse } from '../types';

export type AppNotification = {
  id: string;
  type: string;
  title: string;
  message: string;
  referenceType: string | null;
  referenceId: string | null;
  read: boolean;
  readAt: string | null;
  createdAt: string | null;
};

export function listNotifications(token?: string | null, page = 0, size = 10) {
  return api<PageResponse<AppNotification>>(`/notifications?page=${page}&size=${size}`, { token });
}

export function unreadNotificationCount(token?: string | null) {
  return api<{ count: number }>('/notifications/unread-count', { token });
}

export function markNotificationRead(token: string | null | undefined, notificationId: string) {
  return api<AppNotification>(`/notifications/${notificationId}/read`, { method: 'PATCH', token });
}

export function markAllNotificationsRead(token?: string | null) {
  return api<void>('/notifications/read-all', { method: 'PATCH', token });
}
