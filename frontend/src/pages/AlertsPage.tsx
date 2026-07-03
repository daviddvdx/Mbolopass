import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { dismissAlert, listAlerts, refreshAlerts } from '../api/extras.api';
import { useAuth } from '../auth/AuthContext';
import { Badge, Button, Card, EmptyState, LoadingState } from '../components/ui';

export function AlertsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const alerts = useQuery({ queryKey: ['alerts'], queryFn: () => listAlerts(token!), enabled: Boolean(token) });
  const refresh = useMutation({ mutationFn: () => refreshAlerts(token!), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['alerts'] }) });
  const dismiss = useMutation({ mutationFn: (id: string) => dismissAlert(token!, id), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['alerts'] }) });

  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Prevention</p><h1>Alertes</h1><p>Module connecte au backend, a enrichir apres le MVP.</p></div>
      <Button type="button" onClick={() => refresh.mutate()} disabled={refresh.isPending}><Bell size={18} /> Actualiser les alertes</Button>
      {alerts.isLoading ? <LoadingState /> : null}
      {alerts.data?.length ? alerts.data.map((alert) => <Card key={alert.id}><Badge tone={alert.severity === 'CRITICAL' ? 'danger' : alert.severity === 'WARNING' ? 'warning' : 'blue'}>{alert.severity}</Badge><h2>{alert.title}</h2><p>{alert.message}</p><Button type="button" className="ghost" onClick={() => dismiss.mutate(alert.id)}>Masquer</Button></Card>) : <EmptyState title="Aucune alerte chargee" text="Cliquez sur actualiser pour interroger le backend." />}
    </div>
  );
}