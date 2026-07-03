import { Bell, CreditCard, HeartPulse, Sparkles } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Badge, Card } from '../components/ui';

export function DashboardPage() {
  const { user } = useAuth();
  const location = useLocation();
  const message = (location.state as { message?: string } | null)?.message;
  return (
    <div className="page">
      <div className="page-heading">
        <p className="eyebrow">Tableau de bord</p>
        <h1>Bonjour {user?.firstName ?? 'utilisateur'}</h1>
        <p>Retrouvez votre profil, votre carte d'urgence et les modules de prevention.</p>
        {message ? <p className="error">{message}</p> : null}
      </div>
      <div className="feature-grid">
        <Link to="/mon-profil" className="feature-card"><HeartPulse /><h2>Profil sante</h2><p>Renseigner les champs geres par le backend.</p></Link>
        <Link to="/ma-carte" className="feature-card"><CreditCard /><h2>Ma carte d'urgence</h2><p>Generer une URL QR securisee.</p></Link>
        <Card><Bell /><Badge tone="warning">Bientot disponible</Badge><h2>Alertes prevention</h2><p>Le backend existe, l'interface reste volontairement minimale pour le MVP.</p></Card>
        <Card><Sparkles /><Badge tone="teal">Bientot disponible</Badge><h2>Resume sante</h2><p>La synthese assistive sera connectee sans diagnostic ni prescription.</p></Card>
      </div>
    </div>
  );
}
