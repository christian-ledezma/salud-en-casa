-- HU-19, after the author's walkthrough: the attention reuses the professional's
-- public profile and never surfaces a phone number, so my_attention stops
-- returning the counterpart's phone. Coordination moves to the in-app
-- conversation (HU-21); the contact detail an acceptance reveals is the
-- counterpart's identity, not a phone. docs/decisions.md, 2026-10-30.

set search_path = public, extensions;

-- The return type changes (a column is dropped), so the function is dropped and
-- recreated rather than replaced.
drop function public.my_attention(uuid);

create function public.my_attention(p_request_id uuid)
returns table (
    service_id uuid,
    request_id uuid,
    status public.service_status,
    final_amount_bob numeric,
    service_type_name text,
    created_at timestamptz,
    counterpart_id uuid,
    counterpart_name text,
    counterpart_photo_url text,
    i_am_patient boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        s.id,
        s.request_id,
        s.status,
        s.final_amount_bob,
        t.name as service_type_name,
        s.created_at,
        cp.id as counterpart_id,
        cp.full_name as counterpart_name,
        cp.photo_url as counterpart_photo_url,
        (s.patient_id = (select auth.uid())) as i_am_patient
    from public.services s
    join public.service_requests r on r.id = s.request_id
    join public.service_types t on t.id = r.service_type_id
    join public.profiles cp
        on cp.id = case
            when s.patient_id = (select auth.uid()) then s.professional_id
            else s.patient_id
        end
    where s.request_id = p_request_id
      and (s.patient_id = (select auth.uid()) or s.professional_id = (select auth.uid()));
$$;

revoke all on function public.my_attention(uuid) from public, anon;
grant execute on function public.my_attention(uuid) to authenticated;
