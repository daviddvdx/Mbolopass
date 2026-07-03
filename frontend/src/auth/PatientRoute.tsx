import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { LoadingState } from '../components/ui';

export function PatientRoute() {
  const { status, hasRole } = useAuth();
  if (status === 'loading') return <LoadingState />;
  if (!hasRole('PATIENT')) return <Navigate to="/non-autorise" replace state={{ message: 'Espace reserve aux patients.' }} />;
  return <Outlet />;
}
