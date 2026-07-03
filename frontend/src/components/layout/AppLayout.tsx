import { Baby, Bell, CreditCard, FileText, HeartPulse, Home, LogOut, QrCode, Settings, ShieldCheck, Sparkles } from 'lucide-react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { Logo } from '../Logo';

const links = [
  { to: '/tableau-de-bord', label: 'Tableau de bord', icon: Home },
  { to: '/mon-profil', label: 'Profil', icon: HeartPulse },
  { to: '/mes-enfants', label: 'Mes enfants', icon: Baby },
  { to: '/documents', label: 'Documents', icon: FileText },
  { to: '/ma-carte', label: 'Mon Passeport Santé', icon: CreditCard },
  { to: '/scanner', label: 'Scanner', icon: QrCode },
  { to: '/alertes', label: 'Alertes', icon: Bell },
  { to: '/resume', label: 'Resume', icon: Sparkles },
  { to: '/parametres', label: 'Parametres', icon: Settings }
];

export function AppLayout() {
  const { user, clearSession } = useAuth();
  const navigate = useNavigate();

  function logout() {
    clearSession();
    navigate('/connexion', { replace: true });
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <Logo compact />
        <nav aria-label="Navigation principale">
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => isActive ? 'active' : undefined}>
              <Icon size={18} /> {label}
            </NavLink>
          ))}
        </nav>
        <button type="button" onClick={logout}><LogOut size={18} /> Deconnexion</button>
      </aside>
      <section className="workspace">
        <header>
          <ShieldCheck size={18} />
          <strong>{user ? `${user.firstName} ${user.lastName}` : 'Session MboloPass'}</strong>
        </header>
        <Outlet />
      </section>
    </main>
  );
}
