import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { LoadingState } from '../components/ui';

export function AdminRoute() {
  const { status, hasRole } = useAuth();
  if (status === 'loading') return <LoadingState />;
  if (!hasRole('ADMIN')) return <Navigate to="/non-autorise" replace state={{ message: 'Acces reserve a l administration.' }} />;
  return <Outlet />;
}
