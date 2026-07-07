import { api } from './client';

export type ProfessionalMe = {
  id: string;
  professionalType: string;
  speciality: string | null;
  organizationName: string | null;
  verificationStatus: string;
};

export type AccessResponse = {
  id: string;
  healthProfileId: string;
  professionalProfileId: string;
  professionalName: string;
  status: string;
  reason: string | null;
  grantedAt: string | null;
  expiresAt: string | null;
};

export type PatientRecord = {
  healthProfileId: string;
  cardNumber: string;
  firstName: string;
  lastName: string;
  bloodType: string | null;
  allergies: string[];
  conditions: string[];
  encounters: EncounterResponse[];
};

export type EncounterResponse = {
  id: string;
  healthProfileId: string;
  status: string;
  reason: string | null;
  startedAt: string | null;
  closedAt: string | null;
};

export type CatalogResponse = {
  id: string;
  category: string;
  codeSystem: string;
  code: string | null;
  displayName: string;
  active: boolean;
};

export function getProfessionalMe() {
  return api<ProfessionalMe>('/professional/me');
}

export function requestPatientAccess(payload: { healthProfileId: string; reason?: string | null }) {
  return api<AccessResponse>('/professional/access-requests', { method: 'POST', body: JSON.stringify(payload) });
}

export function listProfessionalAccessRequests() {
  return api<AccessResponse[]>('/professional/access-requests');
}

export function openPatientRecord(healthProfileId: string) {
  return api<PatientRecord>(`/professional/patients/${healthProfileId}/record`);
}

export function createEncounter(healthProfileId: string, payload: { encounterType?: string | null; reason?: string | null; clinicalNotes?: string | null }) {
  return api<EncounterResponse>(`/professional/patients/${healthProfileId}/encounters`, { method: 'POST', body: JSON.stringify(payload) });
}

export function closeEncounter(encounterId: string) {
  return api<EncounterResponse>(`/professional/encounters/${encounterId}/close`, { method: 'POST' });
}

export function addDiagnosis(encounterId: string, payload: { referenceCatalogId: string; clinicalStatus?: string | null; notes?: string | null }) {
  return api(`/professional/encounters/${encounterId}/diagnoses`, { method: 'POST', body: JSON.stringify(payload) });
}

export function addExamOrder(encounterId: string, payload: { examCatalogId: string; priority?: string | null; clinicalReason?: string | null }) {
  return api(`/professional/encounters/${encounterId}/exam-orders`, { method: 'POST', body: JSON.stringify(payload) });
}

export function listPatientAccessRequests() {
  return api<AccessResponse[]>('/patient/access-requests');
}

export function approvePatientAccess(accessId: string) {
  return api<AccessResponse>(`/patient/access-requests/${accessId}/approve`, { method: 'POST' });
}

export function revokePatientAccess(accessId: string) {
  return api<AccessResponse>(`/patient/access-requests/${accessId}/revoke`, { method: 'POST' });
}
