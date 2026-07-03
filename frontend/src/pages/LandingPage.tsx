import { ArrowRight, CreditCard, LockKeyhole, QrCode, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { Card } from '../components/ui';

export function LandingPage() {
  return (
    <main className="landing-page">
      <nav className="topbar">
        <Logo />
        <div className="nav-actions">
          <Link to="/connexion">Connexion</Link>
          <Link className="btn small" to="/inscription">Creer mon passeport</Link>
        </div>
      </nav>

      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">MboloPass</p>
          <h1>Votre passeport de sante numerique.</h1>
          <p className="lead">Votre sante, votre acces.</p>
          <div className="actions">
            <Link className="btn" to="/inscription">Creer mon passeport <ArrowRight size={18} /></Link>
            <Link className="btn ghost" to="/connexion">Se connecter</Link>
          </div>
        </div>
        <div className="phone-preview" aria-label="Apercu carte MboloPass">
          <div className="phone-card">
            <ShieldCheck size={26} />
            <strong>Carte MboloPass</strong>
            <span>QR d'urgence securise</span>
            <div className="qr-placeholder"><QrCode size={92} /></div>
            <small>Acces limite aux informations vitales.</small>
          </div>
        </div>
      </section>

      <section className="landing-grid" aria-label="Fonctionnalites principales">
        <Card><CreditCard /><h2>Carte de sante</h2><p>Un espace patient pour renseigner les informations gerees par le backend: profil, groupe sanguin, notes d'urgence et contacts.</p></Card>
        <Card><QrCode /><h2>QR d'urgence</h2><p>Le QR ouvre une URL publique avec jeton opaque. Il ne contient aucune donnee medicale.</p></Card>
        <Card><LockKeyhole /><h2>Securite</h2><p>JWT en memoire, acces d'urgence limite, pas de stockage local du token ou du QR dans le navigateur.</p></Card>
      </section>

      <footer className="footer">Cyber241 · MboloPass</footer>
    </main>
  );
}