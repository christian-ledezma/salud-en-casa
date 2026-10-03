-- HU-34: a person holds more than one role, and switches between them.
--
-- Until now profiles.role was a single nullable enum, immutable once written.
-- The stakeholder meeting of 2026-10-01 asked for one person to be both patient
-- and professional and alternate between them, the way an inDrive or Uber driver
-- alternates between driving and riding.
--
-- The model: profile_roles says which roles a person holds, profiles.active_role
-- says which one they are in. No security policy reads active_role -- a policy
-- that did would be security theatre, because someone holding both roles just
-- switches and repeats the request. Security decides by role possession and row
-- ownership; active_role is presentation state, kept server side so it survives
-- a reinstall. See docs/decisions.md, 2026-10-01.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. profile_roles: which roles a person holds
-- ---------------------------------------------------------------------------

-- Append only, like request_offers. Dropping a role would have to decide what
-- happens to that role's past services and ratings, which INV-05 forbids
-- rewriting, so there is no delete policy and no update policy. Giving up a
-- role is FA-09.
create table public.profile_roles (
    profile_id uuid not null references public.profiles (id) on delete cascade,
    role public.user_role not null,
    created_at timestamptz not null default now(),
    primary key (profile_id, role)
);

alter table public.profile_roles enable row level security;

create policy profile_roles_select_own on public.profile_roles
    for select to authenticated
    using (profile_id = (select auth.uid()));

create policy profile_roles_select_admin on public.profile_roles
    for select to authenticated
    using (public.is_admin());

-- RF-01.5 dies here, in the policy rather than in a trigger. A trigger would
-- carry the "when (not is_admin())" exception that profiles_guard_role had; this
-- check has no exception at all, so not even an administrator self-grants ADMIN
-- through the client. Granting it is a manual operation with the service key.
create policy profile_roles_insert_own on public.profile_roles
    for insert to authenticated
    with check (
        profile_id = (select auth.uid())
        and role <> 'ADMIN'
    );

create policy profile_roles_insert_admin on public.profile_roles
    for insert to authenticated
    with check (public.is_admin());

-- ---------------------------------------------------------------------------
-- 2. Carry the existing single roles over, before anything depends on them
-- ---------------------------------------------------------------------------

insert into public.profile_roles (profile_id, role)
select id, role
from public.profiles
where role is not null;

-- ---------------------------------------------------------------------------
-- 3. active_role, with the engine guaranteeing it is a role the person holds
-- ---------------------------------------------------------------------------

alter table public.profiles add column active_role public.user_role;

-- The composite foreign key points at profile_roles' primary key, so activating
-- a role the person does not hold is impossible rather than merely refused by
-- the application. A check constraint cannot query another table, and a trigger
-- would be code to maintain for something the engine already does. Same
-- criterion as INV-10 on payments.
--
-- Null until the first role is chosen: under match simple, which is the default,
-- the constraint is satisfied when any column of the key is null, and that is
-- exactly what is wanted while the person holds no role at all.
alter table public.profiles
    add constraint profiles_active_role_is_held
    foreign key (id, active_role)
    references public.profile_roles (profile_id, role);

update public.profiles
set active_role = role
where role is not null;

-- ---------------------------------------------------------------------------
-- 4. is_admin() reads the new table
-- ---------------------------------------------------------------------------

-- Still security definer, so it bypasses row level security on profile_roles
-- and cannot recurse into the policies that call it.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.profile_roles
        where profile_id = (select auth.uid())
          and role = 'ADMIN'
    );
$$;

-- ---------------------------------------------------------------------------
-- 5. The only two policies that read profiles.role
-- ---------------------------------------------------------------------------

-- These are the whole role coupling of the schema. Everything else resolves by
-- ownership (patient_id = auth.uid(), professional_id = auth.uid()) or by the
-- existence of a professionals row, so services, payments, messages, reviews and
-- request_offers need no change here.
drop policy patients_insert_own on public.patients;

create policy patients_insert_own on public.patients
    for insert to authenticated
    with check (
        id = (select auth.uid())
        and exists (
            select 1
            from public.profile_roles
            where profile_id = (select auth.uid())
              and role = 'PATIENT'
        )
    );

drop policy professionals_insert_own on public.professionals;

create policy professionals_insert_own on public.professionals
    for insert to authenticated
    with check (
        id = (select auth.uid())
        and exists (
            select 1
            from public.profile_roles
            where profile_id = (select auth.uid())
              and role = 'PROFESSIONAL'
        )
    );

-- ---------------------------------------------------------------------------
-- 6. add_my_role replaces assign_my_role
-- ---------------------------------------------------------------------------

