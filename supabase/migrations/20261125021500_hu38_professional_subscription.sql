-- HU-38: a professional appears before patients while their subscription is
-- valid. A switch in platform_settings turns the requirement on; off, every
-- approved and active professional is valid. Approval grants a 30-day trial.
-- Without a valid subscription a professional leaves the search, the inbox and
-- the nearby push, cannot be booked and cannot make a first offer; an open
-- negotiation and an agreed attention go on. docs/decisions.md, 2026-11-24, HU-38.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. The switch and the length of the trial.
-- ---------------------------------------------------------------------------

alter table public.platform_settings
    add column subscription_required boolean not null default false,
    add column trial_days integer not null default 30
        constraint trial_days_is_positive check (trial_days > 0);

-- ---------------------------------------------------------------------------
-- 2. The periods. Only the engine writes them (INV-18): no write policy, and
--    no write privilege for a client. PLAY_BILLING joins in Sprint 11.
-- ---------------------------------------------------------------------------

create type public.subscription_source as enum ('TRIAL');

create table public.subscription_periods (
    id uuid primary key default gen_random_uuid(),
    professional_id uuid not null references public.professionals(id) on delete cascade,
    source public.subscription_source not null,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    created_at timestamptz not null default now(),
    constraint period_ends_after_it_starts check (ends_at > starts_at)
);

create unique index idx_subscription_periods_one_trial
    on public.subscription_periods (professional_id)
    where source = 'TRIAL';

create index idx_subscription_periods_professional_ends_at
    on public.subscription_periods (professional_id, ends_at desc);

alter table public.subscription_periods enable row level security;

create policy subscription_periods_select_own
    on public.subscription_periods
    for select
    to authenticated
    using (professional_id = (select auth.uid()));

revoke all on public.subscription_periods from anon, authenticated;
grant select on public.subscription_periods to authenticated;

-- ---------------------------------------------------------------------------
-- 3. Validity, decided once. Executable only by the engine: a client could
--    otherwise ask about any professional's subscription.
-- ---------------------------------------------------------------------------

