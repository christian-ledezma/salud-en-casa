-- HU-16: a patient schedules an appointment with a specific professional, at a
-- date and time inside that professional's declared availability (RF-07.3),
-- and the professional sees it in their inbox marked as scheduled.
--
-- service_requests already carries professional_id and scheduled_at, and
-- scheduled_request_is_complete already requires both for a SCHEDULED row. This
-- migration adds:
--   a) bookable_start_times, the one place that decides which start times a
--      professional offers for a service;
--   b) create_scheduled_request, the only path a scheduled request is born
--      through, which validates against (a);
--   c) the end of direct client inserts and of post-creation rewrites of the
--      request's terms;
--   d) scheduled_at and the professional's name in my_service_requests;
--   e) request_inbox, which replaces immediate_inbox and adds the scheduled
--      requests addressed to the caller;
--   f) the inbox broadcast and the expiry sweep, both extended to SCHEDULED.
--
-- See docs/decisions.md, 2026-10-15, HU-16.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- a) bookable_start_times: the rule, written once
-- ---------------------------------------------------------------------------

-- availability_slots holds wall-clock times with no zone, declared by a
-- professional who works in Bolivia, so every comparison happens in
-- America/La_Paz and never in the session's zone. A start is offered when:
--   * it is at least two hours and at most fourteen days away;
--   * it sits on the slot's own grid, every 30 minutes from its start_time;
--   * the whole appointment, start plus the service's estimated duration, fits
--     inside that one slot. Adjacent slots are not merged.
-- Requests already made for the same time are not subtracted: a request is not
-- a reservation, and the clash is resolved when an offer is accepted (HU-19).
--
-- security definer because the gates it folds in must hold for whoever calls:
-- an approved and active professional (INV-07), a service they declare, and a
-- caller who is not that professional (INV-16). It reads slots and service
-- declarations that availability_slots_select_public and
-- professional_services_select_public already publish, so it reveals nothing
-- those policies keep back. The result is bounded by construction: at most
-- fifteen calendar days of 30-minute starts.
create or replace function public.bookable_start_times(
    p_professional_id uuid,
    p_service_type_id uuid
)
returns table (
    start_at timestamptz,
    local_date date,
    local_time time
)
language sql
stable
security definer
set search_path = ''
as $$
    with offering as (
        select t.estimated_duration_min
        from public.service_types t
        join public.professional_services ps
            on ps.service_type_id = t.id
           and ps.professional_id = p_professional_id
           and ps.active
        where t.id = p_service_type_id
          and t.active
          and p_professional_id <> (select auth.uid())
          and exists (
              select 1
              from public.professional_directory d
              where d.id = p_professional_id
          )
    ),
    bounds as (
        select
            now() + interval '2 hours' as earliest,
            now() + interval '14 days' as latest
    ),
    days as (
        select day::date as day
        from bounds b,
            generate_series(
                (b.earliest at time zone 'America/La_Paz')::date,
                (b.latest at time zone 'America/La_Paz')::date,
                interval '1 day'
            ) as day
    ),
    candidates as (
        -- date + time is a timestamp without zone: the wall clock in La Paz.
        -- The offsets run on minutes from the slot's start, never on time
        -- arithmetic, so nothing can wrap past midnight.
        select (d.day + s.start_time) + make_interval(mins => m) as local_start
        from offering o
        cross join days d
        join public.availability_slots s
            on s.professional_id = p_professional_id
           and s.day_of_week = extract(isodow from d.day)::smallint
        cross join lateral generate_series(
            0,
            (extract(epoch from (s.end_time - s.start_time)) / 60)::int - o.estimated_duration_min,
            30
        ) as m
    )
    -- distinct because reject_overlapping_slot takes no lock: two concurrent
    -- inserts can both pass it, and two overlapping slots would list the same
    -- start twice.
    select distinct
        c.local_start at time zone 'America/La_Paz' as start_at,
        c.local_start::date as local_date,
        c.local_start::time as local_time
    from candidates c, bounds b
    where c.local_start at time zone 'America/La_Paz' between b.earliest and b.latest
    order by 1;
$$;

