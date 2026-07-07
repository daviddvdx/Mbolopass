import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { createAdminUser, listAdminUsers, updateAdminUserRole, updateAdminUserStatus } from '../api/admin.api';
import { useAuth } from '../auth/AuthContext';
import { Badge, Button, Card, Input, LoadingState, Select } from '../components/ui';
import type { Role } from '../types';

export function AdminUsersPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const users = useQuery({ queryKey: ['admin-users', role, status], queryFn: () => listAdminUsers(token!, { page: 0, size: 30, search, role, status }), enabled: Boolean(token) });
  const create = useMutation({ mutationFn: (form: FormData) => createAdminUser(token!, { firstName: String(form.get('firstName')), lastName: String(form.get('lastName')), email: String(form.get('email')), password: String(form.get('password')), role: String(form.get('role')) as 'PATIENT' | 'HEALTH_PROFESSIONAL' }), onSuccess: () => { setShowCreate(false); queryClient.invalidateQueries({ queryKey: ['admin-users'] }); } });
  const statusMutation = useMutation({ mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) => updateAdminUserStatus(token!, id, enabled, enabled ? 'Deblocage admin' : 'Blocage admin'), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] }) });
  const roleMutation = useMutation({ mutationFn: ({ id, next }: { id: string; next: Role }) => updateAdminUserRole(token!, id, next), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] }) });

  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Administration</p><h1>Utilisateurs</h1><p>Aucun mot de passe ni dossier medical n’est affiche.</p></div>
      <Card><div className="admin-filters"><Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Recherche nom ou email" /><Select value={role} onChange={(event) => setRole(event.target.value)}><option value="">Tous roles</option><option value="PATIENT">PATIENT</option><option value="HEALTH_PROFESSIONAL">HEALTH_PROFESSIONAL</option><option value="HEALTH_ADMIN">HEALTH_ADMIN</option></Select><Select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tous statuts</option><option value="ACTIVE">Actifs</option><option value="BLOCKED">Bloques</option></Select><Button type="button" onClick={() => users.refetch()}><RefreshCw size={16} /> Actualiser</Button><Button type="button" onClick={() => setShowCreate(true)}><Plus size={16} /> Creer</Button></div></Card>
      {showCreate ? <Card><h2>Creer un utilisateur</h2><form className="admin-form" onSubmit={(event) => { event.preventDefault(); create.mutate(new FormData(event.currentTarget)); }}><Input name="firstName" placeholder="Prenom" required /><Input name="lastName" placeholder="Nom" required /><Input name="email" type="email" placeholder="email@example.test" required /><Input name="password" type="password" placeholder="Mot de passe temporaire" required /><Select name="role"><option value="PATIENT">PATIENT</option><option value="HEALTH_PROFESSIONAL">HEALTH_PROFESSIONAL</option></Select><Button type="submit">Creer</Button><Button type="button" className="ghost" onClick={() => setShowCreate(false)}>Annuler</Button></form></Card> : null}
      <Card>{users.isLoading ? <LoadingState /> : <div className="table-wrap"><table><thead><tr><th>Nom</th><th>Email</th><th>Role</th><th>Statut</th><th>Profil</th><th>QR actif</th><th>Actions</th></tr></thead><tbody>{users.data?.content.map((user) => <tr key={user.id}><td>{user.firstName} {user.lastName}</td><td>{user.email}</td><td><Badge>{user.role}</Badge></td><td>{user.enabled ? <Badge tone="teal">Actif</Badge> : <Badge tone="danger">Bloque</Badge>}</td><td>{user.healthProfileExists ? 'Oui' : 'Non'}</td><td>{user.activeQrCardExists ? 'Oui' : 'Non'}</td><td><div className="table-actions"><Button type="button" className="ghost" onClick={() => statusMutation.mutate({ id: user.id, enabled: !user.enabled })}>{user.enabled ? 'Bloquer' : 'Debloquer'}</Button><Select value={user.role} onChange={(event) => roleMutation.mutate({ id: user.id, next: event.target.value as Role })}><option value="PATIENT">PATIENT</option><option value="HEALTH_PROFESSIONAL">HEALTH_PROFESSIONAL</option><option value="HEALTH_ADMIN">HEALTH_ADMIN</option></Select></div></td></tr>)}</tbody></table></div>}</Card>
    </div>
  );
}
