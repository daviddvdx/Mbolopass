import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { PatientRoute } from './auth/PatientRoute';
import { ProfessionalRoute } from './auth/ProfessionalRoute';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { LoadingState } from './components/ui';

const AdminAuditLogPage = lazy(() => import('./admin/AdminAuditLogPage').then((module) => ({ default: module.AdminAuditLogPage })));
const AdminDashboardPage = lazy(() => import('./admin/AdminDashboardPage').then((module) => ({ default: module.AdminDashboardPage })));
const AdminLayout = lazy(() => import('./admin/AdminLayout').then((module) => ({ default: module.AdminLayout })));
const AdminQrCardsPage = lazy(() => import('./admin/AdminQrCardsPage').then((module) => ({ default: module.AdminQrCardsPage })));
const AdminRoute = lazy(() => import('./admin/AdminRoute').then((module) => ({ default: module.AdminRoute })));
const AdminSettingsPage = lazy(() => import('./admin/AdminSettingsPage').then((module) => ({ default: module.AdminSettingsPage })));
const AdminUsersPage = lazy(() => import('./admin/AdminUsersPage').then((module) => ({ default: module.AdminUsersPage })));
const AppLayout = lazy(() => import('./components/layout/AppLayout').then((module) => ({ default: module.AppLayout })));
const AlertsPage = lazy(() => import('./pages/AlertsPage').then((module) => ({ default: module.AlertsPage })));
const CardPage = lazy(() => import('./pages/CardPage').then((module) => ({ default: module.CardPage })));
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((module) => ({ default: module.DashboardPage })));
const DependentsPage = lazy(() => import('./pages/DependentsPage').then((module) => ({ default: module.DependentsPage })));
const DocumentsPage = lazy(() => import('./pages/DocumentsPage').then((module) => ({ default: module.DocumentsPage })));
const EmergencyPage = lazy(() => import('./pages/EmergencyPage').then((module) => ({ default: module.EmergencyPage })));
const LandingPage = lazy(() => import('./pages/LandingPage').then((module) => ({ default: module.LandingPage })));
const LoginPage = lazy(() => import('./pages/LoginPage').then((module) => ({ default: module.LoginPage })));
const PatientMedicalRecordPage = lazy(() => import('./pages/PatientMedicalRecordPage').then((module) => ({ default: module.PatientMedicalRecordPage })));
const ProfilePage = lazy(() => import('./pages/ProfilePage').then((module) => ({ default: module.ProfilePage })));
const ProfessionalLayout = lazy(() => import('./layouts/ProfessionalLayout').then((module) => ({ default: module.ProfessionalLayout })));
const ProfessionalAccessRequestsPage = lazy(() => import('./pages/ProfessionalAccessRequestsPage').then((module) => ({ default: module.ProfessionalAccessRequestsPage })));
const ProfessionalDashboardPage = lazy(() => import('./pages/ProfessionalDashboardPage').then((module) => ({ default: module.ProfessionalDashboardPage })));
const ProfessionalEncounterDetailsPage = lazy(() => import('./pages/ProfessionalEncounterDetailsPage').then((module) => ({ default: module.ProfessionalEncounterDetailsPage })));
const ProfessionalEncountersPage = lazy(() => import('./pages/ProfessionalEncountersPage').then((module) => ({ default: module.ProfessionalEncountersPage })));
const ProfessionalExamsPage = lazy(() => import('./pages/ProfessionalExamsPage').then((module) => ({ default: module.ProfessionalExamsPage })));
const ProfessionalPatientRecordPage = lazy(() => import('./pages/ProfessionalPatientRecordPage').then((module) => ({ default: module.ProfessionalPatientRecordPage })));
const ProfessionalPatientsPage = lazy(() => import('./pages/ProfessionalPatientsPage').then((module) => ({ default: module.ProfessionalPatientsPage })));
const ProfessionalPendingPage = lazy(() => import('./pages/ProfessionalPendingPage').then((module) => ({ default: module.ProfessionalPendingPage })));
const ProfessionalProfilePage = lazy(() => import('./pages/ProfessionalProfilePage').then((module) => ({ default: module.ProfessionalProfilePage })));
const ProfessionalRestrictedPage = lazy(() => import('./pages/ProfessionalRestrictedPage').then((module) => ({ default: module.ProfessionalRestrictedPage })));
const RegisterPage = lazy(() => import('./pages/RegisterPage').then((module) => ({ default: module.RegisterPage })));
const SettingsPage = lazy(() => import('./pages/SettingsPage').then((module) => ({ default: module.SettingsPage })));
const SummaryPage = lazy(() => import('./pages/SummaryPage').then((module) => ({ default: module.SummaryPage })));
const UnauthorizedPage = lazy(() => import('./pages/UnauthorizedPage').then((module) => ({ default: module.UnauthorizedPage })));

