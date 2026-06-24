-- License key system: 1 key can bind to max_devices (default 1).
-- Keys stored as SHA-256 hashes; plaintext shown only once at generation.

create table if not exists public.receiptvault_license_keys (
  id uuid primary key default gen_random_uuid(),
  key_hash text not null unique,
  key_prefix text not null,
  tier text not null default 'lifetime'
    check (tier in ('monthly', 'yearly', 'lifetime')),
  max_devices int not null default 1 check (max_devices >= 1),
  expires_at timestamptz,
  revoked_at timestamptz,
  created_at timestamptz not null default now(),
  created_by text,
  notes text,
  metadata jsonb not null default '{}'::jsonb
);

create index if not exists idx_receiptvault_license_keys_prefix
  on public.receiptvault_license_keys (key_prefix);

create table if not exists public.receiptvault_license_activations (
  id uuid primary key default gen_random_uuid(),
  license_key_id uuid not null references public.receiptvault_license_keys (id) on delete cascade,
  device_hash text not null,
  device_label text,
  app_version text,
  activated_ip inet,
  last_seen_ip inet,
  activated_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  unique (license_key_id, device_hash)
);

create index if not exists idx_receiptvault_license_activations_device
  on public.receiptvault_license_activations (device_hash);

create table if not exists public.receiptvault_license_events (
  id uuid primary key default gen_random_uuid(),
  license_key_id uuid references public.receiptvault_license_keys (id) on delete set null,
  activation_id uuid references public.receiptvault_license_activations (id) on delete set null,
  event_type text not null
    check (event_type in ('activate', 'check', 'revoke', 'unbind', 'create')),
  device_hash text,
  client_ip inet,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_receiptvault_license_events_key
  on public.receiptvault_license_events (license_key_id, created_at desc);

alter table public.receiptvault_license_keys enable row level security;
alter table public.receiptvault_license_activations enable row level security;
alter table public.receiptvault_license_events enable row level security;

create policy "service_role_only_license_keys"
  on public.receiptvault_license_keys for all
  using (false) with check (false);

create policy "service_role_only_license_activations"
  on public.receiptvault_license_activations for all
  using (false) with check (false);

create policy "service_role_only_license_events"
  on public.receiptvault_license_events for all
  using (false) with check (false);

insert into public.receiptvault_app_config (key, value) values
  ('license', '{"enabled": true, "offline_grace_days": 30}'::jsonb)
on conflict (key) do update set value = excluded.value, updated_at = now();
