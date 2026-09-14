-- HU-02: choosing a role writes it on profiles and creates the matching role
-- record in one transaction.
--
-- Two separate writes from the client cannot guarantee that. If the second one
-- failed, the person would carry a role with no record behind it, nothing would
-- ever retry, and the application would route them into a role that does not
-- exist. That is the partial record .claude/rules/supabase.md reserves a stored
-- function to prevent.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- A professional profile is completed after the role is chosen, not with it
-- ---------------------------------------------------------------------------

-- RF-01.4 asks for the role before any other functionality. RF-02.2 puts the
-- professional type and the base rate in the professional profile, which HU-04
-- collects in the next sprint. The table demanded both at insert time, so the
-- role could not be chosen without inventing a type and a rate on the person's
-- behalf. Both become nullable and the constraint below keeps an incomplete
-- professional out of everything INV-07 makes visible: professional_directory
-- and search_nearby_professionals both read only APPROVED rows.
alter table public.professionals
    alter column professional_type drop not null,
    alter column base_rate_bob drop not null;

alter table public.professionals
    add constraint professionals_approved_profile_is_complete
    check (
        verification_status <> 'APPROVED'
        or (professional_type is not null and base_rate_bob is not null)
    );

-- ---------------------------------------------------------------------------
-- The role choice itself
-- ---------------------------------------------------------------------------

-- Security invoker on purpose: every write below is one the caller is already
-- allowed to make by its own policy, so the function adds atomicity and
-- nothing else. It never needs to bypass a policy, and a definer function that
-- did would be a hole with no reason to exist.
--
-- patients_insert_own and professionals_insert_own read profiles.role to check
-- the row matches the chosen role. The update above is visible to them because
-- both statements run inside this one transaction.
create or replace function public.assign_my_role(p_role public.user_role)
returns public.user_role
language plpgsql
volatile
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_assigned public.user_role;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    -- RF-01.5. The trigger profiles_guard_role refuses it too; refusing here
    -- first means the row is never written at all.
    if p_role = 'ADMIN' then
        raise exception 'admin_role_is_not_self_assignable';
    end if;

    select role into v_assigned
    from public.profiles
    where id = v_caller;

    if not found then
        raise exception 'profile_not_found';
    end if;

    if v_assigned is not null then
        raise exception 'role_already_assigned';
    end if;

    update public.profiles
    set role = p_role
    where id = v_caller;

    if p_role = 'PATIENT' then
        insert into public.patients (id) values (v_caller);
    else
        insert into public.professionals (id) values (v_caller);
    end if;

    return p_role;
end;
$$;

revoke all on function public.assign_my_role(public.user_role) from public, anon;

grant execute on function public.assign_my_role(public.user_role) to authenticated;
