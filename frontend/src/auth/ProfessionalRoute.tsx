import { Navigate, Outlet } from 'react-router-dom';
import { LoadingState } from '../components/ui';
import { useAuth } from './AuthContext';

export function ProfessionalRoute() {
  const { status, hasRole, user } = useAuth();
  if (status === 'loading') return <LoadingState />;
  if (hasRole('HEALTH_ADMIN')) return <Outlet />;
  if (!hasRole('HEALTH_PROFESSIONAL')) return <Navigate to="/unauthorized" replace state={{ message: 'Cet espace est reserve aux professionnels de sante autorises.' }} />;
  const professionalStatus = user?.professionalProfile?.verificationStatus;
  if (professionalStatus === 'PENDING') return <Navigate to="/professional/pending-verification" replace />;
  if (professionalStatus !== 'APPROVED') return <Navigate to="/professional/access-restricted" replace />;
  return <Outlet />;
}
