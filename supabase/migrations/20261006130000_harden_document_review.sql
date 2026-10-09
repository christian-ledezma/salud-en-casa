-- HU-09 review fixes. See docs/decisions.md, 2026-10-06.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. Changing the professional type withdraws the verification
-- ---------------------------------------------------------------------------

-- required_document_types depends on the type, and save_my_profile lets the owner
-- change it. Without this, an approved STUDENT who switches to DOCTOR stays
-- APPROVED and public without a license. Demoting is done here, in the guard that
-- already owns verification_status, because a separate trigger ordered before it
-- would be refused by it.
create or replace function public.guard_verification_status()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if old.verification_status = 'APPROVED'
        and new.professional_type is distinct from old.professional_type then
        new.verification_status := 'PENDING';
        return new;
    end if;

    if new.verification_status is distinct from old.verification_status then
        raise exception 'verification_status_is_set_by_an_administrator';
    end if;
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- 2. The promotion locks the row before it reads what it decides on
-- ---------------------------------------------------------------------------

-- Dropped and recreated: a function cannot change its return type in place, and
-- the APPROVED it used to return was a constant nobody read.
drop function public.approve_professional_verification(uuid);

create function public.approve_professional_verification(p_profile_id uuid)
returns void
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_type public.professional_type;
    v_rate numeric;
begin
    if not public.is_admin() then
        raise exception 'not_authorized';
    end if;

    -- for update: a rejection or a type change that commits between the check
    -- below and the write would otherwise be overwritten by an APPROVED.
    select professional_type, base_rate_bob into v_type, v_rate
    from public.professionals
    where id = p_profile_id
    for update;

    if not found then
        raise exception 'profile_is_not_a_professional';
    end if;

    -- The check constraint refuses it too; this names the failure for the client.
    if v_type is null or v_rate is null then
        raise exception 'professional_profile_incomplete';
    end if;

    if exists (
        select 1
        from unnest(public.required_document_types(p_profile_id)) as required(document_type)
        where not exists (
            select 1 from public.verification_documents d
            where d.profile_id = p_profile_id
              and d.document_type = required.document_type
              and d.status = 'APPROVED'
        )
    ) then
        raise exception 'required_documents_not_approved';
    end if;

    update public.professionals
    set verification_status = 'APPROVED'
    where id = p_profile_id;
end;
$$;

revoke all on function public.approve_professional_verification(uuid) from public, anon;
grant execute on function public.approve_professional_verification(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 3. The audit stamp cannot be rewritten without a verdict
-- ---------------------------------------------------------------------------

-- The first version fired only on a change of status, so an administrator could
-- still rewrite reviewed_by and reviewed_at on a row whose status stayed put.
drop trigger verification_documents_stamp_review on public.verification_documents;

create or replace function public.stamp_document_review()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.status is not distinct from old.status then
        new.reviewed_by := old.reviewed_by;
        new.reviewed_at := old.reviewed_at;
        return new;
    end if;

    if new.status = 'PENDING' then
        new.reviewed_by := null;
        new.reviewed_at := null;
        new.rejection_reason := null;
    else
        new.reviewed_by := (select auth.uid());
        new.reviewed_at := now();
        if new.status = 'APPROVED' then
            new.rejection_reason := null;
        end if;
    end if;
    return new;
end;
$$;

create trigger verification_documents_stamp_review
    before update on public.verification_documents
    for each row
    when (public.is_admin())
    execute function public.stamp_document_review();

-- ---------------------------------------------------------------------------
-- 4. required_document_types runs as its caller
-- ---------------------------------------------------------------------------

-- Its only callers are the two definer functions above, which already run as the
-- owner, so it never needed to skip row level security on its own. Same body:
-- RequiredDocumentsParityTest reads it from here.
create or replace function public.required_document_types(p_profile_id uuid)
returns public.document_type[]
language sql
stable
set search_path = ''
as $$
    select case
        when exists (
            select 1 from public.profile_roles
            where profile_id = p_profile_id and role = 'PROFESSIONAL'
        ) then
            case
                when (select professional_type from public.professionals where id = p_profile_id) = 'STUDENT'
                    then array['ID_FRONT', 'ID_BACK', 'SELFIE', 'DEGREE', 'STUDENT_CARD']::public.document_type[]
                else array['ID_FRONT', 'ID_BACK', 'SELFIE', 'DEGREE', 'LICENSE']::public.document_type[]
            end
        when exists (
            select 1 from public.profile_roles
            where profile_id = p_profile_id and role = 'PATIENT'
        ) then array['ID_FRONT', 'ID_BACK', 'SELFIE']::public.document_type[]
        else '{}'::public.document_type[]
    end;
$$;

-- ---------------------------------------------------------------------------
-- 5. An administrator no longer grants roles from the client
-- ---------------------------------------------------------------------------

-- Nothing in the application used it, and it let a stolen administrator account
-- mint another administrator. Granting ADMIN is a manual operation again until
-- HT-17 gives that power to the super administrator alone (RF-01.5).
drop policy profile_roles_insert_admin on public.profile_roles;

-- ---------------------------------------------------------------------------
-- 6. The read models expose only what the screens read
-- ---------------------------------------------------------------------------

drop view public.document_review_queue;
drop view public.document_review_profiles;

create view public.document_review_queue
with (security_invoker = true) as
select
    d.id,
    d.profile_id,
    d.document_type,
    d.caption,
    d.created_at,
    p.full_name,
    p.email
from public.verification_documents d
join public.profiles p on p.id = d.profile_id
where d.status = 'PENDING';

revoke all on public.document_review_queue from anon;
grant select on public.document_review_queue to authenticated;

create view public.document_review_profiles
with (security_invoker = true) as
select
    p.id,
    p.full_name,
    p.email,
    coalesce(
        array_agg(r.role order by r.role) filter (where r.role is not null),
        '{}'::public.user_role[]
    ) as held_roles,
    pro.professional_type,
    pro.verification_status as professional_verification_status
from public.profiles p
left join public.profile_roles r on r.profile_id = p.id
left join public.professionals pro on pro.id = p.id
group by p.id, p.full_name, p.email, pro.professional_type, pro.verification_status;

revoke all on public.document_review_profiles from anon;
grant select on public.document_review_profiles to authenticated;
