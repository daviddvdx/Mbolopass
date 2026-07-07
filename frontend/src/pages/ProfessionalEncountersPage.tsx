import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { listEncounters } from '../api/professional.api';
import { useAuth } from '../auth/AuthContext';
import { EncounterStatusBadge, ProfessionalEmptyState } from '../components/professional/ProfessionalUi';
import { Card, LoadingState, Select } from '../components/ui';
import { useState } from 'react';

export function ProfessionalEncountersPage() {
  const { token } = useAuth();
  const [status, setStatus] = useState('');
  const encounters = useQuery({ queryKey: ['professional-encounters', status], queryFn: () => listEncounters(token, { status: status || undefined }), enabled: Boolean(token) });

  return (
    <div className="page professional-page">
      <div className="page-heading">
        <p className="eyebrow">Activite clinique</p>
        <h1>Consultations</h1>
        <p>Les consultations sont deduites des fiches patient auxquelles vous avez un acces actif.</p>
      </div>
      <Card><div className="professional-filters"><Select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tous statuts</option><option value="DRAFT">DRAFT</option><option value="IN_PROGRESS">IN_PROGRESS</option><option value="CLOSED">CLOSED</option><option value="CANCELLED">CANCELLED</option></Select><Link className="btn ghost" to="/professional/patients">Creer depuis une fiche patient</Link></div></Card>
      <Card>
        {encounters.isLoading ? <LoadingState /> : null}
        {encounters.data?.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Motif</th><th>Patient</th><th>Statut</th><th>Debut</th><th>Cloture</th><th>Action</th></tr></thead>
              <tbody>{encounters.data.map((encounter) => (
                <tr key={encounter.id}>
                  <td>{encounter.reason ?? 'Sans motif'}</td>
                  <td>{encounter.healthProfileId.slice(0, 8)}</td>
                  <td><EncounterStatusBadge status={encounter.status} /></td>
                  <td>{formatDate(encounter.startedAt)}</td>
                  <td>{formatDate(encounter.closedAt)}</td>
                  <td><Link to={`/professional/encounters/${encounter.id}`}>Ouvrir</Link></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : !encounters.isLoading ? <ProfessionalEmptyState title="Aucune consultation" text="Ouvrez une fiche patient autorisee pour creer une consultation." /> : null}
      </Card>
    </div>
  );
}

function formatDate(value: string | null) { return value ? new Date(value).toLocaleString() : '-'; }
