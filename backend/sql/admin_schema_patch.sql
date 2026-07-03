-- MboloPass admin schema patch
-- Execute manually after reviewing. Required when spring.jpa.hibernate.ddl-auto=validate.

create table if not exists admin_audit_logs (
  id uuid primary key,
  actor_user_id uuid not null references users(id),
  action varchar(80) not null,
  target_type varchar(80) not null,
  target_id uuid null,
  details varchar(1000) null,
  created_at timestamptz not null,
  source_ip_hash varchar(255) null
);

create index if not exists idx_admin_audit_logs_actor_user_id on admin_audit_logs(actor_user_id);
create index if not exists idx_admin_audit_logs_action on admin_audit_logs(action);
create index if not exists idx_admin_audit_logs_created_at on admin_audit_logs(created_at);