export default function App() {
  return (
    <Suspense fallback={<LoadingState />}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/connexion" element={<LoginPage />} />
        <Route path="/inscription" element={<RegisterPage />} />
        <Route path="/urgence/:token" element={<EmergencyPage />} />
        <Route path="/non-autorise" element={<UnauthorizedPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/professional/pending-verification" element={<ProfessionalPendingPage />} />
          <Route path="/professional/access-restricted" element={<ProfessionalRestrictedPage />} />
          <Route element={<PatientRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/tableau-de-bord" element={<DashboardPage />} />
              <Route path="/mon-profil" element={<ProfilePage />} />
              <Route path="/mes-enfants" element={<DependentsPage />} />
              <Route path="/documents" element={<DocumentsPage />} />
              <Route path="/dossier-medical" element={<PatientMedicalRecordPage />} />
              <Route path="/ma-carte" element={<CardPage />} />
              <Route path="/alertes" element={<AlertsPage />} />
              <Route path="/resume" element={<SummaryPage />} />
              <Route path="/parametres" element={<SettingsPage />} />
            </Route>
          </Route>
          <Route element={<AdminRoute />}>
            <Route element={<AdminLayout />}>
              <Route path="/admin" element={<AdminDashboardPage />} />
              <Route path="/admin/utilisateurs" element={<AdminUsersPage />} />
              <Route path="/admin/cartes" element={<AdminQrCardsPage />} />
              <Route path="/admin/journal" element={<AdminAuditLogPage />} />
              <Route path="/admin/parametres" element={<AdminSettingsPage />} />
            </Route>
          </Route>
          <Route element={<ProfessionalRoute />}>
            <Route element={<ProfessionalLayout />}>
              <Route path="/professional" element={<Navigate to="/professional/dashboard" replace />} />
              <Route path="/professional/dashboard" element={<ProfessionalDashboardPage />} />
              <Route path="/professional/access-requests" element={<ProfessionalAccessRequestsPage />} />
              <Route path="/professional/patients" element={<ProfessionalPatientsPage />} />
              <Route path="/professional/patients/:healthProfileId" element={<ProfessionalPatientRecordPage />} />
              <Route path="/professional/encounters" element={<ProfessionalEncountersPage />} />
              <Route path="/professional/encounters/:encounterId" element={<ProfessionalEncounterDetailsPage />} />
              <Route path="/professional/exams" element={<ProfessionalExamsPage />} />
              <Route path="/professional/profile" element={<ProfessionalProfilePage />} />
            </Route>
          </Route>
        </Route>
        <Route path="/login" element={<Navigate to="/connexion" replace />} />
        <Route path="/register" element={<Navigate to="/inscription" replace />} />
        <Route path="/dashboard" element={<Navigate to="/tableau-de-bord" replace />} />
        <Route path="/profile" element={<Navigate to="/mon-profil" replace />} />
        <Route path="/card" element={<Navigate to="/ma-carte" replace />} />
        <Route path="/emergency/:token" element={<EmergencyPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
