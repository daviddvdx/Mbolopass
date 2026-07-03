import { api, API_BASE_URL } from './client';
import type { DocumentCategory, MedicalDocument } from '../types';

type DocumentMetadata = {
  title: string;
  category: DocumentCategory;
  issuedDate?: string | null;
};

function upload(path: string, token: string, file: File, metadata: DocumentMetadata) {
  const form = new FormData();
  form.append('file', file);
  form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
  return api<MedicalDocument>(path, { method: 'POST', token, body: form });
}

export function listDocuments(token: string) {
  return api<MedicalDocument[]>('/documents', { token });
}

export function uploadDocument(token: string, file: File, metadata: DocumentMetadata) {
  return upload('/documents', token, file, metadata);
}

export function listDependentDocuments(token: string, dependentId: string) {
  return api<MedicalDocument[]>(`/dependents/${dependentId}/documents`, { token });
}

export function uploadDependentDocument(token: string, dependentId: string, file: File, metadata: DocumentMetadata) {
  return upload(`/dependents/${dependentId}/documents`, token, file, metadata);
}

export function archiveDocument(token: string, documentId: string) {
  return api<void>(`/documents/${documentId}`, { method: 'DELETE', token });
}

export async function downloadDocument(token: string, documentId: string) {
  const response = await fetch(`${API_BASE_URL}/documents/${documentId}/download`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store'
  });
  if (!response.ok) throw new Error('Telechargement impossible');
  return response.blob();
}
