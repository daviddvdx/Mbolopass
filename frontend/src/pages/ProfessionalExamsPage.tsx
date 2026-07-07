import { useQuery } from '@tanstack/react-query';
import { listExamCatalog, listExamOrders } from '../api/professional.api';
import { useAuth } from '../auth/AuthContext';
import { ExamStatusBadge, ProfessionalEmptyState } from '../components/professional/ProfessionalUi';
import { Card, LoadingState } from '../components/ui';

export function ProfessionalExamsPage() {
  const { token } = useAuth();
  const exams = useQuery({ queryKey: ['professional-exams'], queryFn: listExamOrders });
  const catalog = useQuery({ queryKey: ['professional-exam-catalog'], queryFn: () => listExamCatalog(token), enabled: Boolean(token) });
  return (
    <div className="page professional-page">
      <div className="page-heading">
        <p className="eyebrow">Examens</p>
        <h1>Examens et resultats</h1>
        <p>Les demandes d'examen sont creees depuis une consultation ouverte.</p>
      </div>
      <div className="professional-grid">
        {['Examens demandes', 'Resultats en attente', 'Resultats disponibles', 'Resultats a valider'].map((title) => (
          <Card key={title}>
            <h2>{title}</h2>
            {exams.isLoading ? <LoadingState /> : null}
            {exams.data?.items.length ? exams.data.items.map((item) => <p key={item.id}>{item.examName} <ExamStatusBadge status={item.status} /></p>) : <ProfessionalEmptyState title="Aucune donnee" text="Endpoint de liste des examens non disponible actuellement." />}
          </Card>
        ))}
      </div>
      <Card>
        <h2>Catalogue examens</h2>
        {catalog.isLoading ? <LoadingState /> : null}
        {catalog.data?.length ? <ul className="professional-list">{catalog.data.map((item) => <li key={item.id}><span>{item.name}</span><ExamStatusBadge status={item.active ? 'ACTIVE' : 'CANCELLED'} /></li>)}</ul> : <ProfessionalEmptyState title="Aucun examen catalogue" text="Le catalogue d'examens est vide ou non disponible." />}
      </Card>
    </div>
  );
}
