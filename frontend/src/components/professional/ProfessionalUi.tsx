import { Activity, Clock, FileText, ShieldAlert, ShieldCheck, Stethoscope } from 'lucide-react';
import { Badge, Card, EmptyState } from '../ui';

type Status = string | null | undefined;

export function ProfessionalStatusBadge({ status }: { status: Status }) {
  if (status === 'APPROVED') return <Badge tone="teal">Verifie</Badge>;
  if (status === 'PENDING') return <Badge tone="warning">En attente</Badge>;
  if (status === 'REJECTED' || status === 'SUSPENDED') return <Badge tone="danger">Restreint</Badge>;
  return <Badge>Non renseigne</Badge>;
}

export function AccessStatusBadge({ status }: { status: Status }) {
  if (status === 'ACTIVE') return <Badge tone="teal">Actif</Badge>;
  if (status === 'PENDING') return <Badge tone="warning">En attente</Badge>;
  if (status === 'REVOKED' || status === 'EXPIRED' || status === 'REJECTED') return <Badge tone="danger">{status}</Badge>;
  return <Badge>{status ?? 'INCONNU'}</Badge>;
}

export function EncounterStatusBadge({ status }: { status: Status }) {
  if (status === 'IN_PROGRESS') return <Badge tone="teal">En cours</Badge>;
  if (status === 'CLOSED') return <Badge>Cloturee</Badge>;
  if (status === 'CANCELLED') return <Badge tone="danger">Annulee</Badge>;
  return <Badge tone="warning">{status ?? 'DRAFT'}</Badge>;
}

export function ExamStatusBadge({ status }: { status: Status }) {
  if (status === 'VALIDATED') return <Badge tone="teal">Valide</Badge>;
  if (status === 'CANCELLED') return <Badge tone="danger">Annule</Badge>;
  if (status === 'RESULT_AVAILABLE') return <Badge tone="warning">A valider</Badge>;
  return <Badge>{status ?? 'Non disponible'}</Badge>;
}

export function ProfessionalStatCard({ label, value, hint }: { label: string; value: number | string; hint?: string }) {
  return (
    <Card className="professional-stat">
      <span>{label}</span>
      <strong>{value}</strong>
      {hint ? <small>{hint}</small> : null}
    </Card>
  );
}

export function PatientAccessBanner({ expiresAt }: { expiresAt?: string | null }) {
  if (!expiresAt) return null;
  const days = Math.ceil((new Date(expiresAt).getTime() - Date.now()) / 86400000);
  if (days > 7) return null;
  return <p className="professional-banner"><Clock size={16} /> Cet acces expire dans {Math.max(days, 0)} jour(s).</p>;
}

export function ProfessionalEmptyState({ title, text }: { title: string; text?: string }) {
  return <EmptyState title={title} text={text ?? 'Aucune donnee disponible depuis les endpoints backend actuels.'} />;
}

export const professionalIcons = { Activity, FileText, ShieldAlert, ShieldCheck, Stethoscope };
