import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { LoadingState } from '../components/ui';

export function ProtectedRoute() {
  const { isAuthenticated, status } = useAuth();
  const location = useLocation();
  if (status === 'loading') return <LoadingState />;
  if (!isAuthenticated) return <Navigate to="/connexion" replace state={{ from: location }} />;
  return <Outlet />;
}
