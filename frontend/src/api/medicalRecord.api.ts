import { api } from './client';
import type { AccessResponse, MedicalAccessGrant, MedicalConsultation, MedicalHistoryEntry, PatientRecord } from './professional.api';

export type PatientMedicalAccessRequest = AccessResponse;
export type TemporaryAccessCodeResponse = {
  code: string;
  expiresAt: string;
};

export type PatientMedicalRecord = PatientRecord & {
  accessGrants?: MedicalAccessGrant[];
  history: MedicalHistoryEntry[];
  consultations: MedicalConsultation[];
};

export function getPatientMedicalRecord(token?: string | null) {
  return api<PatientMedicalRecord>('/patient/medical-record', { token });
}

export function listPatientMedicalAccessRequests(token?: string | null) {
  return api<PatientMedicalAccessRequest[]>('/patient/medical-access-requests', { token });
}

export function approveMedicalAccessRequest(token: string | null | undefined, requestId: string) {
  return api<PatientMedicalAccessRequest>(`/patient/medical-access-requests/${requestId}/approve`, { method: 'POST', token });
}

export function denyMedicalAccessRequest(token: string | null | undefined, requestId: string) {
  return api<PatientMedicalAccessRequest>(`/patient/medical-access-requests/${requestId}/deny`, { method: 'POST', token });
}

export function generateTemporaryAccessCode(token: string | null | undefined, requestId: string) {
  return api<TemporaryAccessCodeResponse>(`/patient/medical-access-requests/${requestId}/temporary-code`, { method: 'POST', token });
}

export function revokeMedicalAccess(token: string | null | undefined, requestId: string) {
  return api<PatientMedicalAccessRequest>(`/patient/medical-access-requests/${requestId}/revoke`, { method: 'POST', token });
}
