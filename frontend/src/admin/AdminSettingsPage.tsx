import { Card } from '../components/ui';

export function AdminSettingsPage() {
  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Administration</p><h1>Parametres admin</h1></div>
      <Card><h2>Securite</h2><p>Les routes admin sont protegees cote serveur par ROLE_HEALTH_ADMIN. Aucun token QR brut, passwordHash ou dossier medical complet n’est affiche.</p></Card>
      <Card><h2>Premier HEALTH_ADMIN local</h2><p>Utilisez le script backend/sql/promote_local_admin.sql apres avoir cree un utilisateur standard.</p></Card>
    </div>
  );
}
