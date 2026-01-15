insert into users (
  phone_number,
  password,
  role,
  onboarding_status,
  is_verified,
  created_at
)
select
  '8877712345',
  '$2a$12$3oNOdUf.kLFXjrafgJ8SjekgUZ4JG0hYyrvgtD/U1TLGwVJnSx2uK',
  'ADMIN',
  'CUSTOMER_READY',
  true,
  now()
where not exists (
  select 1 from users where phone_number = '8877712345'
);