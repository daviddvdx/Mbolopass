import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileText, LockKeyhole, Plus, RefreshCw, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { createEncounter, getProfessionalMe, listProfessionalAccessRequests, openPatientRecord, requestPatientAccess, closeEncounter } from '../api/clinical.api';
import { Badge, Button, Card, EmptyState, Input, LoadingState, Textarea } from '../components/ui';

export function ProfessionalPage() {
  const queryClient = useQueryClient();
  const [healthProfileId, setHealthProfileId] = useState('');
  const [reason, setReason] = useState('');
  const [selectedProfileId, setSelectedProfileId] = useState<string | null>(null);
  const [encounterReason, setEncounterReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const me = useQuery({ queryKey: ['professional-me'], queryFn: getProfessionalMe });
  const accesses = useQuery({ queryKey: ['professional-accesses'], queryFn: listProfessionalAccessRequests });
  const record = useQuery({
    queryKey: ['professional-record', selectedProfileId],
    enabled: Boolean(selectedProfileId),
    queryFn: () => openPatientRecord(selectedProfileId!)
  });

  const requestAccess = useMutation({
    mutationFn: () => requestPatientAccess({ healthProfileId: healthProfileId.trim(), reason: reason.trim() || null }),
    onSuccess: () => {
      setHealthProfileId('');
      setReason('');
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['professional-accesses'] });
    },
    onError: (caught) => setError(errorMessage(caught))
  });

  const create = useMutation({
    mutationFn: () => createEncounter(selectedProfileId!, { encounterType: 'CONSULTATION', reason: encounterReason || null }),
    onSuccess: () => {
      setEncounterReason('');
      queryClient.invalidateQueries({ queryKey: ['professional-record', selectedProfileId] });
    },
    onError: (caught) => setError(errorMessage(caught))
  });

  const close = useMutation({
    mutationFn: (encounterId: string) => closeEncounter(encounterId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['professional-record', selectedProfileId] }),
    onError: (caught) => setError(errorMessage(caught))
  });

  if (me.isLoading) return <div className="page"><LoadingState /></div>;

  const approved = me.data?.verificationStatus === 'APPROVED';

  return (
    <div className="page">
      <div className="page-heading">
        <p className="eyebrow">Espace professionnel</p>
        <h1>Dossiers patients autorises</h1>
        <p>Les dossiers ne s'ouvrent qu'apres verification professionnelle et accord patient actif.</p>
      </div>

      <Card>
        <div className="section-title">
          <h2>Profil professionnel</h2>
          <Badge tone={approved ? 'teal' : 'warning'}>{me.data?.verificationStatus ?? 'INCONNU'}</Badge>
        </div>
        <p>{me.data?.professionalType} {me.data?.speciality ? `- ${me.data.speciality}` : ''}</p>
        {!approved ? <p className="error"><LockKeyhole size={16} /> Votre compte doit etre approuve avant tout acces patient.</p> : null}
      </Card>

      <Card>
        <h2>Demander un acces patient</h2>
        <div className="inline-form">
          <Input value={healthProfileId} onChange={(event) => setHealthProfileId(event.target.value)} placeholder="ID du profil patient" disabled={!approved || requestAccess.isPending} />
          <Input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Motif clinique" disabled={!approved || requestAccess.isPending} />
          <Button type="button" onClick={() => requestAccess.mutate()} disabled={!approved || !healthProfileId.trim() || requestAccess.isPending}><Plus size={16} /> Demander</Button>
        </div>
        {error ? <p className="error" role="alert">{error}</p> : null}
      </Card>

      <div className="profile-sections">
        <Card>
          <div className="section-title"><h2>Mes acces</h2><Button className="ghost small" type="button" onClick={() => accesses.refetch()}><RefreshCw size={16} /></Button></div>
          {accesses.data?.length ? <ul className="item-list">{accesses.data.map((access) => (
            <li key={access.id}>
              <span><strong>{access.status}</strong> - {access.healthProfileId}</span>
              <Button type="button" className="ghost small" disabled={access.status !== 'ACTIVE'} onClick={() => setSelectedProfileId(access.healthProfileId)}>Ouvrir</Button>
            </li>
          ))}</ul> : <EmptyState title="Aucun acces" text="Les demandes acceptees par les patients apparaitront ici." />}
        </Card>

        <Card>
          <div className="section-title"><h2>Fiche patient</h2><Badge tone={record.data ? 'teal' : 'blue'}>{record.data ? 'OUVERTE' : 'SECURISEE'}</Badge></div>
          {record.isLoading ? <LoadingState /> : null}
          {record.data ? (
            <div>
              <p><strong>{record.data.firstName} {record.data.lastName}</strong> - {record.data.cardNumber}</p>
              <p>Groupe sanguin: {record.data.bloodType ?? 'Non renseigne'}</p>
              <h3>Allergies</h3>
              {record.data.allergies.length ? <ul>{record.data.allergies.map((item) => <li key={item}>{item}</li>)}</ul> : <p>Aucune allergie affichee.</p>}
              <h3>Maladies et antecedents</h3>
              {record.data.conditions.length ? <ul>{record.data.conditions.map((item) => <li key={item}>{item}</li>)}</ul> : <p>Aucune condition affichee.</p>}
              <div className="inline-form">
                <Textarea value={encounterReason} onChange={(event) => setEncounterReason(event.target.value)} placeholder="Motif de consultation" rows={2} />
                <Button type="button" onClick={() => create.mutate()} disabled={create.isPending}><FileText size={16} /> Nouvelle consultation</Button>
              </div>
              {record.data.encounters.length ? <ul className="item-list">{record.data.encounters.map((encounter) => (
                <li key={encounter.id}>
                  <span>{encounter.status} - {encounter.reason ?? 'Sans motif'}</span>
                  <Button className="ghost small" type="button" disabled={encounter.status === 'CLOSED' || close.isPending} onClick={() => close.mutate(encounter.id)}><ShieldCheck size={16} /> Cloturer</Button>
                </li>
              ))}</ul> : null}
            </div>
          ) : <EmptyState title="Aucune fiche ouverte" text="Selectionnez un acces actif pour ouvrir une fiche patient." />}
        </Card>
      </div>
    </div>
  );
}

function errorMessage(caught: unknown) {
  return caught instanceof Error && caught.message ? caught.message : 'Action refusee.';
}
