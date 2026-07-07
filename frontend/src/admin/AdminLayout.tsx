import { BarChart3, CreditCard, FileText, LogOut, Settings, Shield, Users } from 'lucide-react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Logo } from '../components/Logo';
import { NotificationBell } from '../components/notifications/NotificationBell';

const links = [
  { to: '/admin', label: 'Vue generale', icon: BarChart3 },
  { to: '/admin/utilisateurs', label: 'Utilisateurs', icon: Users },
  { to: '/admin/cartes', label: 'Cartes QR', icon: CreditCard },
  { to: '/admin/journal', label: 'Journal', icon: FileText },
  { to: '/admin/parametres', label: 'Parametres', icon: Settings }
];

export function AdminLayout() {
  const { clearSession } = useAuth();
  const navigate = useNavigate();
  async function logout() { await clearSession(); navigate('/connexion', { replace: true }); }
  return (
    <main className="app-shell admin-shell">
      <aside className="sidebar admin-sidebar">
        <Logo compact />
        <div className="admin-console-label"><Shield size={18} /> Console d’administration</div>
        <nav aria-label="Navigation administration">
          {links.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} end={to === '/admin'} className={({ isActive }) => isActive ? 'active' : undefined}><Icon size={18} /> {label}</NavLink>)}
        </nav>
        <button type="button" onClick={logout}><LogOut size={18} /> Deconnexion</button>
      </aside>
      <section className="workspace admin-workspace"><header><NotificationBell /></header><Outlet /></section>
    </main>
  );
}