create function public.professional_has_valid_subscription(p_professional_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select not (select s.subscription_required from public.platform_settings s where s.id)
        or exists (
            select 1
            from public.subscription_periods sp
            where sp.professional_id = p_professional_id
              and sp.starts_at <= now()
              and sp.ends_at > now()
        );
$$;

revoke all on function public.professional_has_valid_subscription(uuid) from public, anon, authenticated;

-- The caller's own subscription, as the professional reads it. status is the
-- source of the period in force, EXPIRED when the latest one ended, and null
-- with no period at all.
create function public.my_subscription()
returns table (required boolean, status text, ends_at timestamptz)
language sql
stable
security definer
set search_path = ''
as $$
    select
        (select s.subscription_required from public.platform_settings s where s.id),
        case
            when latest.ends_at is null then null
            when latest.ends_at > now() then latest.source::text
            else 'EXPIRED'
        end,
        latest.ends_at
    from (select 1) as one
    left join lateral (
        select sp.source, sp.ends_at
        from public.subscription_periods sp
        where sp.professional_id = (select auth.uid())
        order by sp.ends_at desc
        limit 1
    ) as latest on true;
$$;

revoke all on function public.my_subscription() from public, anon;
grant execute on function public.my_subscription() to authenticated;

-- ---------------------------------------------------------------------------
-- 4. The trial: granted at the transition to APPROVED, once per professional.
--    A second approval, after a document sent the professional back to
--    PENDING, finds the trial already there.
-- ---------------------------------------------------------------------------

create function public.grant_trial_on_approval()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.subscription_periods (professional_id, source, starts_at, ends_at)
    select new.id, 'TRIAL', now(), now() + make_interval(days => s.trial_days)
    from public.platform_settings s
    where s.id
    on conflict (professional_id) where source = 'TRIAL' do nothing;
    return null;
end;
$$;

revoke all on function public.grant_trial_on_approval() from public, anon, authenticated;

create trigger professionals_grant_trial_on_approval
    after update of verification_status on public.professionals
    for each row
    when (new.verification_status = 'APPROVED' and old.verification_status is distinct from 'APPROVED')
    execute function public.grant_trial_on_approval();

-- Those already approved get theirs from today.
insert into public.subscription_periods (professional_id, source, starts_at, ends_at)
select pro.id, 'TRIAL', now(), now() + make_interval(days => s.trial_days)
from public.professionals pro
cross join public.platform_settings s
where s.id
  and pro.verification_status = 'APPROVED'
on conflict (professional_id) where source = 'TRIAL' do nothing;

-- ---------------------------------------------------------------------------
-- 5. A first offer needs a valid subscription. A trigger and not the insert
--    policy, so the refusal names its reason: a policy refuses with the
--    generic row-level security error, and WITH CHECK runs after BEFORE
--    triggers. A counter-offer, which has a parent, is never asked.
-- ---------------------------------------------------------------------------

create function public.guard_first_offer_needs_a_subscription()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not public.professional_has_valid_subscription(new.professional_id) then
        raise exception 'subscription_expired';
    end if;
    return new;
end;
$$;

revoke all on function public.guard_first_offer_needs_a_subscription() from public, anon, authenticated;

create trigger request_offers_guard_first_offer_subscription
    before insert on public.request_offers
    for each row
    when (new.issuer = 'PROFESSIONAL' and new.parent_offer_id is null)
    execute function public.guard_first_offer_needs_a_subscription();

-- ---------------------------------------------------------------------------
-- 6. The search: an expired professional is not found.
-- ---------------------------------------------------------------------------

create or replace function public.search_nearby_professionals(
    p_latitude double precision,
    p_longitude double precision,
    p_radius_km double precision default 5,
    p_service_type_id uuid default null,
    p_available_now boolean default null,
    p_limit integer default 20,
    p_offset integer default 0
)
returns table (
    id uuid,
    full_name text,
    photo_url text,
    professional_type public.professional_type,
    specialty text,
    base_rate_bob numeric,
    average_rating numeric,
    total_reviews integer,
    total_services integer,
    available_now boolean,
    distance_m double precision,
    base_latitude double precision,
    base_longitude double precision
)
language sql
stable
security definer
set search_path = ''
as $$
    with snapped as (
        select
            pro.id,
            p.full_name,
            p.photo_url,
            pro.professional_type,
            pro.specialty,
            pro.base_rate_bob,
            pro.average_rating,
            pro.total_reviews,
            pro.total_services,
            pro.available_now,
            pro.coverage_radius_km,
            -- The base on the city block grid, built once. Longitude first and
            -- latitude second, as st_makepoint takes them; st_x is the
            -- longitude and st_y the latitude on the way out of the geometry.
            extensions.st_setsrid(
                extensions.st_makepoint(
                    round(extensions.st_x(a.location::extensions.geometry)::numeric, 3)::double precision,
                    round(extensions.st_y(a.location::extensions.geometry)::numeric, 3)::double precision
                ),
                4326
            )::extensions.geography as base
        from public.professionals pro
        join public.profiles p on p.id = pro.id
        join public.addresses a on a.profile_id = pro.id and a.is_professional_base
        where
            -- INV-07: only approved, active and valid professionals are visible.
            pro.verification_status = 'APPROVED'
            and p.active
            and public.professional_has_valid_subscription(pro.id)
            -- RN-13: a dual-role person never appears in their own results.
            and pro.id <> (select auth.uid())
            -- Index accelerated, on the untouched column: this is the condition
            -- that uses idx_addresses_location. It only has to be generous
            -- enough to keep every row the snapped test below could accept.
            and extensions.st_dwithin(
                a.location,
                extensions.st_setsrid(
                    extensions.st_makepoint(p_longitude, p_latitude), 4326
                )::extensions.geography,
                (round(coalesce(p_radius_km, 5)::numeric, 1) * 1000)::double precision + 100
            )
            and extensions.st_dwithin(
                a.location,
                extensions.st_setsrid(
                    extensions.st_makepoint(p_longitude, p_latitude), 4326
                )::extensions.geography,
                pro.coverage_radius_km * 1000 + 100
            )
            and (p_available_now is null or pro.available_now = p_available_now)
            and (
                p_service_type_id is null
                or exists (
                    select 1
                    from public.professional_services ps
                    where ps.professional_id = pro.id
                      and ps.service_type_id = p_service_type_id
                      and ps.active
                )
            )
    )
    select
        s.id,
        s.full_name,
        s.photo_url,
        s.professional_type,
        s.specialty,
        s.base_rate_bob,
        s.average_rating,
        s.total_reviews,
        s.total_services,
        s.available_now,
        round(
            extensions.st_distance(
                s.base,
                extensions.st_setsrid(
                    extensions.st_makepoint(p_longitude, p_latitude), 4326
                )::extensions.geography
            )::numeric,
            -2
        )::double precision as distance_m,
        extensions.st_y(s.base::extensions.geometry) as base_latitude,
        extensions.st_x(s.base::extensions.geometry) as base_longitude
    from snapped s
    where
        -- The conditions that decide, now about the published point. A caller
        -- bisecting on the origin recovers this circle, and its centre is the
        -- marker they already have.
        extensions.st_dwithin(
            s.base,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography,
            (round(coalesce(p_radius_km, 5)::numeric, 1) * 1000)::double precision
        )
        and extensions.st_dwithin(
            s.base,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography,
            s.coverage_radius_km * 1000
        )
    -- RF-06.4: ascending distance. Ordered by the snapped distance now, so the
    -- moment two rows swap places says nothing the two markers did not. Ties
    -- are frequent by construction, which is why the tiebreaker is mandatory.
    order by
        extensions.st_distance(
            s.base,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography
        ) asc,
        s.id asc
    -- RNF-08: no query returns an unbounded collection, and a negative limit
    -- or offset clamps to the nearest valid value instead of reaching Postgres.
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

-- ---------------------------------------------------------------------------
-- 7. The inbox: an expired professional keeps only the requests they already
--    offered on, so an open negotiation stays where it is picked up.
-- ---------------------------------------------------------------------------

create or replace function public.request_inbox(p_limit integer default 20, p_offset integer default 0)
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

-- ---------------------------------------------------------------------------
-- 8. The nearby push reaches exactly those the inbox shows the new request to.
-- ---------------------------------------------------------------------------

create or replace function public.immediate_request_recipients(p_request_id uuid)
returns table (profile_id uuid, distance_m integer)
language sql
stable
security definer
set search_path = ''
as $$
    select
        pro.id,
        round(extensions.st_distance(base.location, r.location)::numeric, -2)::integer
    from public.service_requests r
    join public.professionals pro
        on pro.id <> r.patient_id
       and pro.available_now
    join public.addresses base
        on base.profile_id = pro.id
       and base.is_professional_base
    where r.id = p_request_id
      and r.modality = 'IMMEDIATE'
      and r.status in ('PUBLISHED', 'NEGOTIATING')
      and (r.expires_at is null or r.expires_at > now())
      -- 50 km is the largest coverage_radius_km the check constraint allows;
      -- only this form can use idx_addresses_location.
      and extensions.st_dwithin(base.location, r.location, 50000)
      and public.professional_covers_for(pro.id, r.location, r.service_type_id)
      and public.professional_has_valid_subscription(pro.id);
$$;

-- ---------------------------------------------------------------------------
-- 9. Booking: an expired professional has no start time to offer, and a
--    request addressed to them is refused as unavailable, as for anyone the
--    directory does not hold. The patient is not told why.
-- ---------------------------------------------------------------------------

create or replace function public.bookable_start_times(p_professional_id uuid, p_service_type_id uuid)
returns table (start_at timestamptz, local_date date, local_time time)
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
          and public.professional_has_valid_subscription(p_professional_id)
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

    -- INV-07: only an approved, active and valid professional can be addressed.
    if not exists (
        select 1
        from public.professional_directory d
        where d.id = p_professional_id
    ) or not public.professional_has_valid_subscription(p_professional_id) then
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
