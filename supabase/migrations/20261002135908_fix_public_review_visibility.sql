-- Fix: reviews_select_visible could never show a review to a third party.
--
-- 20261002041901_close_dual_role_gaps narrowed public visibility to reviews the
-- recipient received as the professional of that service, which RF-12.4 asks
-- for. It expressed the condition as
-- "exists (select 1 from public.services s where s.id = reviews.service_id ...)".
--
-- That subquery runs under the policies of public.services, and
-- services_select_participants only shows a service to its own patient or
-- professional. For anyone else -- which is exactly who a public review is for
-- -- the subquery returns nothing, so no review was public at all. The fix of
-- the leak had closed the door entirely.
--
-- Found on 2026-10-02 running the RN-14 experiment of HT-16: the reputation
-- split came out right, 5.00 as a professional and 2.00 as a patient, and the
-- third party saw zero reviews where one was expected. The cause was isolated by
-- evaluating the three conditions of the policy separately as that third party:
-- it could see the recipient in professional_directory but not the services row.
-- professional_directory survives because it is security_invoker = false and so
-- runs with its owner's privileges; the subquery on services had no such
-- exemption.
--
-- See docs/decisions.md, 2026-10-02.

set search_path = public, extensions;

-- Same remedy as offer_continues_thread: the lookup moves into a security
-- definer function so it runs outside the policies of the table it reads.
--
-- Safe as a definer because it answers which side of a service somebody was on
-- and nothing else. It returns no row, no amount and no identifier, and the
-- caller already knows the service id and the recipient from the review they are
-- reading.
create or replace function public.was_the_professional_of(
    p_service_id uuid,
    p_profile_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.services s
        where s.id = p_service_id
          and s.professional_id = p_profile_id
    );
$$;

revoke all on function public.was_the_professional_of(uuid, uuid) from public, anon;

grant execute on function public.was_the_professional_of(uuid, uuid) to authenticated;

drop policy reviews_select_visible on public.reviews;

create policy reviews_select_visible on public.reviews
    for select to authenticated
    using (
        visible
        and public.was_the_professional_of(reviews.service_id, reviews.recipient_id)
        and exists (
            select 1 from public.professional_directory d where d.id = reviews.recipient_id
        )
    );
