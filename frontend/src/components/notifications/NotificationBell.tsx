import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listNotifications, markAllNotificationsRead, markNotificationRead, unreadNotificationCount } from '../../api/notifications.api';
import { useAuth } from '../../auth/AuthContext';

export function NotificationBell() {
  const { token, user, status } = useAuth();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const enabled = status === 'authenticated' && Boolean(token && user?.id);
  const baseKey = ['notifications', user?.id];
  const count = useQuery({ queryKey: ['notifications-unread-count', user?.id], queryFn: () => unreadNotificationCount(token), enabled, refetchInterval: 45000 });
  const notifications = useQuery({ queryKey: baseKey, queryFn: () => listNotifications(token, 0, 8), enabled: enabled && open });
  const markRead = useMutation({
    mutationFn: (id: string) => markNotificationRead(token, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: baseKey });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count', user?.id] });
    }
  });
  const markAll = useMutation({
    mutationFn: () => markAllNotificationsRead(token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: baseKey });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count', user?.id] });
    }
  });

  function openNotification(id: string, type: string) {
    markRead.mutate(id);
    if (type.startsWith('MEDICAL_ACCESS')) {
      navigate(user?.role === 'PATIENT' ? '/dossier-medical' : '/professional/access-requests');
      setOpen(false);
    }
  }

  return (
    <div className="notification-bell">
      <button type="button" className="icon-button" aria-label="Notifications" onClick={() => setOpen((value) => !value)}>
        <Bell size={18} />
        {(count.data?.count ?? 0) > 0 ? <span className="notification-badge">{count.data?.count}</span> : null}
      </button>
      {open ? (
        <div className="notification-panel">
          <div className="notification-panel-header">
            <strong>Notifications</strong>
            <button type="button" onClick={() => markAll.mutate()} disabled={markAll.isPending}>Tout lire</button>
          </div>
          {notifications.isLoading ? <p>Chargement...</p> : null}
          {notifications.data?.content.length ? (
            <ul>
              {notifications.data.content.map((notification) => (
                <li key={notification.id}>
                  <button type="button" className={notification.read ? undefined : 'unread'} onClick={() => openNotification(notification.id, notification.type)}>
                    <strong>{notification.title}</strong>
                    <span>{notification.message}</span>
                    <small>{notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ''}</small>
                  </button>
                </li>
              ))}
            </ul>
          ) : !notifications.isLoading ? <p>Aucune notification.</p> : null}
        </div>
      ) : null}
    </div>
  );
}
