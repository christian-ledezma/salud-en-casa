-- Support: the device identifiers push notifications are delivered to.

set search_path = public, extensions;

-- RF-09.4 and RF-09.5. HU-20 requires that changing device keeps notifications
-- working, so a profile may hold several tokens and each token is unique.
create table public.device_tokens (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    token text not null unique,
    platform public.device_platform not null,
    last_used_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create index idx_device_tokens_profile_id on public.device_tokens (profile_id);

alter table public.device_tokens enable row level security;

-- A device identifier belongs to its owner and to nobody else. The function
-- that sends notifications reads them as definer.
create policy device_tokens_select_own on public.device_tokens
    for select to authenticated
    using (profile_id = (select auth.uid()));

create policy device_tokens_insert_own on public.device_tokens
    for insert to authenticated
    with check (profile_id = (select auth.uid()));

create policy device_tokens_update_own on public.device_tokens
    for update to authenticated
    using (profile_id = (select auth.uid()))
    with check (profile_id = (select auth.uid()));

create policy device_tokens_delete_own on public.device_tokens
    for delete to authenticated
    using (profile_id = (select auth.uid()));
