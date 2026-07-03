import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Camera, FileText, HeartPulse, Plus, ShieldAlert, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { addItem, deleteItem, deleteProfilePhoto, fetchProfilePhoto, getProfile, getUserProfile, listItems, updateProfile, updateUserProfile, uploadProfilePhoto } from '../api/profile.api';
import { useAuth } from '../auth/AuthContext';
import { Badge, Button, Card, EmptyState, Input, LoadingState, Select, Textarea } from '../components/ui';
import type { HealthItemType, ItemRequest, ProfileRequest, UserProfileRequest } from '../types';

const itemSections: Array<{ type: HealthItemType; title: string; levelLabel: string; defaultLevel?: string; showPhone?: boolean; showCritical?: boolean; placeholder: string }> = [
  { type: 'allergies', title: 'Allergies', levelLabel: 'Severite', defaultLevel: 'HIGH', placeholder: 'Information a renseigner' },
  { type: 'conditions', title: 'Conditions', levelLabel: 'Statut', defaultLevel: 'ACTIVE', placeholder: 'Information a renseigner' },
  { type: 'medications', title: 'Medicaments', levelLabel: 'Critique', showCritical: true, placeholder: 'Information a renseigner' },
  { type: 'emergency-contacts', title: 'Contact d’urgence', levelLabel: 'Relation', showPhone: true, showCritical: true, placeholder: 'Contact principal' }
];

