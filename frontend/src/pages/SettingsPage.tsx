import { LogOut, ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Button, Card } from '../components/ui';

export function SettingsPage() {
  const { clearSession } = useAuth();
  const navigate = useNavigate();
  function logout() {
    clearSession();
    navigate('/connexion', { replace: true });
  }
  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Parametres</p><h1>Confidentialite et session</h1></div>
      <Card>
        <ShieldCheck />
        <h2>Donnees d'urgence</h2>
        <p>Le QR code ne contient aucune donnee medicale. Il contient uniquement une URL avec jeton opaque vers une fiche limitee par le backend.</p>
      </Card>
      <Card>
        <h2>Session</h2>
        <p>Le JWT est conserve uniquement en memoire pendant cette session du navigateur.</p>
        <Button type="button" onClick={logout}><LogOut size={18} /> Deconnexion</Button>
      </Card>
    </div>
  );
}