import { useQuery } from '@tanstack/react-query';
import { ClipboardPlus, FileText, ShieldCheck, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getProfessionalDashboard, getProfessionalMe } from '../api/professional.api';
import { useAuth } from '../auth/AuthContext';
import { AccessStatusBadge, ProfessionalEmptyState, ProfessionalStatCard, ProfessionalStatusBadge } from '../components/professional/ProfessionalUi';
import { Card, LoadingState } from '../components/ui';

export function ProfessionalDashboardPage() {
  const { token, user } = useAuth();
  const me = useQuery({ queryKey: ['professional-me'], queryFn: () => getProfessionalMe(token), enabled: Boolean(token && user?.professionalProfile?.exists) });
  const dashboard = useQuery({ queryKey: ['professional-dashboard'], queryFn: () => getProfessionalDashboard(token), enabled: Boolean(token) });
  const status = me.data?.verificationStatus ?? user?.professionalProfile?.verificationStatus;

  if (dashboard.isLoading || me.isLoading) return <div className="page"><LoadingState /></div>;

  return (
    <div className="page professional-page">
      <div className="page-heading">
        <p className="eyebrow">Espace professionnel</p>
        <h1>Bonjour, {me.data?.professionalType === 'PHYSICIAN' ? 'Dr ' : ''}{user?.lastName ?? 'Professionnel'}</h1>
        <p>Vue d'ensemble de votre activite et de vos acces patients.</p>
      </div>

      <Card className="professional-status-card">
        <div>
          <h2>Profil professionnel</h2>
          <p>{me.data?.speciality ?? me.data?.organizationName ?? 'Informations professionnelles synchronisees avec le backend.'}</p>
        </div>
        <ProfessionalStatusBadge status={status} />
      </Card>

      {dashboard.error ? <Card><p className="error">Impossible de charger le tableau de bord professionnel.</p></Card> : null}

      <div className="professional-stat-grid">
        <ProfessionalStatCard label="Patients autorises" value={dashboard.data?.patientsAuthorized ?? 0} />
        <ProfessionalStatCard label="Demandes en attente" value={dashboard.data?.pendingAccessRequests ?? 0} />
        <ProfessionalStatCard label="Consultations en cours" value={dashboard.data?.activeEncounters ?? 0} hint="Liste deduite des acces actifs" />
        <ProfessionalStatCard label="Examens a completer" value={dashboard.data?.examsToComplete ?? 'API requise'} />
        <ProfessionalStatCard label="Resultats a valider" value={dashboard.data?.resultsToValidate ?? 'API requise'} />
      </div>

      <div className="professional-grid">
        <Card>
          <h2>Actions rapides</h2>
          <div className="professional-actions">
            <Link className="btn" to="/professional/access-requests"><ClipboardPlus size={16} /> Demander un acces</Link>
            <Link className="btn ghost" to="/professional/patients"><Users size={16} /> Ouvrir mes patients</Link>
            <Link className="btn ghost" to="/professional/encounters"><FileText size={16} /> Consultations</Link>
          </div>
        </Card>
        <Card>
          <h2>Activite recente</h2>
          {dashboard.data?.recentAccesses.length ? (
            <ul className="professional-list">
              {dashboard.data.recentAccesses.map((access) => (
                <li key={access.id}>
                  <span>Acces patient {access.patientName || maskId(access.patientId)}</span>
                  <AccessStatusBadge status={access.status} />
                </li>
              ))}
            </ul>
          ) : <ProfessionalEmptyState title="Aucune activite recente" text="Les acces accordes et demandes recentes apparaitront ici." />}
        </Card>
      </div>

      <Card className="professional-note">
        <ShieldCheck size={18} />
        <p>Les donnees medicales detaillees restent dans les fiches patient securisees et ne sont pas affichees sur le tableau de bord general.</p>
      </Card>
    </div>
  );
}

function maskId(value: string) {
  return value.length > 8 ? `${value.slice(0, 4)}...${value.slice(-4)}` : value;
}
