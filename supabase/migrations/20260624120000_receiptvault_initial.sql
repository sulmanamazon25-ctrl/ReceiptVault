-- ReceiptVault backend schema (separate Supabase project — do NOT apply to other projects).
-- Supports Play entitlement audit, app config, and support contact (optional future online features).

create table if not exists public.receiptvault_app_config (
  key text primary key,
  value jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

comment on table public.receiptvault_app_config is
  'Remote config: min_app_version, maintenance_mode, feature_flags.';

insert into public.receiptvault_app_config (key, value) values
  ('release', '{"min_version_code": 1, "maintenance_mode": false}'::jsonb),
  ('billing', '{"products": ["pro_monthly", "pro_yearly", "pro_lifetime"]}'::jsonb)
on conflict (key) do nothing;

create table if not exists public.receiptvault_play_entitlements (
  id uuid primary key default gen_random_uuid(),
  purchase_token text not null unique,
  product_id text not null,
  purchase_type text not null check (purchase_type in ('monthly', 'yearly', 'lifetime')),
  package_name text not null default 'com.receiptvault.app',
  purchase_time timestamptz,
  expiry_time timestamptz,
  acknowledged_at timestamptz not null default now(),
  raw_payload jsonb default '{}'::jsonb
);

create index if not exists idx_receiptvault_play_entitlements_product
  on public.receiptvault_play_entitlements (product_id);

comment on table public.receiptvault_play_entitlements is
  'Server-side audit of Google Play purchases (webhook / support). App remains offline-first.';

create table if not exists public.receiptvault_support_messages (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  subject text,
  message text not null,
  app_version text,
  created_at timestamptz not null default now()
);

comment on table public.receiptvault_support_messages is
  'Optional support form submissions from a future landing page.';

-- RLS: deny public access; service role only for admin/webhooks.
alter table public.receiptvault_app_config enable row level security;
alter table public.receiptvault_play_entitlements enable row level security;
alter table public.receiptvault_support_messages enable row level security;

create policy "service_role_only_app_config"
  on public.receiptvault_app_config
  for all
  using (false)
  with check (false);

create policy "service_role_only_play_entitlements"
  on public.receiptvault_play_entitlements
  for all
  using (false)
  with check (false);

create policy "service_role_only_support"
  on public.receiptvault_support_messages
  for all
  using (false)
  with check (false);

-- Allow anonymous read of release config only (for future force-update checks).
create policy "anon_read_release_config"
  on public.receiptvault_app_config
  for select
  to anon
  using (key = 'release');
