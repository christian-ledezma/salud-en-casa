-- Catalog: the service types the platform offers, what each professional
-- declares to provide, and the weekly availability they publish.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- service_types
-- ---------------------------------------------------------------------------

-- RF-05.1: reference catalog maintained by the platform.
create table public.service_types (
    id uuid primary key default gen_random_uuid(),
    name text not null unique check (char_length(name) between 1 and 80),
    description text,
    reference_price_bob numeric(10, 2) not null check (reference_price_bob > 0),
    estimated_duration_min integer not null check (estimated_duration_min between 1 and 1440),
    active boolean not null default true,
    created_at timestamptz not null default now()
);

alter table public.service_types enable row level security;

-- The catalog is reference data: every signed in user reads it, only an
-- administrator writes it.
create policy service_types_select_all on public.service_types
    for select to authenticated
    using (true);

create policy service_types_write_admin on public.service_types
    for all to authenticated
    using (public.is_admin())
    with check (public.is_admin());

-- ---------------------------------------------------------------------------
-- professional_services
-- ---------------------------------------------------------------------------

-- RF-02.3: which types a professional provides and at what price.
create table public.professional_services (
    id uuid primary key default gen_random_uuid(),
    professional_id uuid not null references public.professionals (id) on delete cascade,
    service_type_id uuid not null references public.service_types (id) on delete restrict,
    reference_price_bob numeric(10, 2) not null check (reference_price_bob > 0),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (professional_id, service_type_id)
);

create index idx_professional_services_professional_id
    on public.professional_services (professional_id);
create index idx_professional_services_service_type_id
    on public.professional_services (service_type_id) where active;

create trigger professional_services_set_updated_at
    before update on public.professional_services
    for each row execute function public.set_updated_at();

alter table public.professional_services enable row level security;

create policy professional_services_select_own on public.professional_services
    for select to authenticated
    using (professional_id = (select auth.uid()));

-- Part of the public card of an approved professional (RF-02.6). Restricted to
-- approved and active professionals, same condition as INV-07.
create policy professional_services_select_public on public.professional_services
    for select to authenticated
    using (
        exists (
            select 1
            from public.professional_directory d
            where d.id = professional_services.professional_id
        )
    );

create policy professional_services_write_own on public.professional_services
    for all to authenticated
    using (professional_id = (select auth.uid()))
    with check (professional_id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- availability_slots
-- ---------------------------------------------------------------------------

-- RF-02.4: weekly availability declared by the professional. day_of_week
-- follows the ISO convention where 1 is Monday and 7 is Sunday.
create table public.availability_slots (
    id uuid primary key default gen_random_uuid(),
    professional_id uuid not null references public.professionals (id) on delete cascade,
    day_of_week smallint not null check (day_of_week between 1 and 7),
    start_time time not null,
    end_time time not null,
    created_at timestamptz not null default now(),
    constraint slot_ends_after_it_starts check (end_time > start_time),
    unique (professional_id, day_of_week, start_time, end_time)
);

create index idx_availability_slots_professional_id
    on public.availability_slots (professional_id);

alter table public.availability_slots enable row level security;

create policy availability_slots_select_own on public.availability_slots
    for select to authenticated
    using (professional_id = (select auth.uid()));

-- A patient needs to see the slots to schedule an appointment (RF-07.3).
create policy availability_slots_select_public on public.availability_slots
    for select to authenticated
    using (
        exists (
            select 1
            from public.professional_directory d
            where d.id = availability_slots.professional_id
        )
    );

create policy availability_slots_write_own on public.availability_slots
    for all to authenticated
    using (professional_id = (select auth.uid()))
    with check (professional_id = (select auth.uid()));
