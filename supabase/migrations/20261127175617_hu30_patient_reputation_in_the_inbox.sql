-- HU-30: the professional rates the patient of a completed attention, and the
-- professionals who receive that patient's next request see their reputation. The
-- insert policy, the uniqueness and the recalculation of patients.average_rating
-- already work for the professional's side; what is missing is showing the result
-- where a professional decides whether to go to a home.
-- docs/decisions.md, 2026-11-27, HU-30.

set search_path = public, extensions;

-- The return types change, so both functions are dropped and created again. The
-- bodies are the ones of 20261125021500 (request_inbox) and 20261023130000
-- (inbox_request) with the patient's reputation appended. Neither returns who the
-- patient is: a count and an average say how they have been rated, never by whom
-- or about what (INV-13, INV-17).
drop function public.request_inbox(integer, integer);

create function public.request_inbox(p_limit integer default 20, p_offset integer default 0)
returns table (
    id uuid,
    service_type_name text,
    city text,
    description text,
    suggested_budget_bob numeric,
    distance_m double precision,
    status public.request_status,
    created_at timestamptz,
    modality public.request_modality,
    scheduled_at timestamptz,
    patient_rating numeric,
    patient_total_reviews integer
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        r.id,
        t.name as service_type_name,
        r.city,
        r.description,
        r.suggested_budget_bob,
        round(
            extensions.st_distance(base.location, r.location)::numeric,
            -2
        )::double precision as distance_m,
        r.status,
        r.created_at,
        r.modality,
        r.scheduled_at,
        pat.average_rating,
        pat.total_reviews
    from public.service_requests r
    join public.service_types t on t.id = r.service_type_id
    join public.addresses base
        on base.profile_id = (select auth.uid())
       and base.is_professional_base
    left join public.patients pat on pat.id = r.patient_id
    where r.status in ('PUBLISHED', 'NEGOTIATING')
      and r.patient_id <> (select auth.uid())
      and (
          (
              r.modality = 'IMMEDIATE'
              and (r.expires_at is null or r.expires_at > now())
              and public.professional_covers(r.location, r.service_type_id)
          )
          or (
              r.modality = 'SCHEDULED'
              and r.professional_id = (select auth.uid())
              and r.scheduled_at > now()
              and exists (
                  select 1
                  from public.professional_directory d
                  where d.id = (select auth.uid())
              )
          )
      )
      and (
          public.professional_has_valid_subscription((select auth.uid()))
          or exists (
              select 1
              from public.request_offers o
              where o.request_id = r.id
                and o.professional_id = (select auth.uid())
          )
      )
    order by r.created_at desc, r.id asc
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

revoke all on function public.request_inbox(integer, integer) from public, anon;
grant execute on function public.request_inbox(integer, integer) to authenticated;

drop function public.inbox_request(uuid);

create function public.inbox_request(p_request_id uuid)
returns table (
    id uuid,
    service_type_name text,
    city text,
    description text,
    suggested_budget_bob numeric,
    distance_m double precision,
    status public.request_status,
    created_at timestamptz,
    modality public.request_modality,
    scheduled_at timestamptz,
    already_offered boolean,
    patient_rating numeric,
    patient_total_reviews integer
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        r.id,
        t.name as service_type_name,
        r.city,
        r.description,
        r.suggested_budget_bob,
        round(
            extensions.st_distance(base.location, r.location)::numeric,
            -2
        )::double precision as distance_m,
        r.status,
        r.created_at,
        r.modality,
        r.scheduled_at,
        exists (
            select 1
            from public.request_offers o
            where o.request_id = r.id
              and o.professional_id = (select auth.uid())
        ) as already_offered,
        pat.average_rating,
        pat.total_reviews
    from public.service_requests r
    join public.service_types t on t.id = r.service_type_id
    join public.addresses base
        on base.profile_id = (select auth.uid())
       and base.is_professional_base
    left join public.patients pat on pat.id = r.patient_id
    where r.id = p_request_id
      and (
          public.professional_may_offer_on(p_request_id)
          -- Still openable after acceptance if they already offered on it.
          or exists (
              select 1
              from public.request_offers o
              where o.request_id = r.id
                and o.professional_id = (select auth.uid())
          )
      );
$$;

revoke all on function public.inbox_request(uuid) from public, anon;
grant execute on function public.inbox_request(uuid) to authenticated;
