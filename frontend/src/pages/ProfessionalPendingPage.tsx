import { Clock } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Card } from '../components/ui';

export function ProfessionalPendingPage() {
  return (
    <main className="auth-page">
      <Card className="auth-card">
        <Clock size={38} />
        <h1>Verification en cours</h1>
        <p>Votre demande de compte professionnel est en cours de verification.</p>
        <p>Vous pourrez acceder aux dossiers patients apres validation.</p>
        <Link to="/connexion">Changer de compte</Link>
      </Card>
    </main>
  );
}
