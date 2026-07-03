import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Sparkles } from 'lucide-react';
import { latestSummary, regenerateSummary } from '../api/extras.api';
import { useAuth } from '../auth/AuthContext';
import { Button, Card, EmptyState, LoadingState } from '../components/ui';

export function SummaryPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const latest = useQuery({ queryKey: ['summary'], queryFn: () => latestSummary(token!), enabled: Boolean(token), retry: false });
  const generate = useMutation({ mutationFn: () => regenerateSummary(token!), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['summary'] }) });

  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Assistance</p><h1>Resume sante</h1><p>Cette synthese est une aide a la lecture des donnees existantes.</p></div>
      <Button type="button" onClick={() => generate.mutate()} disabled={generate.isPending}><Sparkles size={18} /> Generer / actualiser</Button>
      {latest.isLoading ? <LoadingState /> : null}
      {latest.data ? <Card><pre>{latest.data.content}</pre><p className="safe">{latest.data.disclaimer}</p></Card> : <EmptyState title="Bientot disponible" text="Aucun resume disponible pour cette session." />}
    </div>
  );
}