-- MboloPass profile/dependents/documents schema patch
-- Execute manually when spring.jpa.hibernate.ddl-auto=validate.
-- Non destructive: no table is dropped and no data is deleted.

create table if not exists dependent_profiles (
  id uuid primary key,
  guardian_user_id uuid not null references users(id),
  first_name varchar(255),
  last_name varchar(255),
  relationship varchar(255),
  birth_date date null,
  gender varchar(255) null,
  blood_type varchar(255) null,
  emergency_notes varchar(255) null,
  photo_storage_key varchar(255) null,
  enabled boolean not null default true,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists dependent_allergies (
  id uuid primary key,
  dependent_profile_id uuid not null references dependent_profiles(id),
  label varchar(255),
  severity varchar(255),
  notes varchar(255) null
);

create table if not exists dependent_medical_conditions (
  id uuid primary key,
  dependent_profile_id uuid not null references dependent_profiles(id),
  label varchar(255),
  status varchar(255),
  notes varchar(255) null
);

create table if not exists dependent_medications (
  id uuid primary key,
  dependent_profile_id uuid not null references dependent_profiles(id),
  name varchar(255),
  dosage varchar(255) null,
  frequency varchar(255) null,
  start_date date null,
  end_date date null,
  is_critical boolean not null default false,
  notes varchar(255) null
);

create table if not exists dependent_emergency_contacts (
  id uuid primary key,
  dependent_profile_id uuid not null references dependent_profiles(id),
  full_name varchar(255),
  relationship varchar(255) null,
  phone varchar(255),
  is_primary boolean not null default false
);

create table if not exists medical_documents (
  id uuid primary key,
  owner_type varchar(255),
  health_profile_id uuid null references health_profiles(id),
  dependent_profile_id uuid null references dependent_profiles(id),
  category varchar(255),
  title varchar(255),
  original_filename varchar(255),
  storage_key varchar(255),
  mime_type varchar(255),
  size_bytes bigint not null default 0,
  issued_date date null,
  uploaded_at timestamptz not null,
  uploaded_by_user_id uuid not null references users(id),
  archived boolean not null default false
);

alter table qr_tokens add column if not exists dependent_profile_id uuid null;
alter table qr_tokens alter column health_profile_id drop not null;
alter table emergency_access_logs add column if not exists dependent_profile_id uuid null;
alter table emergency_access_logs alter column health_profile_id drop not null;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'fk_qr_tokens_dependent_profile') then
    alter table qr_tokens add constraint fk_qr_tokens_dependent_profile foreign key (dependent_profile_id) references dependent_profiles(id);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'fk_emergency_logs_dependent_profile') then
    alter table emergency_access_logs add constraint fk_emergency_logs_dependent_profile foreign key (dependent_profile_id) references dependent_profiles(id);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'ck_qr_tokens_single_subject') then
    alter table qr_tokens add constraint ck_qr_tokens_single_subject check (
      (health_profile_id is not null and dependent_profile_id is null)
      or (health_profile_id is null and dependent_profile_id is not null)
    );
  end if;
  if not exists (select 1 from pg_constraint where conname = 'ck_medical_documents_single_owner') then
    alter table medical_documents add constraint ck_medical_documents_single_owner check (
      (health_profile_id is not null and dependent_profile_id is null)
      or (health_profile_id is null and dependent_profile_id is not null)
    );
  end if;
end $$;

create index if not exists idx_dependent_profiles_guardian_user_id on dependent_profiles(guardian_user_id);
create index if not exists idx_dependent_allergies_dependent_profile_id on dependent_allergies(dependent_profile_id);
create index if not exists idx_dependent_conditions_dependent_profile_id on dependent_medical_conditions(dependent_profile_id);
create index if not exists idx_dependent_medications_dependent_profile_id on dependent_medications(dependent_profile_id);
create index if not exists idx_dependent_contacts_dependent_profile_id on dependent_emergency_contacts(dependent_profile_id);
create index if not exists idx_medical_documents_owner_type on medical_documents(owner_type);
create index if not exists idx_medical_documents_category on medical_documents(category);
create index if not exists idx_medical_documents_uploaded_at on medical_documents(uploaded_at);
create index if not exists idx_medical_documents_health_profile_id on medical_documents(health_profile_id);
create index if not exists idx_medical_documents_dependent_profile_id on medical_documents(dependent_profile_id);
create index if not exists idx_qr_tokens_dependent_profile_id on qr_tokens(dependent_profile_id);
create index if not exists idx_qr_tokens_status_dependents on qr_tokens(status, dependent_profile_id);
create index if not exists idx_emergency_logs_dependent_profile_id on emergency_access_logs(dependent_profile_id);

alter table allergies add column if not exists created_at timestamptz;
alter table allergies add column if not exists updated_at timestamptz;
update allergies set created_at = coalesce(created_at, CURRENT_TIMESTAMP), updated_at = coalesce(updated_at, CURRENT_TIMESTAMP);
alter table allergies alter column created_at set not null;
alter table allergies alter column updated_at set not null;

alter table medical_conditions add column if not exists created_at timestamptz;
alter table medical_conditions add column if not exists updated_at timestamptz;
update medical_conditions set created_at = coalesce(created_at, CURRENT_TIMESTAMP), updated_at = coalesce(updated_at, CURRENT_TIMESTAMP);
alter table medical_conditions alter column created_at set not null;
alter table medical_conditions alter column updated_at set not null;

create index if not exists idx_allergies_health_profile_id_label on allergies(health_profile_id, lower(label));
create index if not exists idx_medical_conditions_health_profile_id_label on medical_conditions(health_profile_id, lower(label));

alter table health_profiles add column if not exists card_number varchar(32);
create unique index if not exists ux_health_profiles_card_number on health_profiles(card_number) where card_number is not null;
