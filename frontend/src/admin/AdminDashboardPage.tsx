import { useQuery } from '@tanstack/react-query';
import { getAdminDashboard, listAdminAuditLogs } from '../api/admin.api';
import { useAuth } from '../auth/AuthContext';
import { Card, LoadingState } from '../components/ui';

export function AdminDashboardPage() {
  const { token } = useAuth();
  const dashboard = useQuery({ queryKey: ['admin-dashboard'], queryFn: () => getAdminDashboard(token!), enabled: Boolean(token) });
  const logs = useQuery({ queryKey: ['admin-audit-recent'], queryFn: () => listAdminAuditLogs(token!, { page: 0, size: 5 }), enabled: Boolean(token) });
  if (dashboard.isLoading) return <div className="page"><LoadingState /></div>;
  const data = dashboard.data;
  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Administration</p><h1>Console MboloPass</h1><p>Supervision des comptes, cartes QR et actions d’administration.</p></div>
      <div className="admin-stat-grid">
        <Stat title="Utilisateurs totaux" value={data?.totalUsers} />
        <Stat title="Comptes actifs" value={data?.activeUsers} />
        <Stat title="Comptes bloques" value={data?.blockedUsers} />
        <Stat title="QR actifs" value={data?.activeQrCards} />
        <Stat title="QR revoques" value={data?.revokedQrCards} />
        <Stat title="Urgences 24 h" value={data?.emergencyAccessesLast24Hours} />
        <Stat title="Personnes a charge" value={data?.dependentProfiles} />
        <Stat title="Documents stockes" value={data?.medicalDocuments} />
        <Stat title="QR enfants actifs" value={data?.activeDependentQrCards} />
      </div>
      <Card><h2>Activite recente</h2>{logs.data?.content.length ? <ul className="admin-list">{logs.data.content.map((log) => <li key={log.id}><strong>{log.action}</strong><span>{log.actorName}</span><small>{log.createdAt ? new Date(log.createdAt).toLocaleString('fr-FR') : ''}</small></li>)}</ul> : <p className="muted">Aucune activite recente.</p>}</Card>
    </div>
  );
}

function Stat({ title, value }: { title: string; value?: number }) {
  return <Card className="admin-stat"><span>{title}</span><strong>{value ?? 0}</strong></Card>;
}
