-- Archived SQL schema for the hackathon backend.
-- This file is kept only as schema documentation and is not executed by Spring Boot.

create table if not exists users (
  id uuid primary key,
  email varchar(255) unique not null,
  password_hash varchar(255) not null,
  first_name varchar(255) not null,
  last_name varchar(255) not null,
  role varchar(50) not null,
  enabled boolean not null default true,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists health_profiles (
  id uuid primary key,
  user_id uuid unique not null references users(id) on delete cascade,
  birth_date date,
  blood_type varchar(20),
  card_number varchar(32) unique not null,
  profile_photo_url varchar(500),
  emergency_notes varchar(1000),
  last_medical_visit_date date,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists emergency_contacts (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  full_name varchar(255) not null,
  relationship varchar(255),
  phone varchar(50) not null,
  is_primary boolean not null default false
);

create table if not exists allergies (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  label varchar(255) not null,
  severity varchar(50) not null,
  notes varchar(1000)
);

create table if not exists medical_conditions (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  label varchar(255) not null,
  status varchar(50) not null,
  notes varchar(1000)
);

create table if not exists medications (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  name varchar(255) not null,
  dosage varchar(255),
  frequency varchar(255),
  start_date date,
  end_date date,
  is_critical boolean not null default false,
  notes varchar(1000)
);

create table if not exists vaccinations (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  vaccine_name varchar(255) not null,
  administered_on date,
  next_due_date date,
  status varchar(50) not null
);

create table if not exists qr_tokens (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  token_hash varchar(255) unique not null,
  token_prefix varchar(32),
  status varchar(50) not null,
  expires_at timestamptz,
  created_at timestamptz not null,
  revoked_at timestamptz
);

create table if not exists emergency_access_logs (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  qr_token_id uuid references qr_tokens(id),
  access_type varchar(50) not null,
  requester_label varchar(255),
  source_ip_hash varchar(255),
  accessed_at timestamptz not null,
  outcome varchar(50) not null
);

create table if not exists prevention_alerts (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  type varchar(80) not null,
  severity varchar(50) not null,
  title varchar(255) not null,
  message varchar(1000) not null,
  status varchar(50) not null,
  created_at timestamptz not null,
  due_date date
);

create table if not exists ai_summaries (
  id uuid primary key,
  health_profile_id uuid not null references health_profiles(id) on delete cascade,
  content text not null,
  generated_at timestamptz not null,
  source_version varchar(255) not null,
  disclaimer_visible boolean not null
);

create index if not exists idx_users_email on users(email);
create index if not exists idx_health_profiles_user_id on health_profiles(user_id);
create index if not exists idx_qr_tokens_token_hash on qr_tokens(token_hash);
create index if not exists idx_qr_tokens_status on qr_tokens(status);
create index if not exists idx_prevention_alerts_health_profile_id on prevention_alerts(health_profile_id);
create index if not exists idx_emergency_access_logs_health_profile_id on emergency_access_logs(health_profile_id);
