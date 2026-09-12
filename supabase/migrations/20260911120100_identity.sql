-- Identity: profiles, the two role tables, verification documents and addresses.
-- Every table is created together with its row level security policies (INV-02).
--
-- Order matters in this file. PostgreSQL validates the body of a `language sql`
-- function when it is created, so every such function appears after the tables
-- it reads.

set search_path = public, extensions;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------

-- INV-01: the single identity of every person. The primary key is the
-- identifier issued by the authentication provider; patients and professionals
-- share it instead of duplicating the person.
create table public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    full_name text not null check (char_length(full_name) between 1 and 120),
    email text not null check (char_length(email) between 3 and 320),
    phone text check (phone is null or char_length(phone) between 7 and 20),
    photo_url text,
    -- Null until the user chooses between patient and professional (RF-01.4).
    role public.user_role,
    active boolean not null default true,
    -- Reputation lives here because both roles are rated (RF-12.1, RF-12.2).
    average_rating numeric(3, 2) not null default 0 check (average_rating between 0 and 5),
    total_reviews integer not null default 0 check (total_reviews >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_profiles_role on public.profiles (role) where active;

-- Reads the caller's role bypassing row level security. A policy on profiles
-- that queried profiles directly would recurse forever, which is why this is a
-- security definer function.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.profiles
        where id = (select auth.uid())
          and role = 'ADMIN'
    );
$$;

create trigger profiles_set_updated_at
    before update on public.profiles
    for each row execute function public.set_updated_at();

-- RF-01.4 and RF-01.5: the role is chosen once and ADMIN is never
-- self-assignable. Enforced here because a client side check is not a guarantee.
create or replace function public.guard_profile_role()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.role is distinct from old.role then
        if old.role is not null then
            raise exception 'role_already_assigned';
        end if;
        if new.role = 'ADMIN' then
            raise exception 'admin_role_is_not_self_assignable';
        end if;
    end if;
    return new;
end;
$$;

create trigger profiles_guard_role
    before update on public.profiles
    for each row
    when (not public.is_admin())
    execute function public.guard_profile_role();

alter table public.profiles enable row level security;

create policy profiles_select_own on public.profiles
    for select to authenticated
    using (id = (select auth.uid()));

create policy profiles_select_admin on public.profiles
    for select to authenticated
    using (public.is_admin());

create policy profiles_update_own on public.profiles
    for update to authenticated
    using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

create policy profiles_update_admin on public.profiles
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

-- No insert policy on purpose: rows are created by the trigger on auth.users,
-- which runs as definer. No delete policy: deleting the auth user cascades.
-- The policy that reveals contact details to the counterpart is added by the
-- migration that creates services, because that is the condition it depends on.

-- ---------------------------------------------------------------------------
-- patients
-- ---------------------------------------------------------------------------