revoke all on function public.bookable_start_times(uuid, uuid) from public, anon;
grant execute on function public.bookable_start_times(uuid, uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- b) create_scheduled_request: the only path a scheduled request is born through
-- ---------------------------------------------------------------------------

-- The sibling of create_immediate_request: the snapshot comes from an address
-- the caller owns (INV-03), status and expires_at are set here and not by the
-- client. Each refusal is a bare label the client matches, none a substring of
-- another. The time is accepted only if bookable_start_times lists it, so the
-- screen and this check cannot disagree about the rule.
--
-- The budget arrives as text for the reason create_immediate_request gives.
create or replace function public.create_scheduled_request(
    p_professional_id uuid,
    p_address_id uuid,
    p_service_type_id uuid,
    p_scheduled_at timestamptz,
    p_description text,
    p_suggested_budget_bob text
)
returns uuid
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_location extensions.geography;
    v_address_text text;
    v_city text;
    v_reference text;
    v_request_id uuid;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    if p_description is not null and char_length(p_description) > 1000 then
        raise exception 'description_too_long';
    end if;

    -- INV-16: nobody takes part in the same care as patient and professional.
    if p_professional_id = v_caller then
        raise exception 'cannot_schedule_with_yourself';
    end if;

    select a.location, a.address_text, a.city, a.reference
    into v_location, v_address_text, v_city, v_reference
    from public.addresses a
    where a.id = p_address_id
      and a.profile_id = v_caller;

    if not found then
        raise exception 'address_not_owned';
    end if;

    -- INV-07: only an approved and active professional can be addressed.
    if not exists (
        select 1
        from public.professional_directory d
        where d.id = p_professional_id
    ) then
        raise exception 'professional_unavailable';
    end if;

    if not exists (
        select 1
        from public.service_types t
        where t.id = p_service_type_id
          and t.active
    ) then
        raise exception 'service_type_unavailable';
    end if;

    if not exists (
        select 1
        from public.professional_services ps
        where ps.professional_id = p_professional_id
          and ps.service_type_id = p_service_type_id
          and ps.active
    ) then
        raise exception 'service_not_offered';
    end if;

    -- RN-02: the address must be inside the professional's coverage, or the
    -- professional could never offer on it (request_offers_insert_participants
    -- asks professional_covers). Decided on the base snapped to the grid the
    -- map already publishes, never on the exact one: the caller chooses the
    -- address and can place it anywhere, which is the bisection channel
    -- docs/decisions.md, 2026-10-11, closed for the nearby search.
    if not exists (
        select 1
        from public.professionals pro
        join public.addresses base
            on base.profile_id = pro.id
           and base.is_professional_base
        where pro.id = p_professional_id
          and extensions.st_dwithin(
              extensions.st_setsrid(
                  extensions.st_makepoint(
                      round(extensions.st_x(base.location::extensions.geometry)::numeric, 3)::double precision,
                      round(extensions.st_y(base.location::extensions.geometry)::numeric, 3)::double precision
                  ),
                  4326
              )::extensions.geography,
              v_location,
              pro.coverage_radius_km * 1000
          )
    ) then
        raise exception 'outside_coverage';
    end if;

    if p_scheduled_at is null or not exists (
        select 1
        from public.bookable_start_times(p_professional_id, p_service_type_id) b
        where b.start_at = p_scheduled_at
    ) then
        raise exception 'time_not_bookable';
    end if;

    insert into public.service_requests (
        patient_id,
        professional_id,
        service_type_id,
        address_id,
        location,
        address_text,
        city,
        reference,
        modality,
        scheduled_at,
        description,
        suggested_budget_bob,
        expires_at
    ) values (
        v_caller,
        p_professional_id,
        p_service_type_id,
        p_address_id,
        v_location,
        v_address_text,
        v_city,
        v_reference,
        'SCHEDULED',
        p_scheduled_at,
        p_description,
        nullif(p_suggested_budget_bob, '')::numeric,
        -- An appointment nobody agreed to before its time is of no use to anyone.
        p_scheduled_at
    )
    returning id into v_request_id;

    return v_request_id;
end;
$$;

revoke all on function public.create_scheduled_request(uuid, uuid, uuid, timestamptz, text, text) from public, anon;
grant execute on function public.create_scheduled_request(uuid, uuid, uuid, timestamptz, text, text) to authenticated;

