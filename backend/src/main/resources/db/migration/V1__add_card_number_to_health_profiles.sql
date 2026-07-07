alter table health_profiles add column if not exists card_number varchar(32);

create unique index if not exists ux_health_profiles_card_number
  on health_profiles(card_number)
  where card_number is not null;

-- Existing rows are filled by HealthPassNumberBackfillRunner with SecureRandom.
-- After a successful application startup has assigned all missing values, run:
-- alter table health_profiles alter column card_number set not null;
