-- HU-08. Two gaps on the verification path, closed in one migration.
--
-- 1. The owner could insert a professionals row already APPROVED. The insert
--    policy checked the role but not the status, and the guard trigger runs
--    only on update. A professional now starts PENDING (RF-04.4, INV-07).
--
-- 2. A rejected document could not be resubmitted. The owner may move a
--    REJECTED row back to PENDING. The trigger lets that one transition
--    through, and the policy forces the review fields clear on the same write,
--    so the owner can never record a decision of their own.

drop policy professionals_insert_own on public.professionals;

create policy professionals_insert_own on public.professionals
    for insert to authenticated
    with check (
        id = (select auth.uid())
        and verification_status = 'PENDING'
        and exists (
            select 1
            from public.profile_roles
            where profile_id = (select auth.uid())
              and role = 'PROFESSIONAL'
        )
    );

drop policy verification_documents_update_own on public.verification_documents;

create policy verification_documents_update_own on public.verification_documents
    for update to authenticated
    using (
        profile_id = (select auth.uid())
        and status in ('PENDING', 'REJECTED')
    )
    with check (
        profile_id = (select auth.uid())
        and status = 'PENDING'
        and reviewed_by is null
        and reviewed_at is null
        and rejection_reason is null
    );

create or replace function public.guard_document_review()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if old.status = 'REJECTED' and new.status = 'PENDING' then
        return new;
    end if;

    if new.status is distinct from old.status
        or new.reviewed_by is distinct from old.reviewed_by
        or new.rejection_reason is distinct from old.rejection_reason then
        raise exception 'document_review_is_written_by_an_administrator';
    end if;
    return new;
end;
$$;
