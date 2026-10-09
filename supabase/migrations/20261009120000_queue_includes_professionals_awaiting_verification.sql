-- The queue lists the administrator's pending work, and verifying a
-- professional whose paperwork is already in order is pending work.
-- See docs/decisions.md, 2026-10-09.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. Who is waiting for the verifying act
-- ---------------------------------------------------------------------------

-- The conditions mirror approve_professional_verification, including the type
-- and the rate: a card that promised work the engine refuses with
-- professional_profile_incomplete would send the administrator to a dead end,
-- and that person is waiting on their own profile, not on a review.
--
-- Definer because required_document_types has no grant to authenticated, which
-- was deliberate: executing it tells the caller whether a given uuid holds the
-- professional role and of what type. The is_admin() filter is what keeps that
-- from leaking through this door, and it is a filter and not a raise so the
-- view below still works for anyone reading their own pending documents.
create or replace function public.professionals_awaiting_verification()
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
    select pro.id
    from public.professionals pro
    where public.is_admin()
      and pro.verification_status <> 'APPROVED'
      and pro.professional_type is not null
      and pro.base_rate_bob is not null
      and not exists (
          select 1
          from unnest(public.required_document_types(pro.id)) as required(document_type)
          where not exists (
              select 1 from public.verification_documents d
              where d.profile_id = pro.id
                and d.document_type = required.document_type
                and d.status = 'APPROVED'
          )
      );
$$;

revoke all on function public.professionals_awaiting_verification() from public, anon;
grant execute on function public.professionals_awaiting_verification() to authenticated;

-- ---------------------------------------------------------------------------
-- 2. The queue, now of work and not of documents
-- ---------------------------------------------------------------------------

-- A person can be in both sets at once: every required document approved and
-- the optional attachment still pending. The union of identifiers is what keeps
-- that person a single card, because two rows with the same key crash the list.
--
-- waiting_since replaces oldest_pending_at: for a document it is when it was
-- uploaded, and for the verifying act it is when the last required document was
-- approved, which is when the turn became the administrator's. One column with
-- two meanings is how a read model rots, so the name no longer says pending.
drop view public.document_review_subject_queue;

create view public.document_review_subject_queue
with (security_invoker = true) as
with pending as (
    select
        d.profile_id,
        count(*)::integer as pending_count,
        min(d.created_at) as oldest_pending_at
    from public.verification_documents d
    where d.status = 'PENDING'
    group by d.profile_id
),
awaiting as (
    select
        a.profile_id,
        (
            select max(d.reviewed_at)
            from public.verification_documents d
            where d.profile_id = a.profile_id and d.status = 'APPROVED'
        ) as approved_at
    from public.professionals_awaiting_verification() as a(profile_id)
),
subjects as (
    select profile_id from pending
    union
    select profile_id from awaiting
)
select
    s.profile_id,
    p.full_name,
    p.email,
    coalesce(w.pending_count, 0) as pending_count,
    a.profile_id is not null as awaiting_verification,
    least(w.oldest_pending_at, a.approved_at) as waiting_since,
    -- One column instead of two so the search is a single ilike and never a
    -- logical expression: a term carrying a dot, which every email search does,
    -- would otherwise have to survive being quoted inside an or=() group.
    p.full_name || ' ' || p.email as search_text,
    -- Not shown on the card; it is what the role filter matches.
    (
        select coalesce(array_agg(r.role order by r.role), '{}'::public.user_role[])
        from public.profile_roles r
        where r.profile_id = s.profile_id
    ) as held_roles
from subjects s
join public.profiles p on p.id = s.profile_id
left join pending w on w.profile_id = s.profile_id
left join awaiting a on a.profile_id = s.profile_id;

revoke all on public.document_review_subject_queue from anon;
grant select on public.document_review_subject_queue to authenticated;