-- Security invoker for the same reason assign_my_role was: every write below is
-- one the caller's own policy already allows, so the function adds atomicity and
-- nothing else.
--
-- It still needs to be atomic. If the second write failed, the person would hold
-- a role with no record behind it and nothing would retry.
--
-- The new role becomes the active one. Activating a professional profile and
-- landing in patient mode would leave the person looking for a screen that moved.
create or replace function public.add_my_role(p_role public.user_role)
returns public.user_role
language plpgsql
volatile
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_exists boolean;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    -- RF-01.5. profile_roles_insert_own refuses it too; refusing here first
    -- means nothing is written at all.
    if p_role = 'ADMIN' then
        raise exception 'admin_role_is_not_self_assignable';
    end if;

    if not exists (select 1 from public.profiles where id = v_caller) then
        raise exception 'profile_not_found';
    end if;

    select exists (
        select 1
        from public.profile_roles
        where profile_id = v_caller
          and role = p_role
    ) into v_exists;

    -- The primary key refuses it too. This raise is what turns a constraint
    -- violation into an error the client can tell apart from the unexpected.
    if v_exists then
        raise exception 'role_already_held';
    end if;

    insert into public.profile_roles (profile_id, role)
    values (v_caller, p_role);

    -- The role record has to exist before active_role can point at it only in
    -- the sense that profile_roles must; patients and professionals are
    -- independent of the foreign key. Both are inserted here because the role
    -- is meaningless without its record.
    if p_role = 'PATIENT' then
        insert into public.patients (id) values (v_caller)
        on conflict (id) do nothing;
    else
        insert into public.professionals (id) values (v_caller)
        on conflict (id) do nothing;
    end if;

    update public.profiles
    set active_role = p_role
    where id = v_caller;

    return p_role;
end;
$$;

revoke all on function public.add_my_role(public.user_role) from public, anon;

grant execute on function public.add_my_role(public.user_role) to authenticated;

-- ---------------------------------------------------------------------------
-- 7. save_my_profile dispatches on the active role
-- ---------------------------------------------------------------------------

-- The signature does not change, so this replaces the body in place. Reading the
-- role on the server rather than trusting the client still holds, and still
-- means an argument sent by mistake is ignored instead of written; what changes
-- is which role it reads. Dispatching on possession would be wrong for someone
-- who holds both: editing as a patient would have to send professional arguments
-- too, and the not null columns of professionals would refuse the nulls.
create or replace function public.save_my_profile(
    p_full_name text,
    p_phone text default null,
    p_birth_date date default null,
    p_emergency_contact text default null,
    p_medical_notes text default null,
    p_professional_type public.professional_type default null,
    p_specialty text default null,
    p_biography text default null,
    p_years_of_experience integer default null,
    p_base_rate_bob numeric default null,
    p_coverage_radius_km numeric default null
)
returns void
language plpgsql
volatile
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_active public.user_role;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    select active_role into v_active
    from public.profiles
    where id = v_caller;

    if not found then
        raise exception 'profile_not_found';
    end if;

    if v_active is null then
        raise exception 'no_active_role';
    end if;

    update public.profiles
    set full_name = p_full_name,
        phone = p_phone
    where id = v_caller;

    if v_active = 'PATIENT' then
        update public.patients
        set birth_date = p_birth_date,
            emergency_contact = p_emergency_contact,
            medical_notes = p_medical_notes
        where id = v_caller;
    elsif v_active = 'PROFESSIONAL' then
        update public.professionals
        set professional_type = p_professional_type,
            specialty = p_specialty,
            biography = p_biography,
            years_of_experience = p_years_of_experience,
            base_rate_bob = p_base_rate_bob,
            coverage_radius_km = p_coverage_radius_km
        where id = v_caller;
    end if;
end;
$$;

-- ---------------------------------------------------------------------------
-- 8. my_roles: the held roles and the active one in a single row
-- ---------------------------------------------------------------------------

-- security_invoker, so profiles_select_own and profile_roles_select_own decide
-- which row it returns (INV-13), exactly like my_addresses. It exists so the
-- client reads the role state in one request instead of two; embedding
-- profile_roles from profiles would be ambiguous now that two foreign keys join
-- the same pair of tables, which is the same trap HU-03 hit reading the profile.
create view public.my_roles
with (security_invoker = true) as
select
    p.id,
    p.active_role,
    coalesce(
        array_agg(r.role order by r.role) filter (where r.role is not null),
        '{}'::public.user_role[]
    ) as held_roles
from public.profiles p
left join public.profile_roles r on r.profile_id = p.id
group by p.id, p.active_role;

revoke all on public.my_roles from anon;
grant select on public.my_roles to authenticated;

-- ---------------------------------------------------------------------------
-- 9. Drop what no longer has an owner
-- ---------------------------------------------------------------------------

-- profiles_guard_role enforced "a role is never changed". That stopped being the
-- rule. The rule that remains -- a role is never held twice -- is enforced by
-- profile_roles' primary key, which needs no trigger.
drop trigger profiles_guard_role on public.profiles;
drop function public.guard_profile_role();

drop function public.assign_my_role(public.user_role);

-- profiles.role is removed rather than kept as a denormalised "primary role".
-- Two sources of truth for one fact diverge, and the copy nobody reads goes
-- stale without anything failing. See docs/decisions.md, 2026-09-08.
drop index public.idx_profiles_role;
alter table public.profiles drop column role;
