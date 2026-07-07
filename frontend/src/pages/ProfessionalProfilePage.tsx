import { useQuery } from '@tanstack/react-query';
import { getProfessionalMe } from '../api/professional.api';
import { useAuth } from '../auth/AuthContext';
import { ProfessionalStatusBadge } from '../components/professional/ProfessionalUi';
import { Card, LoadingState } from '../components/ui';

export function ProfessionalProfilePage() {
  const { token, user } = useAuth();
  const me = useQuery({ queryKey: ['professional-me'], queryFn: () => getProfessionalMe(token), enabled: Boolean(token && user?.professionalProfile?.exists) });
  if (me.isLoading) return <div className="page"><LoadingState /></div>;
  const profile = me.data;
  return (
    <div className="page professional-page">
      <div className="page-heading">
        <p className="eyebrow">Identite professionnelle</p>
        <h1>Mon profil professionnel</h1>
        <p>Ces informations sont en lecture seule et verifiees par l'administration sante.</p>
      </div>
      <Card>
        <div className="professional-profile-grid">
          <span>Nom<strong>{user?.firstName} {user?.lastName}</strong></span>
          <span>Type<strong>{profile?.professionalType ?? user?.professionalProfile?.professionalType ?? '-'}</strong></span>
          <span>Specialite<strong>{profile?.speciality ?? user?.professionalProfile?.speciality ?? '-'}</strong></span>
          <span>Organisation<strong>{profile?.organizationName ?? '-'}</strong></span>
          <span>Statut<strong><ProfessionalStatusBadge status={profile?.verificationStatus ?? user?.professionalProfile?.verificationStatus} /></strong></span>
          <span>Date de verification<strong>Non exposee par l'endpoint actuel</strong></span>
        </div>
      </Card>
    </div>
  );
}