export function ProfilePage() {
  const { token, user, setUser } = useAuth();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState<ProfileRequest>({});
  const [identityDraft, setIdentityDraft] = useState<Partial<UserProfileRequest>>({});
  const [activeTab, setActiveTab] = useState<'health' | 'emergency' | 'documents'>('health');
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const enabled = Boolean(token);
  const visibleItemSections = useMemo(() => {
    if (activeTab === 'health') return itemSections.filter((section) => section.type !== 'emergency-contacts');
    if (activeTab === 'emergency') return itemSections.filter((section) => section.type === 'emergency-contacts');
    return [];
  }, [activeTab]);
  const userProfile = useQuery({ queryKey: ['user-profile'], queryFn: () => getUserProfile(token!), enabled });
  const profile = useQuery({ queryKey: ['profile'], queryFn: () => getProfile(token!), enabled });
  const items = useQuery({
    queryKey: ['profile-items', activeTab],
    enabled: enabled && visibleItemSections.length > 0,
    queryFn: async () => Object.fromEntries(await Promise.all(visibleItemSections.map(async (section) => [section.type, await listItems(token!, section.type)]))) as Record<HealthItemType, Awaited<ReturnType<typeof listItems>>>
  });
  const saveProfile = useMutation({ mutationFn: (payload: ProfileRequest) => updateProfile(token!, payload), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['profile'] }) });
  const saveIdentity = useMutation({
    mutationFn: (payload: UserProfileRequest) => updateUserProfile(token!, payload),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['user-profile'] });
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setIdentityDraft({});
      if (user) setUser({ ...user, firstName: updated.firstName, lastName: updated.lastName, email: updated.email });
    }
  });
  const add = useMutation({ mutationFn: ({ type, payload }: { type: HealthItemType; payload: ItemRequest }) => addItem(token!, type, payload), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['profile-items'] }) });
  const remove = useMutation({ mutationFn: ({ type, id }: { type: HealthItemType; id: string }) => deleteItem(token!, type, id), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['profile-items'] }) });
  const uploadPhoto = useMutation({ mutationFn: (file: File) => uploadProfilePhoto(token!, file), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['user-profile'] }); reloadPhoto(true); } });
  const removePhoto = useMutation({ mutationFn: () => deleteProfilePhoto(token!), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['user-profile'] }); if (photoUrl) URL.revokeObjectURL(photoUrl); setPhotoUrl(null); } });

  const currentIdentity = userProfile.data;
  const current = profile.data;
  const identityValues = useMemo(() => ({
    firstName: identityDraft.firstName ?? currentIdentity?.firstName ?? '',
    lastName: identityDraft.lastName ?? currentIdentity?.lastName ?? '',
    email: currentIdentity?.email ?? '',
    birthDate: identityDraft.birthDate ?? currentIdentity?.birthDate ?? '',
    gender: identityDraft.gender ?? currentIdentity?.gender ?? ''
  }), [currentIdentity, identityDraft]);
  const formValues = useMemo(() => ({
    birthDate: draft.birthDate ?? current?.birthDate ?? '',
    gender: draft.gender ?? current?.gender ?? '',
    bloodType: draft.bloodType ?? current?.bloodType ?? '',
    emergencyNotes: draft.emergencyNotes ?? current?.emergencyNotes ?? '',
    lastMedicalVisitDate: draft.lastMedicalVisitDate ?? current?.lastMedicalVisitDate ?? ''
  }), [current, draft]);

  const reloadPhoto = useCallback((force = false) => {
    if (!token || (!force && !currentIdentity?.profilePhotoUrl)) {
      setPhotoUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return null;
      });
      return;
    }
    fetchProfilePhoto(token).then((blob) => {
      setPhotoUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return blob ? URL.createObjectURL(blob) : null;
      });
    });
  }, [currentIdentity?.profilePhotoUrl, token]);

  useEffect(() => {
    reloadPhoto();
    return () => setPhotoUrl((previous) => {
      if (previous) URL.revokeObjectURL(previous);
      return null;
    });
  }, [reloadPhoto]);

  if (userProfile.isLoading || profile.isLoading || (visibleItemSections.length > 0 && items.isLoading)) return <div className="page"><LoadingState /></div>;

  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Profil patient</p><h1>Mon profil sante</h1><p>Seuls les champs exposes par le backend sont affiches.</p></div>
      <Card>
        <div className="profile-photo-card">
          <div className="avatar">{photoUrl ? <img src={photoUrl} alt="Photo de profil privee" /> : <Camera size={34} />}</div>
          <div>
            <h2>Photo de profil</h2>
            <p>Photo privee chargee avec votre jeton de session. Elle n'apparait jamais sur la fiche urgence publique.</p>
            <div className="actions compact-actions">
              <label className="btn small" htmlFor="profile-photo-input"><Camera size={16} /> {photoUrl ? 'Modifier la photo' : 'Ajouter une photo'}</label>
              <input id="profile-photo-input" className="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => { const file = event.target.files?.[0]; if (file) uploadPhoto.mutate(file); event.currentTarget.value = ''; }} />
              {photoUrl ? <Button type="button" className="ghost small" onClick={() => removePhoto.mutate()} disabled={removePhoto.isPending}>Supprimer la photo</Button> : null}
            </div>
          </div>
        </div>
      </Card>
      <Card>
        <h2>Identite du compte</h2>
        <form className="profile-form" onSubmit={(event) => { event.preventDefault(); saveIdentity.mutate(cleanIdentity(identityValues)); }}>
          <label>Prenom<Input value={identityValues.firstName} onChange={(event) => setIdentityDraft((value) => ({ ...value, firstName: event.target.value }))} /></label>
          <label>Nom<Input value={identityValues.lastName} onChange={(event) => setIdentityDraft((value) => ({ ...value, lastName: event.target.value }))} /></label>
          <label>Email<Input value={identityValues.email} disabled /></label>
          <label>Date de naissance<Input type="date" value={identityValues.birthDate} onChange={(event) => setIdentityDraft((value) => ({ ...value, birthDate: event.target.value }))} /></label>
          <label>Genre<Input value={identityValues.gender} onChange={(event) => setIdentityDraft((value) => ({ ...value, gender: event.target.value }))} placeholder="Optionnel" /></label>
          <Button type="submit" disabled={saveIdentity.isPending}>Enregistrer l'identite</Button>
        </form>
      </Card>
      <div className="tabs" role="tablist" aria-label="Sections du profil">
        <button type="button" className={activeTab === 'health' ? 'active' : ''} onClick={() => setActiveTab('health')}><HeartPulse size={16} /> Informations sante</button>
        <button type="button" className={activeTab === 'emergency' ? 'active' : ''} onClick={() => setActiveTab('emergency')}><ShieldAlert size={16} /> Urgences</button>
        <button type="button" className={activeTab === 'documents' ? 'active' : ''} onClick={() => setActiveTab('documents')}><FileText size={16} /> Documents medicaux</button>
      </div>
      <Card>
        <form className="profile-form" onSubmit={(event) => { event.preventDefault(); saveProfile.mutate(cleanProfile(formValues)); }}>
          <label>Date de naissance<Input type="date" value={formValues.birthDate} onChange={(event) => setDraft((value) => ({ ...value, birthDate: event.target.value }))} /></label>
          <label>Genre<Input value={formValues.gender} onChange={(event) => setDraft((value) => ({ ...value, gender: event.target.value }))} placeholder="Optionnel" /></label>
          <label>Groupe sanguin<Input value={formValues.bloodType} onChange={(event) => setDraft((value) => ({ ...value, bloodType: event.target.value }))} placeholder="O+" /></label>
          <label>Derniere visite medicale<Input type="date" value={formValues.lastMedicalVisitDate} onChange={(event) => setDraft((value) => ({ ...value, lastMedicalVisitDate: event.target.value }))} /></label>
          <label className="wide">Notes d'urgence<Textarea value={formValues.emergencyNotes} onChange={(event) => setDraft((value) => ({ ...value, emergencyNotes: event.target.value }))} rows={4} placeholder="Notes limitees utiles en urgence" /></label>
          <Button type="submit" disabled={saveProfile.isPending}>Enregistrer</Button>
        </form>
      </Card>
      {activeTab === 'health' ? <div className="profile-sections">
        {itemSections.filter((section) => section.type !== 'emergency-contacts').map((section) => <ItemSection key={section.type} section={section} items={items.data?.[section.type] ?? []} onAdd={(payload) => add.mutate({ type: section.type, payload })} onDelete={(id) => remove.mutate({ type: section.type, id })} />)}
      </div> : null}
      {activeTab === 'emergency' ? <div className="profile-sections">
        {itemSections.filter((section) => section.type === 'emergency-contacts').map((section) => <ItemSection key={section.type} section={section} items={items.data?.[section.type] ?? []} onAdd={(payload) => add.mutate({ type: section.type, payload })} onDelete={(id) => remove.mutate({ type: section.type, id })} />)}
      </div> : null}
      {activeTab === 'documents' ? <Card><h2>Ordonnances et documents medicaux</h2><p>Les documents sont geres dans la page dediee et ne sont jamais exposes via le QR public.</p><Button type="button" onClick={() => window.location.assign('/documents')}>Ouvrir mes documents</Button></Card> : null}
    </div>
  );
}

