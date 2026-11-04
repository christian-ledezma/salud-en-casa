-- HT-26: the attentions a professional agreed, as a list they can come back to.
-- docs/decisions.md, 2026-11-03, HT-26.
--
-- A definer because the patient's name and the service type come from rows the
-- professional's own policies do not cover in one read, the same reason as
-- my_attention. Pinned to professional_id = auth.uid(): someone with both roles
-- reads here only what they attend, and what they receive stays in their own
-- list (INV-17). Only what the card shows leaves the engine.
create function public.my_attentions_as_professional(
    p_limit int default 20,
    p_offset int default 0
)
returns table (
    request_id uuid,
    status public.service_status,
    final_amount_bob numeric,
    service_type_name text,
    patient_name text,
    created_at timestamptz,
    scheduled_at timestamptz
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
        p.full_name as patient_name,
        s.created_at,
        r.scheduled_at
    from public.services s
    join public.service_requests r on r.id = s.request_id
    join public.service_types t on t.id = r.service_type_id
    join public.profiles p on p.id = s.patient_id
    where s.professional_id = (select auth.uid())
    order by s.created_at desc, s.id desc
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

revoke all on function public.my_attentions_as_professional(int, int) from public, anon;
grant execute on function public.my_attentions_as_professional(int, int) to authenticated;
