-- MboloPass local-only admin bootstrap
-- Replace the email below with an existing local user account.
-- This script never displays or changes passwords.

do $$
declare
  target_email varchar := 'amina.demo@example.test';
  target_id uuid;
begin
  select id into target_id from users where email = lower(target_email);
  if target_id is null then
    raise exception 'User % not found. Create the account first via /api/v1/auth/register.', target_email;
  end if;

  update users set role = 'ADMIN', enabled = true, updated_at = now() where id = target_id;
  raise notice 'User % promoted to ADMIN for local development.', target_email;
end $$;