import { ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Card } from '../components/ui';

export function ProfessionalRestrictedPage() {
  return (
    <main className="auth-page">
      <Card className="auth-card">
        <ShieldAlert size={38} />
        <h1>Acces professionnel indisponible</h1>
        <p>Votre acces professionnel n'est actuellement pas disponible.</p>
        <p>Contactez l'administrateur de sante concerne.</p>
        <Link to="/connexion">Changer de compte</Link>
      </Card>
    </main>
  );
}
