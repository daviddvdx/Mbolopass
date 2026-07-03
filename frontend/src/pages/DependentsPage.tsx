import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, QrCode, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { addDependentItem, createDependent, deleteDependentItem, generateDependentQr, listDependentItems, listDependents } from '../api/dependents.api';
import { uploadDependentDocument, listDependentDocuments } from '../api/documents.api';
import { useAuth } from '../auth/AuthContext';
import { Badge, Button, Card, EmptyState, Input, LoadingState, Select, Textarea } from '../components/ui';
import type { Dependent, HealthItemType, ItemRequest } from '../types';

const itemTypes: Array<{ type: Exclude<HealthItemType, 'vaccinations'>; title: string; defaultLevel?: string }> = [
  { type: 'allergies', title: 'Allergies', defaultLevel: 'HIGH' },
  { type: 'conditions', title: 'Conditions', defaultLevel: 'ACTIVE' },
  { type: 'medications', title: 'Medicaments' },
  { type: 'emergency-contacts', title: 'Contacts urgence' }
];

export function DependentsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [qrUrl, setQrUrl] = useState<string | null>(null);
  const [showDocuments, setShowDocuments] = useState(false);
  const dependents = useQuery({ queryKey: ['dependents'], queryFn: () => listDependents(token!), enabled: Boolean(token) });
  const selected = dependents.data?.find((item) => item.id === selectedId) ?? dependents.data?.[0] ?? null;
  const create = useMutation({
    mutationFn: (form: FormData) => createDependent(token!, cleanDependent(form)),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['dependents'] });
      setSelectedId(created.id);
      setShowDocuments(false);
    }
  });
  const qr = useMutation({ mutationFn: (id: string) => generateDependentQr(token!, id), onSuccess: (response) => setQrUrl(response.emergencyUrl) });
  const documents = useQuery({
    queryKey: ['dependent-documents', selected?.id],
    queryFn: () => listDependentDocuments(token!, selected!.id),
    enabled: Boolean(token && selected && showDocuments)
  });
  const uploadDoc = useMutation({
    mutationFn: ({ file, dependent }: { file: File; dependent: Dependent }) => uploadDependentDocument(token!, dependent.id, file, { title: file.name, category: 'OTHER' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['dependent-documents'] })
  });

  if (dependents.isLoading) return <div className="page"><LoadingState /></div>;

  return (
    <div className="page">
      <div className="page-heading">
        <p className="eyebrow">Famille</p>
        <h1>Mes enfants et personnes a charge</h1>
        <p>Chaque profil a ses propres donnees vitales et son propre QR d'urgence.</p>
      </div>
      <Card>
        <h2>Ajouter un enfant</h2>
        <form className="profile-form" onSubmit={(event) => { event.preventDefault(); create.mutate(new FormData(event.currentTarget)); }}>
          <label>Prenom<Input name="firstName" required /></label>
          <label>Nom<Input name="lastName" required /></label>
          <label>Lien<Input name="relationship" placeholder="Enfant, parent..." /></label>
          <label>Date de naissance<Input name="birthDate" type="date" /></label>
          <label>Genre<Input name="gender" /></label>
          <label>Groupe sanguin<Input name="bloodType" /></label>
          <label className="wide">Notes urgence<Textarea name="emergencyNotes" /></label>
          <Button type="submit" disabled={create.isPending}><Plus size={16} /> Ajouter</Button>
        </form>
      </Card>
      {dependents.data?.length ? (
        <div className="dependent-grid">
          {dependents.data.map((dependent) => (
            <button
              type="button"
              key={dependent.id}
              className={selected?.id === dependent.id ? 'dependent-card active' : 'dependent-card'}
              onClick={() => { setSelectedId(dependent.id); setQrUrl(null); setShowDocuments(false); }}
            >
              <strong>{dependent.firstName} {dependent.lastName}</strong>
              <span>{dependent.relationship ?? 'Personne a charge'}</span>
              {dependent.bloodType ? <Badge tone="blue">{dependent.bloodType}</Badge> : null}
              <small>Carte QR active selon backend apres generation</small>
            </button>
          ))}
        </div>
      ) : <EmptyState title="Aucun dependants" text="Ajoutez uniquement les profils que vous etes autorise a gerer." />}
      {selected ? (
        <>
          <Card>
            <div className="card-top">
              <div>
                <h2>Vous gerez le profil de : {selected.firstName} {selected.lastName}</h2>
                <p>Ses informations ne sont jamais melangees avec celles du parent.</p>
              </div>
              <Button type="button" onClick={() => qr.mutate(selected.id)}><QrCode size={16} /> Generer le QR d'urgence</Button>
            </div>
            {qrUrl ? <div className="qr-zone"><QRCodeSVG value={qrUrl} size={180} /><p>Ce QR n'affiche que les informations medicales necessaires en urgence.</p></div> : null}
          </Card>
          <div className="profile-sections">{itemTypes.map((item) => <DependentItemSection key={item.type} dependentId={selected.id} title={item.title} type={item.type} defaultLevel={item.defaultLevel} />)}</div>
          <Card>
            <h2>Documents de {selected.firstName}</h2>
            {!showDocuments ? <Button type="button" className="ghost small" onClick={() => setShowDocuments(true)}>Afficher les documents</Button> : null}
            <label className="btn small" htmlFor="dependent-document-input">Importer un document</label>
            <input id="dependent-document-input" className="visually-hidden" type="file" accept="application/pdf,image/jpeg,image/png" onChange={(event) => { const file = event.target.files?.[0]; if (file) uploadDoc.mutate({ file, dependent: selected }); event.currentTarget.value = ''; }} />
            {showDocuments ? (
              documents.data?.length ? <ul className="item-list">{documents.data.map((doc) => <li key={doc.id}><span><strong>{doc.title}</strong> - {doc.category}</span></li>)}</ul> : <p className="muted">Aucun document enfant.</p>
            ) : <p className="muted">Les documents ne sont charges qu'a l'ouverture de cette section.</p>}
          </Card>
        </>
      ) : null}
    </div>
  );
}

