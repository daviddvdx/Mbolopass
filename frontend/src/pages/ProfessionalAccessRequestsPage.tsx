import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { KeyRound, Send } from 'lucide-react';
import { useState } from 'react';
import { activateAccessRequest, createAccessRequest, listAccessRequests, searchPatients } from '../api/professional.api';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AccessStatusBadge, ProfessionalEmptyState } from '../components/professional/ProfessionalUi';
import { Button, Card, Input, LoadingState, Textarea } from '../components/ui';

export function ProfessionalAccessRequestsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState('');
  const [selectedPatientId, setSelectedPatientId] = useState('');
  const [reason, setReason] = useState('');
  const [codes, setCodes] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);

  const patients = useQuery({ queryKey: ['medical-patient-search', query], queryFn: () => searchPatients(token, query, 0, 10), enabled: Boolean(token && query.trim().length >= 2) });
  const requests = useQuery({ queryKey: ['professional-access-requests'], queryFn: () => listAccessRequests(token), enabled: Boolean(token) });
  const create = useMutation({
    mutationFn: () => createAccessRequest(token, { patientId: selectedPatientId, reason: reason.trim() || null }),
    onSuccess: () => {
      setReason('');
      setSelectedPatientId('');
      setSuccess('La demande a ete transmise au patient pour validation.');
      queryClient.invalidateQueries({ queryKey: ['professional-access-requests'] });
      queryClient.invalidateQueries({ queryKey: ['professional-dashboard'] });
    }
  });
  const activate = useMutation({
    mutationFn: (requestId: string) => activateAccessRequest(token, requestId, codes[requestId] ?? ''),
    onSuccess: () => {
      setCodes({});
      queryClient.invalidateQueries({ queryKey: ['professional-access-requests'] });
      queryClient.invalidateQueries({ queryKey: ['professional-patients'] });
      queryClient.invalidateQueries({ queryKey: ['professional-dashboard'] });
    }
  });

  return (
    <div className="page professional-page">
      <div className="page-heading">
        <p className="eyebrow">Dossiers patients</p>
        <h1>Demandes d'acces</h1>
        <p>Le dossier complet reste inaccessible tant que le patient n'a pas valide puis transmis un code temporaire.</p>
      </div>
      <Card>
        <h2>Rechercher un patient inscrit</h2>
        <Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Nom, prenom ou email du patient" />
        {patients.isLoading ? <LoadingState /> : null}
        {patients.data?.content.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Patient</th><th>Sexe</th><th>Annee</th><th>Action</th></tr></thead>
              <tbody>{patients.data.content.map((patient) => (
                <tr key={patient.id}>
                  <td>{patient.firstName} {patient.lastName}</td>
                  <td>{patient.gender ?? '-'}</td>
                  <td>{patient.birthYear ?? '-'}</td>
                  <td><Button type="button" className="ghost" onClick={() => setSelectedPatientId(patient.id)}>Selectionner</Button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : query.trim().length >= 2 && !patients.isLoading ? <ProfessionalEmptyState title="Aucun patient trouve" /> : null}
      </Card>
      <Card>
        <h2>Nouvelle demande</h2>
        <div className="professional-form-grid">
          <label>Identifiant patient<Input value={selectedPatientId} onChange={(event) => setSelectedPatientId(event.target.value)} placeholder="Selectionnez un patient dans la recherche" /></label>
          <label className="wide">Motif de la demande<Textarea rows={3} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Motif clinique ou contexte de prise en charge" /></label>
        </div>
        {success ? <p className="safe">{success}</p> : null}
        {create.error ? <p className="error">{create.error instanceof ApiError ? create.error.message : 'Demande refusee.'}</p> : null}
        <Button type="button" disabled={!selectedPatientId.trim() || create.isPending} onClick={() => { setSuccess(null); create.mutate(); }}><Send size={16} /> Envoyer la demande</Button>
      </Card>

      <Card>
        <h2>Mes demandes</h2>
        {requests.isLoading ? <LoadingState /> : null}
        {requests.data?.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Patient</th><th>Statut</th><th>Motif</th><th>Expiration code/acces</th><th>Activation</th></tr></thead>
              <tbody>{requests.data.map((request) => (
                <tr key={request.id}>
                  <td>{request.patientName}</td>
                  <td><AccessStatusBadge status={request.status} /></td>
                  <td>{request.reason ?? 'Non renseigne'}</td>
                  <td>{formatDate(request.activeGrant?.expiresAt ?? request.codeExpiresAt)}</td>
                  <td>
                    {request.status === 'APPROVED' ? (
                      <>
                        <p>Le patient a accepte. Demandez-lui le code temporaire genere dans son espace MboloPass.</p>
                        <div className="inline-form">
                          <Input value={codes[request.id] ?? ''} onChange={(event) => setCodes((current) => ({ ...current, [request.id]: event.target.value.toUpperCase() }))} placeholder="Code temporaire" />
                          <Button type="button" disabled={activate.isPending || !(codes[request.id] ?? '').trim()} onClick={() => activate.mutate(request.id)}><KeyRound size={16} /> Activer</Button>
                        </div>
                      </>
                    ) : request.status === 'ACTIVE' ? 'Acces actif' : '-'}
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : !requests.isLoading ? <ProfessionalEmptyState title="Aucune demande" text="Recherchez un patient puis transmettez une demande d'acces." /> : null}
      </Card>
    </div>
  );
}

function formatDate(value: string | null | undefined) {
  return value ? new Date(value).toLocaleDateString() : '-';
}
