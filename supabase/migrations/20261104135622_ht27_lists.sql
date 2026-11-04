-- HT-27: the patient reads their attentions too, and each list reads what its card
-- shows. docs/decisions.md, 2026-11-04, HT-27.

-- ---------------------------------------------------------------------------
-- 1. my_service_requests: what the list filters and orders by.
-- ---------------------------------------------------------------------------

-- Appended at the end, so create or replace keeps working. A request that reached
-- a service belongs to «Mis atenciones» whatever its status says, so a cancelled
-- attention will not show up in both views. The order lives here because PostgREST
-- orders by a column and not by an expression. The grants are set again because a
-- definer view that is not stripped of them can be written through
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
    r.status in ('PUBLISHED', 'NEGOTIATING') as is_open
from public.service_requests r
join public.service_types t on t.id = r.service_type_id
left join public.profiles p on p.id = r.professional_id
where r.patient_id = (select auth.uid());

revoke all on public.my_service_requests from anon, authenticated;
grant select on public.my_service_requests to authenticated;

-- ---------------------------------------------------------------------------
-- 2. my_attentions: the list of either side.
-- ---------------------------------------------------------------------------

-- Replaces my_attentions_as_professional. p_role only chooses which of the caller's
-- own rows to read, and grants nothing: both branches are pinned to auth.uid(), so
-- someone with both roles reads one side at a time (INV-17). A definer for the
-- reason HT-26 gave: the counterpart's name and the service type come from rows the
-- caller's own policies do not cover in one read. Only what the card shows leaves
-- the engine.
drop function public.my_attentions_as_professional(int, int);

create function public.my_attentions(
    p_role public.user_role,
    p_limit int default 20,
    p_offset int default 0
)
returns table (
    request_id uuid,
    status public.service_status,
    final_amount_bob numeric,
    service_type_name text,
    counterpart_name text,
    created_at timestamptz,
    scheduled_at timestamptz,
    started_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        s.request_id,
        s.status,
        s.final_amount_bob,
        t.name as service_type_name,
        c.full_name as counterpart_name,
        s.created_at,
        r.scheduled_at,
        s.started_at
    from public.services s
    join public.service_requests r on r.id = s.request_id
    join public.service_types t on t.id = r.service_type_id
    join public.profiles c
        on c.id = case when p_role = 'PATIENT' then s.professional_id else s.patient_id end
    where (p_role = 'PATIENT' and s.patient_id = (select auth.uid()))
       or (p_role = 'PROFESSIONAL' and s.professional_id = (select auth.uid()))
    order by s.created_at desc, s.id desc
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

revoke all on function public.my_attentions(public.user_role, int, int) from public, anon;
grant execute on function public.my_attentions(public.user_role, int, int) to authenticated;

-- ---------------------------------------------------------------------------
-- 3. my_active_attention: the one Inicio opens.
-- ---------------------------------------------------------------------------

-- The one running and, if none is, the next one assigned. The same row as the
-- list, because Inicio shows the same facts as its card.
create function public.my_active_attention(p_role public.user_role)
returns table (
    request_id uuid,
    status public.service_status,
    final_amount_bob numeric,
    service_type_name text,
    counterpart_name text,
    created_at timestamptz,
    scheduled_at timestamptz,
    started_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        s.request_id,
        s.status,
        s.final_amount_bob,
        t.name as service_type_name,
        c.full_name as counterpart_name,
        s.created_at,
        r.scheduled_at,
        s.started_at
    from public.services s
    join public.service_requests r on r.id = s.request_id
    join public.service_types t on t.id = r.service_type_id
    join public.profiles c
        on c.id = case when p_role = 'PATIENT' then s.professional_id else s.patient_id end
    where s.status in ('ASSIGNED', 'IN_PROGRESS')
      and (
          (p_role = 'PATIENT' and s.patient_id = (select auth.uid()))
          or (p_role = 'PROFESSIONAL' and s.professional_id = (select auth.uid()))
      )
    order by s.status = 'IN_PROGRESS' desc, coalesce(r.scheduled_at, s.created_at), s.id
    limit 1;
$$;

revoke all on function public.my_active_attention(public.user_role) from public, anon;
grant execute on function public.my_active_attention(public.user_role) to authenticated;