function DependentItemSection({ dependentId, type, title, defaultLevel }: { dependentId: string; type: Exclude<HealthItemType, 'vaccinations'>; title: string; defaultLevel?: string }) {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [label, setLabel] = useState('');
  const [level, setLevel] = useState(defaultLevel ?? '');
  const [phone, setPhone] = useState('');
  const [critical, setCritical] = useState(type === 'emergency-contacts');
  const items = useQuery({ queryKey: ['dependent-items', dependentId, type], queryFn: () => listDependentItems(token!, dependentId, type), enabled: Boolean(token) });
  const add = useMutation({ mutationFn: (payload: ItemRequest) => addDependentItem(token!, dependentId, type, payload), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['dependent-items', dependentId, type] }) });
  const remove = useMutation({ mutationFn: (id: string) => deleteDependentItem(token!, dependentId, type, id), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['dependent-items', dependentId, type] }) });

  return (
    <Card>
      <div className="section-title"><h2>{title}</h2><Badge tone="teal">Enfant</Badge></div>
      <div className="inline-form">
        <Input value={label} onChange={(event) => setLabel(event.target.value)} placeholder="Information a renseigner" />
        {type === 'allergies' ? <Select value={level} onChange={(event) => setLevel(event.target.value)}><option value="LOW">LOW</option><option value="MEDIUM">MEDIUM</option><option value="HIGH">HIGH</option><option value="CRITICAL">CRITICAL</option></Select> : null}
        {type === 'conditions' ? <Select value={level} onChange={(event) => setLevel(event.target.value)}><option value="ACTIVE">ACTIVE</option><option value="HISTORICAL">HISTORICAL</option></Select> : null}
        {type === 'emergency-contacts' ? <Input value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="Telephone" /> : null}
        {(type === 'medications' || type === 'emergency-contacts') ? <label className="checkbox"><input type="checkbox" checked={critical} onChange={(event) => setCritical(event.target.checked)} /> Critique/principal</label> : null}
        <Button type="button" onClick={() => { if (!label.trim()) return; add.mutate({ label: label.trim(), level: level || null, phone: phone || null, critical }); setLabel(''); setPhone(''); }}><Plus size={16} /> Ajouter</Button>
      </div>
      {items.data?.length ? <ul className="item-list">{items.data.map((item) => <li key={item.id}><span><strong>{item.label}</strong>{item.level ? ` - ${item.level}` : ''}{item.phone ? ` - ${item.phone}` : ''}</span><button type="button" onClick={() => remove.mutate(item.id)}><Trash2 size={16} /></button></li>)}</ul> : <EmptyState title="Aucun element" text="Ajoutez uniquement des informations utiles en urgence." />}
    </Card>
  );
}

function cleanDependent(form: FormData) {
  const value = (name: string) => String(form.get(name) ?? '').trim();
  return {
    firstName: value('firstName'),
    lastName: value('lastName'),
    relationship: value('relationship') || null,
    birthDate: value('birthDate') || null,
    gender: value('gender') || null,
    bloodType: value('bloodType') || null,
    emergencyNotes: value('emergencyNotes') || null
  };
}
