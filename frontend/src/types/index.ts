export type Role = 'PATIENT' | 'HEALTH_PROFESSIONAL' | 'HEALTH_ADMIN';

export type ProfessionalProfileIdentity = {
  exists: boolean;
  professionalType: string | null;
  speciality: string | null;
  verificationStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED' | string | null;
  isApproved: boolean;
};

export type User = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  roles: string[];
  professionalProfile: ProfessionalProfileIdentity | null;
};

export type RegisterRequest = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInMinutes: number;
  user: User;
};

export type Profile = {
  id: string;
  cardNumber: string | null;
  birthDate: string | null;
  gender: string | null;
  bloodType: string | null;
  emergencyNotes: string | null;
  lastMedicalVisitDate: string | null;
  updatedAt: string | null;
};

export type ProfileRequest = {
  birthDate?: string | null;
  gender?: string | null;
  bloodType?: string | null;
  emergencyNotes?: string | null;
  lastMedicalVisitDate?: string | null;
};

export type UserProfile = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  cardNumber: string | null;
  birthDate: string | null;
  gender: string | null;
  profilePhotoUrl: string | null;
};

export type UserProfileRequest = {
  firstName: string;
  lastName: string;
  phone?: string | null;
  birthDate?: string | null;
  gender?: string | null;
};

export type HealthItemType = 'allergies' | 'conditions' | 'medications' | 'vaccinations' | 'emergency-contacts';

export type ItemRequest = {
  label: string;
  level?: string | null;
  notes?: string | null;
  dosage?: string | null;
  frequency?: string | null;
  endDate?: string | null;
  critical?: boolean;
  administeredOn?: string | null;
  nextDueDate?: string | null;
  phone?: string | null;
};

export type Item = {
  id: string;
  label: string;
  level: string | null;
  notes: string | null;
  dosage: string | null;
  frequency: string | null;
  endDate: string | null;
  critical: boolean;
  administeredOn: string | null;
  nextDueDate: string | null;
  phone: string | null;
  extra: string | null;
};

export type CardInfo = {
  cardId: string;
  cardNumber: string | null;
  fullName: string;
  identityDocumentNumber: string | null;
  birthDate: string | null;
  gender: string | null;
  bloodType: string | null;
  hasProfilePhoto: boolean;
  emergencyContact: EmergencyContact | null;
  profileCompletionPercentage: number;
  qrStatus: 'ACTIVE' | 'MISSING' | string;
  lastUpdatedAt: string | null;
};

export type QrTokenResponse = {
  emergencyUrl: string;
  expiresAt: string | null;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED' | string;
};

export type EmergencyQrResponse = {
  cardNumber: string | null;
  reference: string;
  payload: string;
  generatedAt: string | null;
  version: number;
};

export type EmergencyAllergy = {
  label: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | string;
};

export type EmergencyContact = {
  fullName: string;
  relationship: string | null;
  phone: string;
};

export type EmergencyInfo = {
  subjectType?: 'PATIENT' | 'DEPENDENT' | string;
  bloodType: string | null;
  allergies: EmergencyAllergy[];
  criticalConditions: string[];
  criticalMedications: string[];
  emergencyNotes: string | null;
  emergencyContact: EmergencyContact | null;
};

export type Dependent = {
  id: string;
  firstName: string;
  lastName: string;
  relationship: string | null;
  birthDate: string | null;
  gender: string | null;
  bloodType: string | null;
  emergencyNotes: string | null;
  enabled: boolean;
  createdAt: string | null;
  updatedAt: string | null;
  hasPhoto: boolean;
};

export type DependentRequest = {
  firstName: string;
  lastName: string;
  relationship?: string | null;
  birthDate?: string | null;
  gender?: string | null;
  bloodType?: string | null;
  emergencyNotes?: string | null;
};

export type DocumentCategory = 'PRESCRIPTION' | 'MEDICAL_RECORD' | 'LAB_RESULT' | 'CERTIFICATE' | 'OTHER';

export type MedicalDocument = {
  id: string;
  ownerType: 'PATIENT' | 'DEPENDENT' | string;
  category: DocumentCategory;
  title: string;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  issuedDate: string | null;
  uploadedAt: string | null;
};

export type Alert = {
  id: string;
  type: string;
  severity: string;
  title: string;
  message: string;
  status: string;
  dueDate: string | null;
};

export type Summary = {
  id: string;
  content: string;
  generatedAt: string;
  sourceVersion: string;
  disclaimerVisible: boolean;
  disclaimer: string;
};

export type ApiErrorBody = {
  timestamp?: string;
  status?: number;
  message?: string;
};
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminDashboard = {
  totalUsers: number;
  activeUsers: number;
  blockedUsers: number;
  patients: number;
  professionals: number;
  administrators: number;
  activeQrCards: number;
  revokedQrCards: number;
  expiredQrCards: number;
  emergencyAccessesLast24Hours: number;
  dependentProfiles: number;
  medicalDocuments: number;
  activeDependentQrCards: number;
};

export type AdminUserSummary = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string | null;
  updatedAt: string | null;
  healthProfileExists: boolean;
  activeQrCardExists: boolean;
};

export type CreateAdminUserRequest = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: 'PATIENT' | 'HEALTH_PROFESSIONAL';
};

export type AdminQrCard = {
  qrTokenId: string;
  ownerUserId: string;
  ownerType: 'PATIENT' | 'DEPENDENT' | string;
  ownerDisplayName: string;
  tokenPrefix: string | null;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED' | string;
  createdAt: string | null;
  expiresAt: string | null;
  revokedAt: string | null;
  scanCount: number;
  lastEmergencyAccessAt: string | null;
};

export type AdminAuditLog = {
  id: string;
  actorName: string;
  action: string;
  targetType: string;
  targetId: string | null;
  details: string | null;
  createdAt: string | null;
};
