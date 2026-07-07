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
  patientId: string;
  healthProfessionalId: string;
  patientName: string;
  professionalName: string;
  status: 'PENDING' | 'APPROVED' | 'ACTIVE' | 'DENIED' | 'REVOKED' | 'EXPIRED' | 'LOCKED' | string;
  reason: string | null;
  requestedAt: string | null;
  respondedAt: string | null;
  codeExpiresAt: string | null;
  activeGrant: MedicalAccessGrant | null;
};

export type MedicalAccessGrant = {
  id: string;
  patientId: string;
  healthProfessionalId: string;
  activatedAt: string | null;
  expiresAt: string | null;
  status: string;
};

export type PatientRecord = {
  id: string;
  patientId: string;
  firstName: string;
  lastName: string;
  gender: string | null;
  birthYear: number | null;
  history: MedicalHistoryEntry[];
  consultations: MedicalConsultation[];
};

export type PatientSearchResult = {
  id: string;
  firstName: string;
  lastName: string;
  gender: string | null;
  birthYear: number | null;
};

export type PatientSearchPage = {
  content: PatientSearchResult[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type MedicalHistoryEntry = {
  id: string;
  category: string;
  title: string;
  description: string | null;
  startDate: string | null;
  endDate: string | null;
  active: boolean;
  createdAt: string | null;
  updatedAt: string | null;
};

export type MedicalConsultation = {
  id: string;
  healthProfessionalId: string;
  professionalName: string;
  consultationDate: string | null;
  reason: string | null;
  diagnosis: string | null;
  notes: string | null;
  treatment: string | null;
  followUpDate: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type EncounterResponse = {
  id: string;
  healthProfileId: string;
  status: 'DRAFT' | 'IN_PROGRESS' | 'CLOSED' | 'CANCELLED' | string;
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

export type ExamCatalogResponse = {
  id: string;
  code: string | null;
  name: string;
  category: string | null;
  active: boolean;
};

export type ExamOrderResponse = {
  id: string;
  encounterId: string;
  examName: string;
  status: string;
};

export type ProfessionalDashboard = {
  patientsAuthorized: number;
  pendingAccessRequests: number;
  activeEncounters: number;
  examsToComplete: number | null;
  resultsToValidate: number | null;
  recentAccesses: AccessResponse[];
};

export function getProfessionalMe(token?: string | null) {
  return api<ProfessionalMe>('/professional/me', { token });
}

export function listAccessRequests(token?: string | null) {
  return api<AccessResponse[]>('/health-professionals/medical-access-requests', { token });
}

export function searchPatients(token: string | null | undefined, query: string, page = 0, size = 10) {
  return api<PatientSearchPage>(`/health-professionals/patients?query=${encodeURIComponent(query)}&page=${page}&size=${size}`, { token });
}

export function createAccessRequest(token: string | null | undefined, payload: { patientId: string; reason?: string | null }) {
  return api<AccessResponse>('/health-professionals/medical-access-requests', { method: 'POST', token, body: JSON.stringify({ patientId: payload.patientId, reason: payload.reason }) });
}

export function activateAccessRequest(token: string | null | undefined, requestId: string, code: string) {
  return api<AccessResponse>(`/health-professionals/medical-access-requests/${requestId}/activate`, { method: 'POST', token, body: JSON.stringify({ code }) });
}

export async function getProfessionalDashboard(token?: string | null): Promise<ProfessionalDashboard> {
  const accesses = await listAccessRequests(token);
  return {
    patientsAuthorized: accesses.filter((access) => access.status === 'ACTIVE').length,
    pendingAccessRequests: accesses.filter((access) => access.status === 'PENDING').length,
    activeEncounters: 0,
    examsToComplete: null,
    resultsToValidate: null,
    recentAccesses: accesses.slice(0, 5)
  };
}

export async function listAuthorizedPatients(token?: string | null) {
  return (await listAccessRequests(token)).filter((access) => access.status === 'ACTIVE');
}

export function getPatientRecord(token: string | null | undefined, healthProfileId: string) {
  return api<PatientRecord>(`/health-professionals/patients/${healthProfileId}/medical-record`, { token });
}

export async function listEncounters(token?: string | null, filters?: { status?: string }) {
  const patients = await listAuthorizedPatients(token);
  const records = await Promise.allSettled(patients.map((access) => getPatientRecord(token, access.patientId)));
  const encounters = records.flatMap((result) => result.status === 'fulfilled'
    ? result.value.consultations.map((consultation) => ({
      id: consultation.id,
      healthProfileId: result.value.patientId,
      status: 'IN_PROGRESS',
      reason: consultation.reason,
      startedAt: consultation.consultationDate,
      closedAt: null
    } satisfies EncounterResponse))
    : []);
  return filters?.status ? encounters.filter((encounter) => encounter.status === filters.status) : encounters;
}

export function createEncounter(token: string | null | undefined, healthProfileId: string, payload: { encounterType?: string | null; reason?: string | null; clinicalNotes?: string | null }) {
  return api<MedicalConsultation>(`/health-professionals/patients/${healthProfileId}/consultations`, { method: 'POST', token, body: JSON.stringify({ reason: payload.reason ?? 'Consultation', notes: payload.clinicalNotes ?? null }) });
}

export function createMedicalHistory(token: string | null | undefined, patientId: string, payload: { category: string; title: string; description?: string | null; active?: boolean }) {
  return api<MedicalHistoryEntry>(`/health-professionals/patients/${patientId}/medical-history`, { method: 'POST', token, body: JSON.stringify(payload) });
}

export function getEncounter(token: string | null | undefined, encounterId: string) {
  return api<EncounterResponse>(`/professional/encounters/${encounterId}`, { token });
}

export function updateEncounter(token: string | null | undefined, encounterId: string, payload: { reason?: string | null; clinicalNotes?: string | null }) {
  return api<EncounterResponse>(`/professional/encounters/${encounterId}`, { method: 'PATCH', token, body: JSON.stringify(payload) });
}

export function closeEncounter(token: string | null | undefined, encounterId: string) {
  return api<EncounterResponse>(`/professional/encounters/${encounterId}/close`, { method: 'POST', token });
}

export function addDiagnosisToEncounter(token: string | null | undefined, encounterId: string, payload: { referenceCatalogId: string; clinicalStatus?: string | null; notes?: string | null }) {
  return api(`/professional/encounters/${encounterId}/diagnoses`, { method: 'POST', token, body: JSON.stringify(payload) });
}

export function addAllergyToEncounter(token: string | null | undefined, encounterId: string, payload: { label: string; level: string; critical: boolean; notes?: string | null }) {
  return api(`/professional/encounters/${encounterId}/allergies`, { method: 'POST', token, body: JSON.stringify(payload) });
}

export function createExamOrder(token: string | null | undefined, encounterId: string, payload: { examCatalogId: string; priority?: string | null; clinicalReason?: string | null }) {
  return api<ExamOrderResponse>(`/professional/encounters/${encounterId}/exam-orders`, { method: 'POST', token, body: JSON.stringify(payload) });
}

export function listExamOrders() {
  return Promise.resolve({ items: [] as ExamOrderResponse[], backendAvailable: false });
}

export function listMedicalCatalog(token?: string | null) {
  return api<CatalogResponse[]>('/professional/medical-catalog', { token });
}

export function listExamCatalog(token?: string | null) {
  return api<ExamCatalogResponse[]>('/professional/exam-catalog', { token });
}