function ItemSection({ section, items, onAdd, onDelete }: { section: (typeof itemSections)[number]; items: Awaited<ReturnType<typeof listItems>>; onAdd: (payload: ItemRequest) => void; onDelete: (id: string) => void }) {
  const [label, setLabel] = useState('');
  const [level, setLevel] = useState(section.defaultLevel ?? '');
  const [phone, setPhone] = useState('');
  const [critical, setCritical] = useState(Boolean(section.showCritical));

  function submit() {
    if (!label.trim()) return;
    onAdd({ label: label.trim(), level: level || null, phone: phone || null, critical });
    setLabel('');
    setPhone('');
  }

  return (
    <Card>
      <div className="section-title"><h2>{section.title}</h2><Badge tone="blue">Backend</Badge></div>
      <div className="inline-form">
        <Input value={label} onChange={(event) => setLabel(event.target.value)} placeholder={section.placeholder} />
        {section.type === 'allergies' ? <Select value={level} onChange={(event) => setLevel(event.target.value)}><option value="LOW">LOW</option><option value="MEDIUM">MEDIUM</option><option value="HIGH">HIGH</option><option value="CRITICAL">CRITICAL</option></Select> : null}
        {section.type === 'conditions' ? <Select value={level} onChange={(event) => setLevel(event.target.value)}><option value="ACTIVE">ACTIVE</option><option value="HISTORICAL">HISTORICAL</option></Select> : null}
        {section.showPhone ? <Input value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="Telephone" /> : null}
        {section.showCritical ? <label className="checkbox"><input type="checkbox" checked={critical} onChange={(event) => setCritical(event.target.checked)} /> Critique/principal</label> : null}
        <Button type="button" onClick={submit}><Plus size={16} /> Ajouter</Button>
      </div>
      {items.length ? <ul className="item-list">{items.map((item) => <li key={item.id}><span><strong>{item.label}</strong>{item.level ? ` · ${item.level}` : ''}{item.phone ? ` · ${item.phone}` : ''}</span><button type="button" aria-label="Supprimer" onClick={() => onDelete(item.id)}><Trash2 size={16} /></button></li>)}</ul> : <EmptyState title="Aucun element" text="Ajoutez uniquement les informations necessaires a cette section." />}
    </Card>
  );
}

function cleanProfile(value: ProfileRequest): ProfileRequest {
  return Object.fromEntries(Object.entries(value).map(([key, entry]) => [key, entry === '' ? null : entry])) as ProfileRequest;
}

function cleanIdentity(value: { firstName: string; lastName: string; birthDate: string; gender: string }): UserProfileRequest {
  return {
    firstName: value.firstName.trim(),
    lastName: value.lastName.trim(),
    birthDate: value.birthDate || null,
    gender: value.gender || null,
    phone: null
  };
}


