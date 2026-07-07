import { useQuery } from '@tanstack/react-query';
import { getAdminDashboard, listAdminAuditLogs } from '../api/admin.api';
import { useAuth } from '../auth/AuthContext';
import { Card, LoadingState } from '../components/ui';

export function AdminDashboardPage() {
  const { token, user, status } = useAuth();
  const enabled = status === 'authenticated' && Boolean(token && user?.id);
  const dashboard = useQuery({ queryKey: ['admin-dashboard', user?.id], queryFn: () => getAdminDashboard(token!), enabled });
  const logs = useQuery({ queryKey: ['admin-audit-recent', user?.id], queryFn: () => listAdminAuditLogs(token!, { page: 0, size: 5 }), enabled });
  if (dashboard.isLoading) return <div className="page"><LoadingState /></div>;
  if (dashboard.isError) {
    return (
      <div className="page">
        <div className="page-heading"><p className="eyebrow">Administration</p><h1>Console MboloPass</h1></div>
        <Card>
          <h2>Donnees indisponibles</h2>
          <p className="error">Le tableau de bord admin n'a pas pu recuperer les statistiques. Verifiez que votre compte possede le role HEALTH_ADMIN et que votre session est encore valide.</p>
        </Card>
      </div>
    );
  }
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