create table public.patients (
    id uuid primary key references public.profiles (id) on delete cascade,
    birth_date date check (birth_date is null or birth_date < current_date),
    emergency_contact text,
    medical_notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create trigger patients_set_updated_at
    before update on public.patients
    for each row execute function public.set_updated_at();

alter table public.patients enable row level security;

create policy patients_select_own on public.patients
    for select to authenticated
    using (id = (select auth.uid()));

create policy patients_select_admin on public.patients
    for select to authenticated
    using (public.is_admin());

create policy patients_insert_own on public.patients
    for insert to authenticated
    with check (id = (select auth.uid()));

create policy patients_update_own on public.patients
    for update to authenticated
    using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- professionals
-- ---------------------------------------------------------------------------

create table public.professionals (
    id uuid primary key references public.profiles (id) on delete cascade,
    professional_type public.professional_type not null,
    specialty text,
    biography text,
    base_rate_bob numeric(10, 2) not null check (base_rate_bob > 0),
    years_of_experience integer not null default 0 check (years_of_experience between 0 and 70),
    coverage_radius_km numeric(5, 2) not null default 5
        check (coverage_radius_km > 0 and coverage_radius_km <= 50),
    available_now boolean not null default false,
    verification_status public.review_status not null default 'PENDING',
    total_services integer not null default 0 check (total_services >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_professionals_verification_status on public.professionals (verification_status);
create index idx_professionals_available_now on public.professionals (available_now) where available_now;

create trigger professionals_set_updated_at
    before update on public.professionals
    for each row execute function public.set_updated_at();

-- Only an administrator moves a professional out of PENDING (RF-04.4, INV-07).
create or replace function public.guard_verification_status()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.verification_status is distinct from old.verification_status then
        raise exception 'verification_status_is_set_by_an_administrator';
    end if;
    return new;
end;
$$;

create trigger professionals_guard_verification_status
    before update on public.professionals
    for each row
    when (not public.is_admin())
    execute function public.guard_verification_status();

alter table public.professionals enable row level security;

create policy professionals_select_own on public.professionals
    for select to authenticated
    using (id = (select auth.uid()));

create policy professionals_select_admin on public.professionals
    for select to authenticated
    using (public.is_admin());

create policy professionals_insert_own on public.professionals
    for insert to authenticated
    with check (id = (select auth.uid()));

create policy professionals_update_own on public.professionals
    for update to authenticated
    using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- Public directory of professionals
-- ---------------------------------------------------------------------------

-- RF-02.6 and RF-06.3 require a professional's public card to be readable by
-- any signed in user, while RN-06 keeps the phone number hidden until an offer
-- is accepted. Row level security cannot hide a single column, so the public
-- projection is a view that simply does not contain the contact columns.
--
-- The view runs with the privileges of its owner on purpose: it must read rows
-- the caller cannot read directly. Its own filter is what enforces INV-07, and
-- it exposes no contact data by construction.
create view public.professional_directory
with (security_invoker = false) as
select
    pro.id,
    p.full_name,
    p.photo_url,
    p.average_rating,
    p.total_reviews,
    pro.professional_type,
    pro.specialty,
    pro.biography,
    pro.base_rate_bob,
    pro.years_of_experience,
    pro.coverage_radius_km,
    pro.available_now,
    pro.total_services
from public.professionals pro
join public.profiles p on p.id = pro.id
where pro.verification_status = 'APPROVED'
  and p.active;

revoke all on public.professional_directory from anon;
grant select on public.professional_directory to authenticated;

-- ---------------------------------------------------------------------------
-- verification_documents
-- ---------------------------------------------------------------------------

create table public.verification_documents (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    document_type public.document_type not null,
    storage_path text not null,
    status public.review_status not null default 'PENDING',
    reviewed_by uuid references public.profiles (id) on delete set null,
    reviewed_at timestamptz,
    rejection_reason text,
    created_at timestamptz not null default now(),
    unique (profile_id, document_type),
    -- RF-04.4: a rejection always carries its reason, and only a rejection does.
    constraint rejection_reason_matches_status check (
        (status = 'REJECTED' and rejection_reason is not null)
        or (status <> 'REJECTED' and rejection_reason is null)
    )
);

create index idx_verification_documents_profile_id on public.verification_documents (profile_id);
create index idx_verification_documents_status on public.verification_documents (status);

-- The owner uploads and may replace the file while it is still pending, but the
-- outcome of the review is written by an administrator only (RF-04.4).
create or replace function public.guard_document_review()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.status is distinct from old.status
        or new.reviewed_by is distinct from old.reviewed_by
        or new.rejection_reason is distinct from old.rejection_reason then
        raise exception 'document_review_is_written_by_an_administrator';
    end if;
    return new;
end;
$$;

create trigger verification_documents_guard_review
    before update on public.verification_documents
    for each row
    when (not public.is_admin())
    execute function public.guard_document_review();

alter table public.verification_documents enable row level security;

-- A user never reads the documents of another user (INV-13, RF-04.3).
create policy verification_documents_select_own on public.verification_documents
    for select to authenticated
    using (profile_id = (select auth.uid()));

create policy verification_documents_select_admin on public.verification_documents
    for select to authenticated
    using (public.is_admin());

create policy verification_documents_insert_own on public.verification_documents
    for insert to authenticated
    with check (profile_id = (select auth.uid()));

create policy verification_documents_update_own on public.verification_documents
    for update to authenticated
    using (profile_id = (select auth.uid()) and status = 'PENDING')
    with check (profile_id = (select auth.uid()));

create policy verification_documents_update_admin on public.verification_documents
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

create policy verification_documents_delete_own on public.verification_documents
    for delete to authenticated
    using (profile_id = (select auth.uid()) and status = 'PENDING');

-- ---------------------------------------------------------------------------
-- addresses
-- ---------------------------------------------------------------------------

create table public.addresses (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    alias text not null check (char_length(alias) between 1 and 60),
    address_text text not null check (char_length(address_text) between 1 and 300),
    reference text,
    city text not null,
    -- The point is built with longitude first and latitude second. Inverting the
    -- order returns empty or absurd results without raising an error.
    location extensions.geography(Point, 4326) not null,
    is_primary boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Without this index the proximity query degrades to a sequential scan (INV-08).
create index idx_addresses_location on public.addresses using gist (location);
create index idx_addresses_profile_id on public.addresses (profile_id);

-- RF-03.4: at most one primary address per person.
create unique index idx_addresses_profile_primary
    on public.addresses (profile_id)
    where is_primary;

create trigger addresses_set_updated_at
    before update on public.addresses
    for each row execute function public.set_updated_at();

-- Marking an address as primary unmarks the previous one instead of failing
-- against the unique index above.
create or replace function public.unmark_previous_primary_address()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    update public.addresses
    set is_primary = false
    where profile_id = new.profile_id
      and id <> new.id
      and is_primary;
    return new;
end;
$$;

create trigger addresses_unmark_previous_primary
    before insert or update of is_primary on public.addresses
    for each row
    when (new.is_primary)
    execute function public.unmark_previous_primary_address();

alter table public.addresses enable row level security;

-- Addresses are private to their owner. The proximity search reads them through
-- a definer function, and the professional of an accepted service reads the
-- snapshot stored on the request, never this table.
create policy addresses_select_own on public.addresses
    for select to authenticated
    using (profile_id = (select auth.uid()));

create policy addresses_insert_own on public.addresses
    for insert to authenticated
    with check (profile_id = (select auth.uid()));

create policy addresses_update_own on public.addresses
    for update to authenticated
    using (profile_id = (select auth.uid()))
    with check (profile_id = (select auth.uid()));

create policy addresses_delete_own on public.addresses
    for delete to authenticated
    using (profile_id = (select auth.uid()));
