import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, ClipboardPlus, FilePlus, FlaskConical, Plus } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { addAllergyToEncounter, addDiagnosisToEncounter, closeEncounter, createExamOrder, getEncounter, listExamCatalog, listMedicalCatalog } from '../api/professional.api';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { EncounterStatusBadge, ProfessionalEmptyState } from '../components/professional/ProfessionalUi';
import { Button, Card, Input, LoadingState, Select, Textarea } from '../components/ui';
import { useState } from 'react';

export function ProfessionalEncounterDetailsPage() {
  const { encounterId = '' } = useParams();
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [referenceCatalogId, setReferenceCatalogId] = useState('');
  const [clinicalStatus, setClinicalStatus] = useState('ACTIVE');
  const [diagnosisNotes, setDiagnosisNotes] = useState('');
  const [allergyLabel, setAllergyLabel] = useState('');
  const [allergyLevel, setAllergyLevel] = useState('LOW');
  const [examCatalogId, setExamCatalogId] = useState('');
  const [examReason, setExamReason] = useState('');

  const encounter = useQuery({ queryKey: ['professional-encounter', encounterId], queryFn: () => getEncounter(token, encounterId), enabled: Boolean(token && encounterId) });
  const catalog = useQuery({ queryKey: ['professional-medical-catalog'], queryFn: () => listMedicalCatalog(token), enabled: Boolean(token) });
  const examCatalog = useQuery({ queryKey: ['professional-exam-catalog'], queryFn: () => listExamCatalog(token), enabled: Boolean(token) });
  const isOpen = encounter.data?.status !== 'CLOSED';

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['professional-encounter', encounterId] });
    queryClient.invalidateQueries({ queryKey: ['professional-encounters'] });
  };
  const diagnosis = useMutation({ mutationFn: () => addDiagnosisToEncounter(token, encounterId, { referenceCatalogId, clinicalStatus, notes: diagnosisNotes || null }), onSuccess: invalidate });
  const allergy = useMutation({ mutationFn: () => addAllergyToEncounter(token, encounterId, { label: allergyLabel, level: allergyLevel, critical: allergyLevel === 'CRITICAL' }), onSuccess: invalidate });
  const exam = useMutation({ mutationFn: () => createExamOrder(token, encounterId, { examCatalogId, priority: 'ROUTINE', clinicalReason: examReason || null }), onSuccess: invalidate });
  const close = useMutation({ mutationFn: () => closeEncounter(token, encounterId), onSuccess: invalidate });

  if (encounter.isLoading) return <div className="page"><LoadingState /></div>;

  return (
    <div className="page professional-page">
      <div className="page-heading">
        <p className="eyebrow">Consultation</p>
        <h1>{encounter.data?.reason ?? 'Consultation clinique'}</h1>
        <p>{encounter.data ? <EncounterStatusBadge status={encounter.data.status} /> : null}</p>
      </div>
      {encounter.error ? <Card><p className="error">Consultation indisponible ou acces refuse.</p></Card> : null}
      {encounter.data ? (
        <>
          <Card>
            <h2>Motif et suivi</h2>
            <p>{encounter.data.reason ?? 'Aucun motif renseigne.'}</p>
            <p>Les modifications directes sont limitees par le backend. Les ajouts medicaux passent par cette consultation.</p>
            {isOpen ? <Button type="button" disabled={close.isPending} onClick={() => close.mutate()}><CheckCircle2 size={16} /> Cloturer la consultation</Button> : <p className="muted">Consultation cloturee en lecture seule.</p>}
          </Card>

          <div className="professional-grid">
            <Card>
              <h2>Ajouter une pathologie</h2>
              {isOpen ? (
                <>
                  <Select value={referenceCatalogId} onChange={(event) => setReferenceCatalogId(event.target.value)}><option value="">Selectionner depuis le catalogue</option>{catalog.data?.filter((item) => item.category === 'CONDITION' || item.category === 'DISEASE' || item.category === 'PATHOLOGY').map((item) => <option key={item.id} value={item.id}>{item.displayName}</option>)}</Select>
                  <Select value={clinicalStatus} onChange={(event) => setClinicalStatus(event.target.value)}><option value="ACTIVE">ACTIVE</option><option value="HISTORICAL">HISTORICAL</option><option value="RESOLVED">RESOLVED</option><option value="SUSPECTED">SUSPECTED</option></Select>
                  <Textarea rows={3} value={diagnosisNotes} onChange={(event) => setDiagnosisNotes(event.target.value)} placeholder="Observation clinique facultative" />
                  {diagnosis.error ? <p className="error">{diagnosis.error instanceof ApiError ? diagnosis.error.message : 'Ajout refuse.'}</p> : null}
                  <Button type="button" disabled={!referenceCatalogId || diagnosis.isPending} onClick={() => diagnosis.mutate()}><ClipboardPlus size={16} /> Ajouter</Button>
                </>
              ) : <ProfessionalEmptyState title="Lecture seule" text="Une consultation cloturee ne peut pas etre modifiee depuis cette interface." />}
            </Card>
            <Card>
              <h2>Valider une allergie</h2>
              {isOpen ? (
                <>
                  <Input value={allergyLabel} onChange={(event) => setAllergyLabel(event.target.value)} placeholder="Ex. Penicilline" />
                  <Select value={allergyLevel} onChange={(event) => setAllergyLevel(event.target.value)}><option value="LOW">LOW</option><option value="MEDIUM">MEDIUM</option><option value="HIGH">HIGH</option><option value="CRITICAL">CRITICAL</option></Select>
                  {allergy.error ? <p className="error">{allergy.error instanceof ApiError ? allergy.error.message : 'Ajout refuse.'}</p> : null}
                  <Button type="button" disabled={!allergyLabel.trim() || allergy.isPending} onClick={() => allergy.mutate()}><Plus size={16} /> Ajouter</Button>
                </>
              ) : <ProfessionalEmptyState title="Lecture seule" />}
            </Card>
            <Card>
              <h2>Prescrire un examen</h2>
              <Select value={examCatalogId} onChange={(event) => setExamCatalogId(event.target.value)} disabled={!isOpen}><option value="">Selectionner un examen</option>{examCatalog.data?.filter((item) => item.active).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select>
              <Textarea rows={2} value={examReason} onChange={(event) => setExamReason(event.target.value)} placeholder="Raison clinique" disabled={!isOpen} />
              {exam.error ? <p className="error">{exam.error instanceof ApiError ? exam.error.message : 'Demande refusee.'}</p> : null}
              <Button type="button" disabled={!isOpen || !examCatalogId || exam.isPending} onClick={() => exam.mutate()}><FlaskConical size={16} /> Demander</Button>
            </Card>
            <Card>
              <h2>Historique des actions</h2>
              <ProfessionalEmptyState title="Audit non affiche" text="Les evenements d'audit restent disponibles cote backend/admin, sans details sensibles ici." />
            </Card>
          </div>
        </>
      ) : null}
    </div>
  );
}
