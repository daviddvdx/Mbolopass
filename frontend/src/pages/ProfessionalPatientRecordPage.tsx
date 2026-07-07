import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus, Plus } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { createEncounter, createMedicalHistory, getPatientRecord } from '../api/professional.api';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ProfessionalEmptyState } from '../components/professional/ProfessionalUi';
import { Button, Card, Input, LoadingState, Select, Textarea } from '../components/ui';
import { useState } from 'react';

export function ProfessionalPatientRecordPage() {
  const { healthProfileId: patientId = '' } = useParams();
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [historyTitle, setHistoryTitle] = useState('');
  const [historyCategory, setHistoryCategory] = useState('PERSONAL_HISTORY');
  const [historyDescription, setHistoryDescription] = useState('');
  const record = useQuery({ queryKey: ['professional-patient-record', patientId], queryFn: () => getPatientRecord(token, patientId), enabled: Boolean(token && patientId) });
  const createConsultation = useMutation({
    mutationFn: () => createEncounter(token, patientId, { reason: reason.trim(), clinicalNotes: null }),
    onSuccess: () => {
      setReason('');
      queryClient.invalidateQueries({ queryKey: ['professional-patient-record', patientId] });
      queryClient.invalidateQueries({ queryKey: ['professional-encounters'] });
    }
  });
  const createHistory = useMutation({
    mutationFn: () => createMedicalHistory(token, patientId, { category: historyCategory, title: historyTitle.trim(), description: historyDescription.trim() || null, active: true }),
    onSuccess: () => {
      setHistoryTitle('');
      setHistoryDescription('');
      queryClient.invalidateQueries({ queryKey: ['professional-patient-record', patientId] });
    }
  });

  if (record.isLoading) return <div className="page"><LoadingState /></div>;

  return (
    <div className="page professional-page">
      {record.error ? <Card><p className="error">{record.error instanceof ApiError && record.error.status === 403 ? 'Acces au dossier refuse, expire ou revoque.' : 'Fiche patient indisponible.'}</p></Card> : null}
      {record.data ? (
        <>
          <div className="page-heading">
            <p className="eyebrow">Fiche sante patient</p>
            <h1>{record.data.firstName} {record.data.lastName}</h1>
            <p>Identifiant patient {mask(record.data.patientId)} - {record.data.gender ?? 'Sexe non renseigne'} - {record.data.birthYear ?? 'Annee non renseignee'}</p>
          </div>
          <div className="professional-tabs-grid">
            <Card>
              <h2>Creer une consultation</h2>
              <Textarea rows={3} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Motif de consultation" />
              {createConsultation.error ? <p className="error">{createConsultation.error instanceof ApiError ? createConsultation.error.message : 'Creation impossible.'}</p> : null}
              <Button type="button" disabled={createConsultation.isPending || reason.trim().length < 2} onClick={() => createConsultation.mutate()}><FilePlus size={16} /> Creer une consultation</Button>
            </Card>
            <Card>
              <h2>Ajouter un antecedent</h2>
              <Select value={historyCategory} onChange={(event) => setHistoryCategory(event.target.value)}>
                <option value="PERSONAL_HISTORY">Antecedent personnel</option>
                <option value="FAMILY_HISTORY">Antecedent familial</option>
                <option value="ALLERGY">Allergie</option>
                <option value="CHRONIC_CONDITION">Maladie chronique</option>
                <option value="SURGERY">Chirurgie</option>
                <option value="MEDICATION">Traitement</option>
                <option value="OTHER">Autre</option>
              </Select>
              <Input value={historyTitle} onChange={(event) => setHistoryTitle(event.target.value)} placeholder="Titre" />
              <Textarea rows={3} value={historyDescription} onChange={(event) => setHistoryDescription(event.target.value)} placeholder="Observation facultative" />
              {createHistory.error ? <p className="error">{createHistory.error instanceof ApiError ? createHistory.error.message : 'Ajout impossible.'}</p> : null}
              <Button type="button" disabled={createHistory.isPending || historyTitle.trim().length < 2} onClick={() => createHistory.mutate()}><Plus size={16} /> Ajouter</Button>
            </Card>
          </div>
          <div className="professional-tabs-grid">
            <Card><h2>Antecedents</h2>{record.data.history.length ? <ul>{record.data.history.map((item) => <li key={item.id}><strong>{item.title}</strong> <span className="source-badge">Valide par un professionnel</span><br /><span>{item.category}</span>{item.description ? <p>{item.description}</p> : null}</li>)}</ul> : <ProfessionalEmptyState title="Aucun antecedent" />}</Card>
            <Card><h2>Consultations</h2>{record.data.consultations.length ? <ul className="professional-list">{record.data.consultations.map((item) => <li key={item.id}><span>{item.reason ?? 'Sans motif'}</span><span>{item.professionalName}</span></li>)}</ul> : <ProfessionalEmptyState title="Aucune consultation" />}</Card>
            <Card><h2>Examens</h2><ProfessionalEmptyState title="Examens non disponibles" text="Les examens restent geres par le module clinique existant." /></Card>
            <Card><h2>Historique d'acces</h2><ProfessionalEmptyState title="Audit non expose ici" text="Les actions sensibles sont journalisees cote backend." /></Card>
          </div>
        </>
      ) : null}
    </div>
  );
}

function mask(value: string) {
  return value.length > 12 ? `${value.slice(0, 4)}...${value.slice(-4)}` : value;
}
