-- HT-28: the card of «Mis solicitudes» says how many offers its request has.
-- docs/decisions.md, 2026-11-04, HT-28.

-- An open negotiation is a chain whose last offer is still PROPOSED and unanswered,
-- which is what the patient's offers screen stacks, so both say the same number. The
-- counter sits at the end, so create or replace keeps working, and the grants are set
-- again because a definer view that is not stripped of them can be written through
-- (docs/decisions.md, 2026-10-11).
create or replace view public.my_service_requests
with (security_invoker = false) as
select
    r.id,
    r.service_type_id,
    t.name as service_type_name,
    r.address_text,
    r.city,
    r.reference,
    extensions.st_y(r.location::extensions.geometry) as latitude,
    extensions.st_x(r.location::extensions.geometry) as longitude,
    r.modality,
    r.status,
    r.description,
    r.suggested_budget_bob,
    r.expires_at,
    r.created_at,
    r.scheduled_at,
    p.full_name as professional_name,
    r.professional_id,
    exists (select 1 from public.services s where s.request_id = r.id) as has_attention,
    r.status in ('PUBLISHED', 'NEGOTIATING') as is_open,
    (
        select count(*)::int
        from public.request_offers o
        where o.request_id = r.id
          and o.status = 'PROPOSED'
          and not exists (
              select 1 from public.request_offers answer where answer.parent_offer_id = o.id
          )
    ) as open_offer_count
from public.service_requests r
join public.service_types t on t.id = r.service_type_id
left join public.profiles p on p.id = r.professional_id
where r.patient_id = (select auth.uid());

revoke all on public.my_service_requests from anon, authenticated;
grant select on public.my_service_requests to authenticated;
