import { useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, Phone, ShieldAlert } from 'lucide-react';
import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getEmergencyInfo } from '../api/publicEmergency.api';
import { Badge, Card, LoadingState } from '../components/ui';

const emergencyQueryKey = ['public-emergency-current'];

export function EmergencyPage() {
  const { token } = useParams();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: emergencyQueryKey,
    queryFn: () => getEmergencyInfo(token!),
    enabled: false,
    retry: false,
    staleTime: 0,
    gcTime: 0
  });
  const { refetch } = query;

  useEffect(() => {
    if (token) void refetch();
  }, [token, refetch]);

  useEffect(() => () => {
    queryClient.removeQueries({ queryKey: emergencyQueryKey });
  }, [queryClient]);

  if (!token || query.isFetching || (!query.data && !query.isError)) return <main className="emergency-page"><LoadingState /></main>;
  if (query.isError || !query.data) return <main className="emergency-page neutral"><AlertTriangle size={36} /><h1>Acces indisponible</h1><p>Ce lien d'urgence est invalide, expire ou revoque.</p></main>;

  const data = query.data;
  return (
    <main className="emergency-page">
      <div className="emergency-banner"><ShieldAlert size={22} /> Informations d'urgence - acces limite.</div>
      <section className="emergency-grid">
        <Card className="vital-card"><Badge tone="danger">Urgence</Badge><h1>Groupe sanguin</h1><strong>{data.bloodType ?? 'Non renseigne'}</strong></Card>
        <Card><h2>Allergies</h2>{data.allergies.length ? <ul>{data.allergies.map((item) => <li key={`${item.label}-${item.severity}`}>{item.label} <Badge tone={item.severity === 'CRITICAL' ? 'danger' : 'warning'}>{item.severity}</Badge></li>)}</ul> : <p>Non renseigne</p>}</Card>
        <Card><h2>Conditions critiques</h2>{data.criticalConditions.length ? <ul>{data.criticalConditions.map((item) => <li key={item}>{item}</li>)}</ul> : <p>Non renseigne</p>}</Card>
        <Card><h2>Medicaments critiques</h2>{data.criticalMedications.length ? <ul>{data.criticalMedications.map((item) => <li key={item}>{item}</li>)}</ul> : <p>Non renseigne</p>}</Card>
        <Card><h2>Notes d'urgence</h2><p>{data.emergencyNotes || 'Non renseigne'}</p></Card>
        <Card><h2>Contact d'urgence</h2>{data.emergencyContact ? <p><Phone size={16} /> {data.emergencyContact.fullName} · {data.emergencyContact.relationship ?? 'Relation non renseignee'} · {data.emergencyContact.phone}</p> : <p>Non renseigne</p>}</Card>
      </section>
    </main>
  );
}
