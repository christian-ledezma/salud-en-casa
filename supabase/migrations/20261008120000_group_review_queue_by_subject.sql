-- The review queue lists people, not documents. See docs/decisions.md,
-- 2026-10-08.

set search_path = public, extensions;

-- One row per document made the list as long as the pile of paperwork: five
-- cards for a single professional. Grouping in the client is not an option,
-- because the count has to be right on the first page and a person's documents
-- can straddle a page boundary. So the engine groups, counts and sorts, and the
-- page is a page of people.
drop view public.document_review_queue;

create view public.document_review_subject_queue
with (security_invoker = true) as
select
    d.profile_id,
    p.full_name,
    p.email,
    count(*)::integer as pending_count,
    min(d.created_at) as oldest_pending_at,
    -- One column instead of two so the search is a single ilike and never a
    -- logical expression: a term carrying a dot, which every email search does,
    -- would otherwise have to survive being quoted inside an or=() group.
    p.full_name || ' ' || p.email as search_text,
    -- Not shown on the card; it is what the role filter matches. The correlated
    -- form keeps the count honest: joining profile_roles would multiply the
    -- document rows by the roles of the person.
    (
        select coalesce(array_agg(r.role order by r.role), '{}'::public.user_role[])
        from public.profile_roles r
        where r.profile_id = d.profile_id
    ) as held_roles
from public.verification_documents d
join public.profiles p on p.id = d.profile_id
where d.status = 'PENDING'
group by d.profile_id, p.full_name, p.email;

revoke all on public.document_review_subject_queue from anon;
grant select on public.document_review_subject_queue to authenticated;
