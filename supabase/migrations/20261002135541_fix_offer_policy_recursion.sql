-- Fix: request_offers_insert_participants recursed on its own table.
--
-- The policy's patient branch validated the counter-offer thread with
-- "exists (select 1 from public.request_offers parent ...)". A policy expression
-- on a relation that selects from that same relation makes PostgreSQL raise
-- 42P17, "infinite recursion detected in policy for relation request_offers",
-- for every insert -- a professional's first offer as much as a patient's
-- counter-offer. The check never got as far as evaluating anything.
--
-- The defect arrived with 20260912110000_close_lifecycle_and_visibility_gaps,
-- which added the thread validation, and survived unnoticed because nothing has
-- ever inserted an offer: issuing one is HU-17, in Sprint 6. It surfaced while
-- running the RN-13 experiment of HT-16, where both the self-dealing insert and
-- the legitimate one were refused with the same SQLSTATE -- which is what gave
-- it away, because a policy refusal and a recursion error are not the same
-- outcome and only one of them was expected.
--
-- Found on 2026-10-02. See docs/decisions.md of the same date.

set search_path = public, extensions;

-- The standard remedy for an RLS self-reference: the lookup moves into a
-- security definer function, so it runs outside the policies of the table it
-- reads and the recursion cannot arise.
--
-- Being a definer function is safe here because it answers a boolean about a
-- thread the caller is already proven to belong to: the policy checks
-- r.patient_id = auth.uid() separately, and this function reveals nothing but
-- whether the parent offer belongs to the same request and professional. It
-- never returns a row, an amount or an identifier.
create or replace function public.offer_continues_thread(
    p_parent_offer_id uuid,
    p_request_id uuid,
    p_professional_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.request_offers parent
        where parent.id = p_parent_offer_id
          and parent.request_id = p_request_id
          and parent.professional_id = p_professional_id
          and parent.issuer = 'PROFESSIONAL'
    );
$$;

revoke all on function public.offer_continues_thread(uuid, uuid, uuid) from public, anon;

grant execute on function public.offer_continues_thread(uuid, uuid, uuid) to authenticated;

drop policy request_offers_insert_participants on public.request_offers;

create policy request_offers_insert_participants on public.request_offers
    for insert to authenticated
    with check (
        exists (
            select 1
            from public.service_requests r
            where r.id = request_offers.request_id
              and r.status in ('PUBLISHED', 'NEGOTIATING')
              and (
                  (
                      request_offers.issuer = 'PROFESSIONAL'
                      and request_offers.professional_id = (select auth.uid())
                      -- RN-13: never an offer on one's own request.
                      and r.patient_id <> (select auth.uid())
                      and exists (
                          select 1
                          from public.professional_directory d
                          where d.id = (select auth.uid())
                      )
                      and public.professional_covers(r.location, r.service_type_id)
                  )
                  or (
                      request_offers.issuer = 'PATIENT'
                      and r.patient_id = (select auth.uid())
                      and request_offers.parent_offer_id is not null
                      and public.offer_continues_thread(
                          request_offers.parent_offer_id,
                          request_offers.request_id,
                          request_offers.professional_id
                      )
                  )
              )
        )
    );
