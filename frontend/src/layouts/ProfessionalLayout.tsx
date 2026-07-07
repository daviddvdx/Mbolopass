import { Activity, ClipboardList, FileSearch, FlaskConical, LayoutDashboard, LogOut, Stethoscope, UserCog, Users, UserRound } from 'lucide-react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Logo } from '../components/Logo';
import { NotificationBell } from '../components/notifications/NotificationBell';
import { Button } from '../components/ui';
import { ProfessionalStatusBadge } from '../components/professional/ProfessionalUi';

const links = [
  { to: '/professional/dashboard', label: 'Tableau de bord', icon: LayoutDashboard },
  { to: '/professional/access-requests', label: 'Demandes d acces', icon: ClipboardList },
  { to: '/professional/patients', label: 'Mes patients', icon: Users },
  { to: '/professional/encounters', label: 'Consultations', icon: Stethoscope },
  { to: '/professional/exams', label: 'Examens', icon: FlaskConical },
  { to: '/professional/profile', label: 'Mon profil professionnel', icon: UserRound }
];

const adminLinks = [
  { to: '/admin', label: 'Validation des professionnels', icon: UserCog },
  { to: '/admin/journal', label: 'Journal d audit', icon: FileSearch }
];

export function ProfessionalLayout() {
  const { user, clearSession, hasRole } = useAuth();
  const navigate = useNavigate();
  const profile = user?.professionalProfile;
  const isPatient = hasRole('PATIENT');
  const isAdmin = hasRole('HEALTH_ADMIN');

  async function logout() {
    await clearSession();
    navigate('/connexion', { replace: true });
  }

  return (
    <main className="app-shell professional-shell">
      <aside className="sidebar professional-sidebar">
        <Logo compact />
        <div className="professional-console-label"><Activity size={18} /> Espace professionnel</div>
        <nav aria-label="Navigation professionnelle">
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => isActive ? 'active' : undefined}>
              <Icon size={18} /> {label}
            </NavLink>
          ))}
        </nav>
        {isAdmin ? (
          <div className="professional-admin-zone">
            <p>Administration sante</p>
            {adminLinks.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to}>
                <Icon size={18} /> {label}
              </NavLink>
            ))}
          </div>
        ) : null}
        {isPatient ? <Button type="button" className="ghost" onClick={() => navigate('/tableau-de-bord')}><Users size={18} /> Espace patient</Button> : null}
        <button type="button" onClick={logout}><LogOut size={18} /> Deconnexion</button>
      </aside>
      <section className="workspace professional-workspace">
        <header className="professional-header">
          <div>
            <strong>{user ? `${user.firstName} ${user.lastName}` : 'Professionnel MboloPass'}</strong>
            <span>{profile?.professionalType ?? (isAdmin ? 'Administration sante' : 'Profil professionnel')}</span>
          </div>
          <ProfessionalStatusBadge status={profile?.verificationStatus ?? (isAdmin ? 'APPROVED' : null)} />
          <NotificationBell />
        </header>
        <Outlet />
      </section>
    </main>
  );
}
