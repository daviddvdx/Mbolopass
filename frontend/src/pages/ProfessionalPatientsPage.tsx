import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listAuthorizedPatients } from '../api/professional.api';
import { useAuth } from '../auth/AuthContext';
import { AccessStatusBadge, PatientAccessBanner, ProfessionalEmptyState } from '../components/professional/ProfessionalUi';
import { Card, Input, LoadingState, Select } from '../components/ui';
import { useMemo, useState } from 'react';

export function ProfessionalPatientsPage() {
  const { token } = useAuth();
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('ACTIVE');
  const patients = useQuery({ queryKey: ['professional-patients'], queryFn: () => listAuthorizedPatients(token), enabled: Boolean(token) });
  const filtered = useMemo(() => (patients.data ?? []).filter((access) => {
    const haystack = `${access.patientName} ${access.patientId}`.toLowerCase();
    const matchesSearch = !search || haystack.includes(search.toLowerCase());
    const matchesFilter = filter === 'ALL' || access.status === filter;
    return matchesSearch && matchesFilter;
  }), [patients.data, search, filter]);

  return (
    <div className="page professional-page">
      <div className="page-heading">
        <p className="eyebrow">Patients autorises</p>
        <h1>Mes patients</h1>
        <p>Seuls les patients ayant accorde un acces actif sont listes.</p>
      </div>
      <Card><div className="professional-filters"><Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Rechercher parmi les acces autorises" /><Select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="ACTIVE">Acces actifs</option><option value="ALL">Tous</option></Select><Link className="btn ghost" to="/professional/access-requests">Demander un acces</Link></div></Card>
      <Card>
        {patients.isLoading ? <LoadingState /> : null}
        {filtered.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Patient</th><th>Identifiant patient</th><th>Acces</th><th>Active le</th><th>Expiration</th><th>Action</th></tr></thead>
              <tbody>{filtered.map((access) => (
                <tr key={access.id}>
                  <td>{access.patientName}</td>
                  <td>{mask(access.patientId)}</td>
                  <td><AccessStatusBadge status={access.status} /><PatientAccessBanner expiresAt={access.activeGrant?.expiresAt ?? null} /></td>
                  <td>{formatDate(access.activeGrant?.activatedAt ?? null)}</td>
                  <td>{formatDate(access.activeGrant?.expiresAt ?? null)}</td>
                  <td><Link className="btn small" to={`/professional/patients/${access.patientId}`}>Ouvrir la fiche</Link></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : !patients.isLoading ? <ProfessionalEmptyState title="Aucun patient autorise" text="Les patients apparaitront ici apres acceptation et activation par code temporaire." /> : null}
      </Card>
    </div>
  );
}

function mask(value: string) { return value.length > 12 ? `${value.slice(0, 4)}...${value.slice(-4)}` : value; }
function formatDate(value: string | null) { return value ? new Date(value).toLocaleDateString() : '-'; }
