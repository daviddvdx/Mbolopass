import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { listAdminAuditLogs } from '../api/admin.api';
import { useAuth } from '../auth/AuthContext';
import { Card, Input, LoadingState } from '../components/ui';

export function AdminAuditLogPage() {
  const { token } = useAuth();
  const [action, setAction] = useState('');
  const logs = useQuery({ queryKey: ['admin-audit', action], queryFn: () => listAdminAuditLogs(token!, { page: 0, size: 50, action }), enabled: Boolean(token) });
  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Administration</p><h1>Journal</h1><p>Actions sensibles sans IP brute ni donnees cliniques.</p></div>
      <Card><Input value={action} onChange={(event) => setAction(event.target.value)} placeholder="Filtrer par action: USER_BLOCKED, QR_REVOKED..." /></Card>
      <Card>{logs.isLoading ? <LoadingState /> : <div className="table-wrap"><table><thead><tr><th>Date</th><th>Acteur</th><th>Action</th><th>Cible</th><th>Details</th></tr></thead><tbody>{logs.data?.content.map((log) => <tr key={log.id}><td>{log.createdAt ? new Date(log.createdAt).toLocaleString('fr-FR') : '-'}</td><td>{log.actorName}</td><td>{log.action}</td><td>{log.targetType} {log.targetId}</td><td>{log.details ?? '-'}</td></tr>)}</tbody></table></div>}</Card>
    </div>
  );
}