-- ---------------------------------------------------------------------------
-- c) No request is written directly by the client, except to cancel it
-- ---------------------------------------------------------------------------

-- Both modalities now have their creation function, so the direct insert goes.
-- With no insert policy left, row level security refuses every client insert;
-- the definer functions are unaffected. The table keeps its select and update
-- policies, so it is still covered (INV-02).
drop policy service_requests_insert_own on public.service_requests;

-- service_requests_update_own let the patient rewrite any column the two guard
-- triggers do not watch: scheduled_at, expires_at, modality, service_type_id.
-- Creating through the function and then moving scheduled_at would skip every
-- check above. The client only ever writes status, to cancel (RF-07.6), and
-- guard_request_lifecycle already narrows that to PUBLISHED or NEGOTIATING to
-- CANCELLED. Same remedy as messages in 20260912090000.
revoke update on public.service_requests from authenticated;
grant update (status) on public.service_requests to authenticated;

-- ---------------------------------------------------------------------------
-- d) my_service_requests: the patient sees when and with whom
-- ---------------------------------------------------------------------------

-- Columns are appended, so create or replace keeps working. The professional's
-- name comes from profiles past its policies, which is what a definer view is
-- for; the WHERE still pins every row to the caller.
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
    p.full_name as professional_name
from public.service_requests r
join public.service_types t on t.id = r.service_type_id
left join public.profiles p on p.id = r.professional_id
where r.patient_id = (select auth.uid());

revoke all on public.my_service_requests from anon, authenticated;
grant select on public.my_service_requests to authenticated;

-- ---------------------------------------------------------------------------
-- e) request_inbox: one inbox, both modalities
-- ---------------------------------------------------------------------------

-- The return type grows by two columns, which create or replace cannot do, and
-- the name no longer describes what it returns, so immediate_inbox is dropped
-- and request_inbox takes its place.
--
-- The immediate branch is the old function unchanged. The scheduled branch is
-- chosen by address, not by coverage: the request names its professional. It
-- keeps the same projection, so the exact location, the address text and the
-- patient's identity still never leave the function (RF-08.6), and the same
-- time guard, scheduled_at standing in for expires_at.
drop function public.immediate_inbox(int, int);

create function public.request_inbox(
    p_limit int default 20,
    p_offset int default 0
)
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
    scheduled_at timestamptz
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
        r.scheduled_at
    from public.service_requests r
    join public.service_types t on t.id = r.service_type_id
    join public.addresses base
        on base.profile_id = (select auth.uid())
       and base.is_professional_base
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
    order by r.created_at desc, r.id asc
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

revoke all on function public.request_inbox(int, int) from public, anon;
grant execute on function public.request_inbox(int, int) to authenticated;

-- ---------------------------------------------------------------------------
-- f) The broadcast and the sweep cover scheduled requests too
-- ---------------------------------------------------------------------------

-- The modality predicate goes: a scheduled insert or status change must reach
-- the addressed professional live. The payload stays empty, so pinging every
-- approved professional costs each a re-query and tells none of them anything.
create or replace trigger service_requests_notify_inbox_insert
    after insert on public.service_requests
    for each row
    execute function public.notify_immediate_inbox();

create or replace trigger service_requests_notify_inbox_update
    after update on public.service_requests
    for each row
    when (old.status is distinct from new.status)
    execute function public.notify_immediate_inbox();

-- cron.schedule with an existing job name replaces its command. A scheduled
-- request expires once its time passes unaccepted, offers or not: unlike an
-- immediate one, a negotiation that missed the appointment has nothing left
-- to agree on.
select cron.schedule(
    'expire_immediate_requests',
    '* * * * *',
    $$
    update public.service_requests
       set status = 'EXPIRED'
     where (
             modality = 'IMMEDIATE'
             and status = 'PUBLISHED'
             and expires_at < now()
             and not exists (
                 select 1
                 from public.request_offers o
                 where o.request_id = service_requests.id
             )
           )
        or (
             modality = 'SCHEDULED'
             and status in ('PUBLISHED', 'NEGOTIATING')
             and scheduled_at < now()
           )
    $$
);
