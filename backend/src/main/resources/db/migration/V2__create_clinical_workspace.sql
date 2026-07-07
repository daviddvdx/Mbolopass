create table if not exists professional_profiles (
  id uuid primary key,
  user_id uuid unique not null references users(id),
  professional_type varchar(50) not null,
  speciality varchar(255),
  license_number varchar(255),
  organization_name varchar(255),
  verification_status varchar(50) not null default 'PENDING',
  verified_at timestamptz,
  verified_by_user_id uuid references users(id),
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists patient_professional_access (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id),
  professional_profile_id uuid not null references professional_profiles(id),
  status varchar(50) not null default 'PENDING',
  access_reason varchar(500),
  granted_at timestamptz,
  expires_at timestamptz,
  revoked_at timestamptz,
  granted_by_user_id uuid references users(id),
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists clinical_encounters (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id),
  professional_profile_id uuid not null references professional_profiles(id),
  patient_access_id uuid not null references patient_professional_access(id),
  encounter_type varchar(120),
  reason varchar(500),
  status varchar(50) not null,
  clinical_notes varchar(2000),
  started_at timestamptz,
  closed_at timestamptz,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists medical_reference_catalog (
  id uuid primary key,
  category varchar(50) not null,
  code_system varchar(50) not null default 'INTERNAL',
  code varchar(120),
  display_name varchar(255) not null,
  active boolean not null default true,
  created_by_user_id uuid references users(id),
  validated_by_user_id uuid references users(id),
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists medical_reference_catalog_versions (
  id uuid primary key,
  catalog_id uuid references medical_reference_catalog(id),
  version varchar(80),
  created_at timestamptz not null
);

create table if not exists exam_catalog (
  id uuid primary key,
  code varchar(120),
  name varchar(255) not null,
  category varchar(120),
  description varchar(1000),
  active boolean not null default true,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists exam_orders (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id),
  clinical_encounter_id uuid not null references clinical_encounters(id),
  ordered_by_professional_id uuid not null references professional_profiles(id),
  exam_catalog_id uuid not null references exam_catalog(id),
  status varchar(50) not null,
  priority varchar(50) not null,
  clinical_reason varchar(1000),
  ordered_at timestamptz,
  scheduled_at timestamptz,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists exam_results (
  id uuid primary key,
  exam_order_id uuid not null references exam_orders(id),
  entered_by_professional_id uuid not null references professional_profiles(id),
  validated_by_professional_id uuid references professional_profiles(id),
  result_summary varchar(1000),
  result_value varchar(255),
  unit varchar(80),
  reference_range varchar(255),
  status varchar(50) not null,
  resulted_at timestamptz,
  validated_at timestamptz,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists clinical_protocols (
  id uuid primary key,
  name varchar(255),
  description varchar(1000),
  target_category varchar(120),
  version varchar(80),
  status varchar(50),
  created_by_user_id uuid references users(id),
  validated_by_user_id uuid references users(id),
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists clinical_protocol_steps (
  id uuid primary key,
  clinical_protocol_id uuid references clinical_protocols(id),
  step_order integer not null default 0,
  title varchar(255),
  description varchar(1000),
  required boolean not null default false,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists clinical_protocol_runs (
  id uuid primary key,
  clinical_protocol_id uuid references clinical_protocols(id),
  clinical_encounter_id uuid references clinical_encounters(id),
  health_profile_id uuid references health_profiles(id),
  status varchar(50),
  started_at timestamptz,
  completed_at timestamptz,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists clinical_audit_events (
  id uuid primary key,
  actor_user_id uuid references users(id),
  actor_professional_profile_id uuid references professional_profiles(id),
  health_profile_id uuid references health_profiles(id),
  clinical_encounter_id uuid references clinical_encounters(id),
  action varchar(120) not null,
  resource_type varchar(120),
  resource_id uuid,
  access_reason varchar(500),
  ip_address varchar(120),
  user_agent varchar(500),
  created_at timestamptz not null
);

alter table allergies add column if not exists critical boolean not null default false;
alter table allergies add column if not exists clinical_encounter_id uuid;
alter table allergies add column if not exists professional_profile_id uuid;
alter table allergies add column if not exists source varchar(50) default 'PATIENT_REPORTED';
alter table allergies add column if not exists verification_status varchar(50) default 'DRAFT';
alter table allergies add column if not exists validated_at timestamptz;
alter table allergies add column if not exists validated_by_user_id uuid;

alter table medical_conditions add column if not exists clinical_encounter_id uuid;
alter table medical_conditions add column if not exists created_by_user_id uuid;
alter table medical_conditions add column if not exists professional_profile_id uuid;
alter table medical_conditions add column if not exists reference_catalog_id uuid;
alter table medical_conditions add column if not exists source varchar(50) default 'PATIENT_REPORTED';
alter table medical_conditions add column if not exists verification_status varchar(50) default 'DRAFT';
alter table medical_conditions add column if not exists clinical_status varchar(50);
alter table medical_conditions add column if not exists validated_at timestamptz;
alter table medical_conditions add column if not exists validated_by_user_id uuid;
alter table medical_conditions add column if not exists amended_from_id uuid;

create index if not exists idx_professional_profiles_user_id on professional_profiles(user_id);
create index if not exists idx_professional_profiles_status on professional_profiles(verification_status);
create index if not exists idx_patient_access_profile on patient_professional_access(health_profile_id);
create index if not exists idx_patient_access_professional on patient_professional_access(professional_profile_id);
create index if not exists idx_patient_access_status on patient_professional_access(status);
create index if not exists idx_clinical_encounters_profile on clinical_encounters(health_profile_id);
create index if not exists idx_clinical_encounters_professional on clinical_encounters(professional_profile_id);
create index if not exists idx_exam_orders_encounter on exam_orders(clinical_encounter_id);
create index if not exists idx_exam_orders_status on exam_orders(status);
create index if not exists idx_exam_results_order on exam_results(exam_order_id);
create index if not exists idx_clinical_audit_profile on clinical_audit_events(health_profile_id);
create index if not exists idx_clinical_audit_created_at on clinical_audit_events(created_at);
