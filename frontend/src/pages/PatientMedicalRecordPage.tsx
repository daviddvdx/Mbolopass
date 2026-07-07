import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Check, KeyRound, ShieldOff, X } from 'lucide-react';
import { useState } from 'react';
import { ApiError } from '../api/client';
import { approveMedicalAccessRequest, denyMedicalAccessRequest, generateTemporaryAccessCode, getPatientMedicalRecord, listPatientMedicalAccessRequests, revokeMedicalAccess } from '../api/medicalRecord.api';
import { useAuth } from '../auth/AuthContext';
import { AccessStatusBadge, ProfessionalEmptyState } from '../components/professional/ProfessionalUi';
import { Button, Card, LoadingState } from '../components/ui';

export function PatientMedicalRecordPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [temporaryCode, setTemporaryCode] = useState<{ code: string; expiresAt: string } | null>(null);
  const record = useQuery({ queryKey: ['patient-medical-record'], queryFn: () => getPatientMedicalRecord(token), enabled: Boolean(token) });
  const requests = useQuery({ queryKey: ['patient-medical-access-requests'], queryFn: () => listPatientMedicalAccessRequests(token), enabled: Boolean(token) });

  function invalidateMedicalAccess() {
    queryClient.invalidateQueries({ queryKey: ['patient-medical-access-requests'] });
    queryClient.invalidateQueries({ queryKey: ['patient-medical-record'] });
  }

  const approve = useMutation({ mutationFn: (id: string) => approveMedicalAccessRequest(token, id), onSuccess: invalidateMedicalAccess });
  const deny = useMutation({ mutationFn: (id: string) => denyMedicalAccessRequest(token, id), onSuccess: invalidateMedicalAccess });
  const revoke = useMutation({ mutationFn: (id: string) => revokeMedicalAccess(token, id), onSuccess: invalidateMedicalAccess });
  const generate = useMutation({
    mutationFn: (id: string) => generateTemporaryAccessCode(token, id),
    onSuccess: (data) => {
      setTemporaryCode(data);
      invalidateMedicalAccess();
    }
  });

  return (
    <div className="page">
      <div className="page-heading">
        <p className="eyebrow">Dossier medical</p>
        <h1>Mon dossier securise</h1>
        <p>Vous controlez les demandes d'acces et les codes temporaires transmis aux professionnels.</p>
      </div>

      {temporaryCode ? (
        <Card>
          <h2>Code temporaire d'acces</h2>
          <p className="temporary-code">{temporaryCode.code}</p>
          <p>Valable jusqu'au {new Date(temporaryCode.expiresAt).toLocaleString()}. Ce code ne sera plus affiche apres fermeture ou actualisation de la page.</p>
          <p>Il est valable une seule fois. Ne le partagez qu'avec le professionnel concerne.</p>
          <div className="actions">
            <Button type="button" onClick={() => navigator.clipboard?.writeText(temporaryCode.code)}>Copier le code</Button>
            <Button type="button" className="ghost" onClick={() => setTemporaryCode(null)}>Fermer</Button>
          </div>
        </Card>
      ) : null}

      <Card>
        <h2>Resume</h2>
        {record.isLoading ? <LoadingState /> : null}
        {record.error ? <p className="error">{record.error instanceof ApiError ? record.error.message : 'Dossier indisponible.'}</p> : null}
        {record.data ? (
          <div className="professional-stats">
            <div><strong>{record.data.history.length}</strong><span>Antecedents</span></div>
            <div><strong>{record.data.consultations.length}</strong><span>Consultations</span></div>
            <div><strong>{record.data.birthYear ?? '-'}</strong><span>Annee de naissance</span></div>
          </div>
        ) : null}
      </Card>

      <div className="professional-tabs-grid">
        <Card>
          <h2>Antecedents</h2>
          {record.data?.history.length ? <ul>{record.data.history.map((entry) => <li key={entry.id}><strong>{entry.title}</strong><br /><span>{entry.category}</span></li>)}</ul> : <ProfessionalEmptyState title="Aucun antecedent professionnel" />}
        </Card>
        <Card>
          <h2>Consultations</h2>
          {record.data?.consultations.length ? <ul>{record.data.consultations.map((item) => <li key={item.id}><strong>{item.reason}</strong><br /><span>{item.professionalName}</span></li>)}</ul> : <ProfessionalEmptyState title="Aucune consultation" />}
        </Card>
      </div>

      <Card>
        <h2>Demandes d'acces recues</h2>
        {requests.isLoading ? <LoadingState /> : null}
        {requests.data?.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Professionnel</th><th>Statut</th><th>Motif</th><th>Code</th><th>Actions</th></tr></thead>
              <tbody>{requests.data.map((request) => (
                <tr key={request.id}>
                  <td>{request.professionalName}</td>
                  <td><AccessStatusBadge status={request.status} /></td>
                  <td>{request.reason ?? 'Non renseigne'}</td>
                  <td>{request.codeExpiresAt ? `Expire le ${new Date(request.codeExpiresAt).toLocaleString()}` : '-'}</td>
                  <td className="actions-cell">
                    {request.status === 'PENDING' ? (
                      <>
                        <Button type="button" disabled={approve.isPending} onClick={() => approve.mutate(request.id)}><Check size={16} /> Accepter</Button>
                        <Button type="button" className="ghost" disabled={deny.isPending} onClick={() => deny.mutate(request.id)}><X size={16} /> Refuser</Button>
                      </>
                    ) : null}
                    {request.status === 'APPROVED' || request.status === 'LOCKED' ? <><p>Acces approuve. Generez un code temporaire a communiquer directement au professionnel.</p><Button type="button" disabled={generate.isPending} onClick={() => generate.mutate(request.id)}><KeyRound size={16} /> Generer mon code temporaire</Button></> : null}
                    {request.status === 'ACTIVE' ? <Button type="button" className="ghost" disabled={revoke.isPending} onClick={() => revoke.mutate(request.id)}><ShieldOff size={16} /> Revoquer</Button> : null}
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : !requests.isLoading ? <ProfessionalEmptyState title="Aucune demande recue" text="Les demandes de professionnels apparaitront ici." /> : null}
      </Card>
    </div>
  );
}
