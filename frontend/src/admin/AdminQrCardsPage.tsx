import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { listAdminQrCards, revokeAdminQrCard } from '../api/admin.api';
import { useAuth } from '../auth/AuthContext';
import { Badge, Button, Card, Input, LoadingState, Select } from '../components/ui';

export function AdminQrCardsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const cards = useQuery({ queryKey: ['admin-qr-cards', status], queryFn: () => listAdminQrCards(token!, { page: 0, size: 30, search, status }), enabled: Boolean(token) });
  const revoke = useMutation({ mutationFn: (id: string) => revokeAdminQrCard(token!, id, 'Carte revoquee depuis la console admin'), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-qr-cards'] }) });
  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Administration</p><h1>Cartes QR</h1><p>Le token QR brut n’est jamais affiche.</p></div>
      <Card><div className="admin-filters"><Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Recherche proprietaire" /><Select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tous statuts</option><option value="ACTIVE">ACTIVE</option><option value="REVOKED">REVOKED</option><option value="EXPIRED">EXPIRED</option></Select><Button type="button" onClick={() => cards.refetch()}><RefreshCw size={16} /> Actualiser</Button></div></Card>
      <Card>{cards.isLoading ? <LoadingState /> : <div className="table-wrap"><table><thead><tr><th>Titulaire</th><th>Type</th><th>Prefixe</th><th>Statut</th><th>Creation</th><th>Expiration</th><th>Scans</th><th>Dernier acces</th><th>Action</th></tr></thead><tbody>{cards.data?.content.map((card) => <tr key={card.qrTokenId}><td>{card.ownerDisplayName}</td><td><Badge tone={card.ownerType === 'DEPENDENT' ? 'teal' : 'blue'}>{card.ownerType}</Badge></td><td>{card.tokenPrefix ?? '-'}</td><td><Badge tone={card.status === 'ACTIVE' ? 'teal' : card.status === 'REVOKED' ? 'danger' : 'warning'}>{card.status}</Badge></td><td>{formatDate(card.createdAt)}</td><td>{formatDate(card.expiresAt)}</td><td>{card.scanCount}</td><td>{formatDate(card.lastEmergencyAccessAt)}</td><td>{card.status === 'ACTIVE' ? <Button type="button" className="ghost" onClick={() => window.confirm('Revoquer cette carte QR ?') && revoke.mutate(card.qrTokenId)}>Revoquer</Button> : '-'}</td></tr>)}</tbody></table></div>}</Card>
    </div>
  );
}

function formatDate(value: string | null) { return value ? new Date(value).toLocaleString('fr-FR') : '-'; }
