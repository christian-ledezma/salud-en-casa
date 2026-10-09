-- HU-09: the administrator reviews verification documents and promotes a
-- professional. See docs/decisions.md, 2026-10-06.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. ADMIN is exclusive
-- ---------------------------------------------------------------------------

-- An administrator holds no other role, so there is nobody to review but
-- someone else. Not retroactive: it acts on insert, and nobody holds ADMIN
-- together with another role today. Definer so the check sees every role of the
-- person whatever the caller's own row level security lets them read.
create or replace function public.guard_admin_role_exclusivity()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.role = 'ADMIN' then
        if exists (
            select 1 from public.profile_roles
            where profile_id = new.profile_id and role <> 'ADMIN'
        ) then
            raise exception 'admin_role_is_exclusive';
        end if;
    elsif exists (
        select 1 from public.profile_roles
        where profile_id = new.profile_id and role = 'ADMIN'
    ) then
        raise exception 'admin_role_is_exclusive';
    end if;
    return new;
end;
$$;

create trigger profile_roles_guard_admin_exclusivity
    before insert on public.profile_roles
    for each row execute function public.guard_admin_role_exclusivity();

-- ---------------------------------------------------------------------------
-- 2. The documents a person must have approved
-- ---------------------------------------------------------------------------

-- Mirrors VerificationChecklist.requiredFor in Kotlin; the arrays are literals
-- so RequiredDocumentsParityTest can read them from this file. A professional
-- without a declared type has no branch of its own: professionals_approved_
-- profile_is_complete already keeps that person from being APPROVED.
--
-- No grant to authenticated: it would tell anyone whether a given uuid holds the
-- professional role and of what type. Only the two definer functions below call it.
create or replace function public.required_document_types(p_profile_id uuid)
returns public.document_type[]
language sql
stable
security definer
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

revoke all on function public.required_document_types(uuid) from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- 3. The only way to APPROVED
-- ---------------------------------------------------------------------------

-- professionals has no update policy for administrators and gets none: this
-- function is the single door, and it is where completeness is decided.
-- Definer skips row level security, so the first line is what keeps any
-- signed-in user from promoting themselves.
create or replace function public.approve_professional_verification(p_profile_id uuid)
returns public.review_status
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

    if not exists (
        select 1 from public.profile_roles
        where profile_id = p_profile_id and role = 'PROFESSIONAL'
    ) then
        raise exception 'profile_is_not_a_professional';
    end if;

    select professional_type, base_rate_bob into v_type, v_rate
    from public.professionals
    where id = p_profile_id;

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

    return 'APPROVED'::public.review_status;
end;
$$;

revoke all on function public.approve_professional_verification(uuid) from public, anon;
grant execute on function public.approve_professional_verification(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 4. The engine signs the verdict
-- ---------------------------------------------------------------------------

-- The client sends status, and the reason when rejecting. Who reviewed and when
-- come from here, so neither can be forged nor taken from the phone's clock, and
-- approving a document that was once rejected clears its reason instead of
-- tripping rejection_reason_matches_status.
create or replace function public.stamp_document_review()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
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
    before update of status on public.verification_documents
    for each row
    when (old.status is distinct from new.status and public.is_admin())
    execute function public.stamp_document_review();

-- ---------------------------------------------------------------------------
-- 5. A required document that stops being approved takes the professional down
-- ---------------------------------------------------------------------------

-- Without this, rejecting a degree after the promotion would leave the person in
-- search results, which INV-07 forbids. Definer because professionals has no
-- update policy for administrators. The optional OTHER attachment is not
-- required, so it never demotes anyone.
create or replace function public.sync_professional_verification()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.document_type = any (public.required_document_types(new.profile_id)) then
        update public.professionals
        set verification_status = 'PENDING'
        where id = new.profile_id
          and verification_status = 'APPROVED';
    end if;
    return null;
end;
$$;

create trigger verification_documents_sync_professional_verification
    after update of status on public.verification_documents
    for each row
    when (old.status = 'APPROVED' and new.status is distinct from 'APPROVED')
    execute function public.sync_professional_verification();

-- ---------------------------------------------------------------------------
-- 6. A rejection reason the owner can read
-- ---------------------------------------------------------------------------

-- rejection_reason_matches_status only asks that it not be null, so an empty
-- string passed. 300 mirrors the RejectionReason value object.
alter table public.verification_documents
    add constraint verification_documents_rejection_reason_length
    check (rejection_reason is null or char_length(rejection_reason) between 1 and 300);

-- ---------------------------------------------------------------------------
-- 7. Read models for the review screens
-- ---------------------------------------------------------------------------

-- security_invoker is what keeps INV-13 here: the rows come from
-- verification_documents_select_admin, profiles_select_admin and the like, so
-- anyone who is not an administrator sees only their own. With
-- security_invoker = false these views would expose everybody's documents.
-- The same trap as my_roles: profiles is ambiguous from verification_documents
-- (two foreign keys), which is why this is a view and not an embed.
create view public.document_review_queue
with (security_invoker = true) as
select
    d.id,
    d.profile_id,
    d.document_type,
    d.caption,
    d.storage_path,
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
    p.photo_url,
    coalesce(
        array_agg(r.role order by r.role) filter (where r.role is not null),
        '{}'::public.user_role[]
    ) as held_roles,
    pro.professional_type,
    pro.verification_status as professional_verification_status
from public.profiles p
left join public.profile_roles r on r.profile_id = p.id
left join public.professionals pro on pro.id = p.id
group by p.id, p.full_name, p.email, p.photo_url, pro.professional_type, pro.verification_status;

revoke all on public.document_review_profiles from anon;
grant select on public.document_review_profiles to authenticated;
