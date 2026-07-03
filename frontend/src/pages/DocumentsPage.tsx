import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, FileUp, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { archiveDocument, downloadDocument, listDocuments, uploadDocument } from '../api/documents.api';
import { useAuth } from '../auth/AuthContext';
import { Badge, Button, Card, EmptyState, Input, LoadingState, Select } from '../components/ui';
import type { DocumentCategory } from '../types';

const categories: Array<{ value: DocumentCategory | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'Tous' },
  { value: 'PRESCRIPTION', label: 'Ordonnances' },
  { value: 'MEDICAL_RECORD', label: 'Fiches medicales' },
  { value: 'LAB_RESULT', label: 'Resultats' },
  { value: 'CERTIFICATE', label: 'Certificats' },
  { value: 'OTHER', label: 'Autres' }
];

export function DocumentsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [category, setCategory] = useState<DocumentCategory | 'ALL'>('ALL');
  const [title, setTitle] = useState('');
  const [uploadCategory, setUploadCategory] = useState<DocumentCategory>('PRESCRIPTION');
  const [issuedDate, setIssuedDate] = useState('');
  const documents = useQuery({ queryKey: ['documents'], queryFn: () => listDocuments(token!), enabled: Boolean(token) });
  const upload = useMutation({ mutationFn: (file: File) => uploadDocument(token!, file, { title: title.trim() || file.name, category: uploadCategory, issuedDate: issuedDate || null }), onSuccess: () => { setTitle(''); setIssuedDate(''); queryClient.invalidateQueries({ queryKey: ['documents'] }); } });
  const archive = useMutation({ mutationFn: (id: string) => archiveDocument(token!, id), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['documents'] }) });
  const filtered = useMemo(() => documents.data?.filter((doc) => category === 'ALL' || doc.category === category) ?? [], [documents.data, category]);

  async function download(id: string, filename: string) {
    const blob = await downloadDocument(token!, id);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  if (documents.isLoading) return <div className="page"><LoadingState /></div>;

  return (
    <div className="page">
      <div className="page-heading"><p className="eyebrow">Documents</p><h1>Ordonnances et documents medicaux</h1><p>Les fichiers sont prives, jamais exposes par le QR d'urgence.</p></div>
      <Card>
        <h2>Importer un document</h2>
        <div className="admin-form">
          <Input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Titre du document" />
          <Select value={uploadCategory} onChange={(event) => setUploadCategory(event.target.value as DocumentCategory)}>
            {categories.filter((item) => item.value !== 'ALL').map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </Select>
          <Input type="date" value={issuedDate} onChange={(event) => setIssuedDate(event.target.value)} />
          <label className="btn small" htmlFor="document-upload"><FileUp size={16} /> Importer</label>
          <input id="document-upload" className="visually-hidden" type="file" accept="application/pdf,image/jpeg,image/png" onChange={(event) => { const file = event.target.files?.[0]; if (file) upload.mutate(file); event.currentTarget.value = ''; }} />
        </div>
      </Card>
      <Card>
        <div className="card-top"><h2>Liste des documents</h2><Select value={category} onChange={(event) => setCategory(event.target.value as DocumentCategory | 'ALL')}>{categories.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</Select></div>
        {filtered.length ? <div className="table-wrap"><table><thead><tr><th>Nom</th><th>Categorie</th><th>Date</th><th>Taille</th><th>Ajout</th><th>Actions</th></tr></thead><tbody>{filtered.map((doc) => <tr key={doc.id}><td>{doc.title}<br /><small>{doc.originalFilename}</small></td><td><Badge tone="blue">{doc.category}</Badge></td><td>{doc.issuedDate ?? '-'}</td><td>{formatBytes(doc.sizeBytes)}</td><td>{doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleDateString('fr-FR') : '-'}</td><td className="table-actions"><Button type="button" className="small ghost" onClick={() => download(doc.id, doc.originalFilename)}><Download size={14} /> Telecharger</Button><Button type="button" className="small ghost" onClick={() => archive.mutate(doc.id)}><Trash2 size={14} /> Archiver</Button></td></tr>)}</tbody></table></div> : <EmptyState title="Aucun document" text="Importez seulement les fichiers necessaires a votre suivi." />}
      </Card>
    </div>
  );
}

function formatBytes(value: number) {
  if (value < 1024) return `${value} o`;
  if (value < 1024 * 1024) return `${Math.round(value / 1024)} Ko`;
  return `${(value / 1024 / 1024).toFixed(1)} Mo`;
}
