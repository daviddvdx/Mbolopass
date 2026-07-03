import { ShieldAlert } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { Card } from '../components/ui';

export function UnauthorizedPage() {
  const location = useLocation();
  const message = (location.state as { message?: string } | null)?.message ?? 'Votre role ne permet pas d acceder a cette page.';

  return (
    <main className="auth-page">
      <Card className="auth-card">
        <ShieldAlert size={38} />
        <h1>Acces non autorise</h1>
        <p>{message}</p>
        <Link to="/connexion">Changer de compte</Link>
      </Card>
    </main>
  );
}
