-- HT-24: negotiate in fewer steps. The patient sees each offer with its distance
-- and can never counter below what they already offered; the patient's own
-- requests carry the professional they scheduled with; the offer push names its
-- professional, so the tap opens that thread. docs/decisions.md, 2026-10-27, HT-24.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. my_request_offers: the distance and the patient's own budget.
-- ---------------------------------------------------------------------------

-- The distance is measured from the published point of the professional's base
-- -- the base snapped to the 0,001 degree grid, the one the nearby search draws --
-- and rounded to the hundred metres, exactly as search_nearby_professionals
-- does (20261011114540). It opens no new channel: the point is already public,
-- and the origin is the patient's own address, not a free parameter
-- (docs/decisions.md, 2026-10-11). It is the distance to the base, not to where
-- the professional is now, which the system does not keep.
--
-- suggested_budget_bob is the request's own budget, repeated on each row: it is
-- the patient's first offer, and with it the client works out the floor of a
-- counter-offer before sending one.
drop function public.my_request_offers(uuid);

create function public.my_request_offers(p_request_id uuid)
returns table (
    id uuid,
    parent_offer_id uuid,
    professional_id uuid,
    professional_name text,
    professional_photo_url text,
    professional_rating numeric,
    amount_bob numeric,
    message text,
    issuer public.offer_issuer,
    status public.offer_status,
    created_at timestamptz,
    distance_m double precision,
    suggested_budget_bob numeric
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        o.id,
        o.parent_offer_id,
        o.professional_id,
        d.full_name as professional_name,
        d.photo_url as professional_photo_url,
        d.average_rating as professional_rating,
        o.amount_bob,
        o.message,
        o.issuer,
        o.status,
        o.created_at,
        round(
            extensions.st_distance(
                extensions.st_setsrid(
                    extensions.st_makepoint(
                        round(extensions.st_x(base.location::extensions.geometry)::numeric, 3)::double precision,
                        round(extensions.st_y(base.location::extensions.geometry)::numeric, 3)::double precision
                    ),
                    4326
                )::extensions.geography,
                r.location
            )::numeric,
            -2
        )::double precision as distance_m,
        r.suggested_budget_bob
    from public.request_offers o
    join public.service_requests r on r.id = o.request_id
    left join public.professional_directory d on d.id = o.professional_id
    -- Through the directory and not the offer: a professional who left it
    -- (deactivated) no longer has a published point, so no distance either.
    left join public.addresses base
        on base.profile_id = d.id
       and base.is_professional_base
    where o.request_id = p_request_id
      and r.patient_id = (select auth.uid())
    order by o.created_at asc, o.id asc;
$$;

revoke all on function public.my_request_offers(uuid) from public, anon;
grant execute on function public.my_request_offers(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 2. The patient never counters below what they already offered.
-- ---------------------------------------------------------------------------

-- The floor is the greater of the suggested budget -- the patient's first offer,
-- like the fare a passenger publishes -- and the highest amount the patient has
-- proposed on this thread. Per thread and not across the request, because each
-- professional negotiates apart. greatest() skips nulls, so with neither there is
-- no floor. Only the patient has one.
--
-- A definer, so the thread is read whole and not through the caller's policies.
-- The check only runs for the owner of the request: for anybody else the insert
-- policy refuses the row anyway, and answering them with this label would let
-- them bisect someone else's budget one amount at a time.
create or replace function public.guard_patient_counter_floor()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_floor numeric;
begin
    select greatest(
               r.suggested_budget_bob,
               (
                   select max(o.amount_bob)
                   from public.request_offers o
                   where o.request_id = new.request_id
                     and o.professional_id = new.professional_id
                     and o.issuer = 'PATIENT'
               )
           )
      into v_floor
    from public.service_requests r
    where r.id = new.request_id
      and r.patient_id = (select auth.uid());

    if v_floor is not null and new.amount_bob < v_floor then
        raise exception 'counter_offer_below_your_last_offer';
    end if;
    return new;
end;
$$;

revoke all on function public.guard_patient_counter_floor() from public, anon, authenticated;

create trigger request_offers_guard_patient_floor
    before insert on public.request_offers
    for each row
    when (new.issuer = 'PATIENT')
    execute function public.guard_patient_counter_floor();

-- ---------------------------------------------------------------------------
-- 3. my_service_requests: the professional a scheduled request is for.
-- ---------------------------------------------------------------------------

-- Appended at the end, so create or replace keeps working. It is the professional
-- the patient chose when scheduling, whose name the view already shows; with the
-- identifier, "Mis solicitudes" opens that thread straight away. The grants are
-- set again because a definer view that is not stripped of them can be written
-- through (docs/decisions.md, 2026-10-11).
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
    r.professional_id
from public.service_requests r
join public.service_types t on t.id = r.service_type_id
left join public.profiles p on p.id = r.professional_id
where r.patient_id = (select auth.uid());

revoke all on public.my_service_requests from anon, authenticated;
grant select on public.my_service_requests to authenticated;

-- ---------------------------------------------------------------------------
-- 4. The offer push names its professional, so the tap opens that thread.
-- ---------------------------------------------------------------------------

-- Rewritten whole with all six branches, because NotificationEventParityTest only
-- scans the last migration that defines it. The only change is that
-- OFFER_RECEIVED also reads and sends the professional of the offer.
create or replace function public.request_push_body(
    p_event text,
    p_request_id uuid,
    p_offer_id uuid default null
)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_request public.service_requests;
    v_type_name text;
    v_role text;
    v_ttl integer;
    v_amount numeric;
    v_issuer public.offer_issuer;
    v_professional_id uuid;
    v_targets jsonb;
begin
    select * into v_request from public.service_requests where id = p_request_id;
    if not found then
        return null;
    end if;

    select t.name into v_type_name
    from public.service_types t
    where t.id = v_request.service_type_id;

    if p_event = 'REQUEST_NEARBY' then
        v_role := 'PROFESSIONAL';
        v_ttl := greatest(extract(epoch from v_request.expires_at - now())::integer, 0);
        select jsonb_agg(jsonb_build_object(
                   'recipientId', rcp.profile_id,
                   'fid', d.token,
                   'distanceM', rcp.distance_m))
        into v_targets
        from public.immediate_request_recipients(p_request_id) rcp
        join public.device_tokens d on d.profile_id = rcp.profile_id;
    elsif p_event = 'APPOINTMENT_SCHEDULED' then
        v_role := 'PROFESSIONAL';
        v_ttl := greatest(extract(epoch from v_request.scheduled_at - now())::integer, 0);
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = v_request.professional_id
          and exists (
              select 1
              from public.professional_directory pd
              where pd.id = d.profile_id
          );
    elsif p_event = 'APPOINTMENT_CANCELLED' then
        v_role := 'PROFESSIONAL';
        v_ttl := greatest(extract(epoch from v_request.scheduled_at - now())::integer, 0);
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = v_request.professional_id;
    elsif p_event = 'REQUEST_EXPIRED' then
        v_role := 'PATIENT';
        v_ttl := 86400;
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = v_request.patient_id;
    elsif p_event = 'OFFER_RECEIVED' then
        v_role := 'PATIENT';
        v_ttl := 86400;
        select o.amount_bob, o.professional_id into v_amount, v_professional_id
        from public.request_offers o
        where o.id = p_offer_id;
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = v_request.patient_id;
    elsif p_event = 'COUNTER_OFFER_RECEIVED' then
        v_ttl := 86400;
        select o.amount_bob, o.issuer, o.professional_id
          into v_amount, v_issuer, v_professional_id
        from public.request_offers o
        where o.id = p_offer_id;
        if v_issuer is null then
            return null;
        end if;
        if v_issuer = 'PROFESSIONAL' then
            v_role := 'PATIENT';
            select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
            into v_targets
            from public.device_tokens d
            where d.profile_id = v_request.patient_id;
        else
            v_role := 'PROFESSIONAL';
            -- The professional of the thread, not service_requests.professional_id,
            -- which is null on an immediate request.
            select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
            into v_targets
            from public.device_tokens d
            where d.profile_id = v_professional_id
              and exists (
                  select 1
                  from public.professional_directory pd
                  where pd.id = d.profile_id
              );
        end if;
    else
        raise exception 'unknown_push_event';
    end if;

    if v_targets is null then
        return null;
    end if;

    return jsonb_strip_nulls(jsonb_build_object(
        'event', p_event,
        'requestId', v_request.id,
        'recipientRole', v_role,
        'serviceTypeName', v_type_name,
        'scheduledAt', case
            when p_event in ('APPOINTMENT_SCHEDULED', 'APPOINTMENT_CANCELLED')
                then to_char(v_request.scheduled_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
        end,
        'amountBob', case when v_amount is not null then v_amount::text end,
        'professionalId', case when v_professional_id is not null then v_professional_id::text end,
        'ttlSeconds', v_ttl,
        'targets', v_targets
    ));
end;
$$;

revoke all on function public.request_push_body(text, uuid, uuid) from public, anon, authenticated;
