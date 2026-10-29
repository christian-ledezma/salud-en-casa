-- HU-19: accepting an offer. Either party accepts the offer on the table in one
-- atomic operation: the offer is marked accepted, the service and its pending
-- payment are created, and the request moves to accepted with its professional
-- set. The counterpart's contact is revealed (the services row switches on the
-- select_counterpart policies of 20260911120400), and the issuer of the accepted
-- offer is told. docs/decisions.md, 2026-10-29.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. The commission percentage is configurable (RN-08), with 10 % to start.
-- ---------------------------------------------------------------------------

-- The payment breakdown is frozen by guard_payment_is_frozen from the moment the
-- row is born, and it is born at acceptance. A deferred split (Sprint 8, RF-11.1)
-- could never be corrected afterwards without relaxing that guard, so the column
-- enters here, the first time a breakdown is written. The settings table already
-- anticipated it (20261012120000).
alter table public.platform_settings
    add column platform_fee_percent numeric(5, 2) not null default 10
        check (platform_fee_percent >= 0 and platform_fee_percent <= 100);

-- ---------------------------------------------------------------------------
-- 2. The lifecycle guard recognises the atomic operation by the service.
-- ---------------------------------------------------------------------------

-- accept_offer inserts the service before it updates the request, so an existing
-- service for this request is proof the atomic operation is running. No client
-- can insert one (services has no insert policy), so this cannot be forged -- a
-- session flag could, because the patient's own cancel policy lets them update
-- their request. This keeps the protection against a request born ACCEPTED that
-- HU-18 left noted, and now also admits PUBLISHED/NEGOTIATING -> ACCEPTED and the
-- professional_id an immediate request gains at acceptance. Rewritten whole.
create or replace function public.guard_request_lifecycle()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    v_has_service boolean;
begin
    if (select auth.uid()) is null then
        return new;
    end if;

    v_has_service := exists (
        select 1 from public.services s where s.request_id = new.id
    );

    if new.professional_id is distinct from old.professional_id then
        if not (
            new.status = 'ACCEPTED'
            and old.status in ('PUBLISHED', 'NEGOTIATING')
            and v_has_service
        ) then
            raise exception 'request_professional_is_set_at_creation';
        end if;
    end if;

    if new.status is distinct from old.status then
        if not (
            (new.status = 'CANCELLED' and old.status in ('PUBLISHED', 'NEGOTIATING'))
            or (old.status = 'PUBLISHED' and new.status = 'NEGOTIATING')
            or (
                new.status = 'ACCEPTED'
                and old.status in ('PUBLISHED', 'NEGOTIATING')
                and v_has_service
            )
        ) then
            raise exception 'request_status_change_requires_the_atomic_operation';
        end if;
    end if;

    return new;
end;
$$;

-- The same reservation seen from the offer side (20260912110000): an offer
-- reached ACCEPTED only through the atomic operation, which now exists. It is
-- recognised the same way as the request's -- a service for this offer already
-- exists, inserted by accept_offer before it marks the offer -- so no client can
-- forge it. The trigger request_offers_guard_acceptance keeps pointing here.
create or replace function public.guard_offer_acceptance()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.status is distinct from old.status and new.status = 'ACCEPTED'
        and not exists (
            select 1 from public.services s where s.offer_id = new.id
        ) then
        raise exception 'offer_acceptance_requires_the_atomic_operation';
    end if;
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- 3. accept_offer: the whole acceptance in one transaction (RF-08.5).
-- ---------------------------------------------------------------------------

-- A definer, so no policy stands between the four writes, and the guard above is
-- what keeps the transition honest. The offer on the table is the one still
-- PROPOSED and not yet answered; whoever did not issue it is the one who accepts
-- it. The service freezes the agreed amount (INV-04, RN-07), and the payment is
-- split from the configured percentage, with professional_amount as total - fee
-- so INV-10 is exact, never a second rounding. services.request_id and offer_id
-- are unique, so two acceptances at once leave exactly one winner.
create or replace function public.accept_offer(p_offer_id uuid)
returns uuid
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_offer public.request_offers;
    v_request public.service_requests;
    v_pct numeric;
    v_fee numeric;
    v_service_id uuid;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    select * into v_offer from public.request_offers where id = p_offer_id;
    if not found then
        raise exception 'offer_not_found';
    end if;

    if v_offer.status <> 'PROPOSED' then
        raise exception 'offer_not_open';
    end if;

    if exists (
        select 1 from public.request_offers a where a.parent_offer_id = v_offer.id
    ) then
        raise exception 'offer_already_answered';
    end if;

    select * into v_request from public.service_requests where id = v_offer.request_id;
    if v_request.status not in ('PUBLISHED', 'NEGOTIATING') then
        raise exception 'request_not_open';
    end if;

    -- INV-06: a professional-issued offer is accepted by the patient; a patient's
    -- counter-offer, by the professional of the thread.
    if v_offer.issuer = 'PROFESSIONAL' then
        if v_caller <> v_request.patient_id then
            raise exception 'not_your_offer_to_accept';
        end if;
    else
        if v_caller <> v_offer.professional_id then
            raise exception 'not_your_offer_to_accept';
        end if;
    end if;

    select platform_fee_percent into v_pct from public.platform_settings where id;
    v_fee := round(v_offer.amount_bob * v_pct / 100, 2);

    insert into public.services (
        request_id, offer_id, patient_id, professional_id, final_amount_bob
    ) values (
        v_request.id, v_offer.id, v_request.patient_id, v_offer.professional_id, v_offer.amount_bob
    )
    returning id into v_service_id;

    insert into public.payments (
        service_id, total_amount_bob, platform_fee_bob, professional_amount_bob
    ) values (
        v_service_id, v_offer.amount_bob, v_fee, v_offer.amount_bob - v_fee
    );

    update public.request_offers set status = 'ACCEPTED' where id = v_offer.id;

    update public.service_requests
       set status = 'ACCEPTED',
           professional_id = v_offer.professional_id
     where id = v_request.id;

    return v_service_id;
end;
$$;

revoke all on function public.accept_offer(uuid) from public, anon;
grant execute on function public.accept_offer(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 4. my_attention: the sealed agreement as one of its two parties reads it.
-- ---------------------------------------------------------------------------

-- The contact of the counterpart -- relative to the caller -- revealed only now
-- (RN-06, RF-08.6), the frozen amount and the status, in one call. A definer so
-- the row is built in one audited place and reserved to a party of the service;
-- the where is the rule, as with my_request_offers and inbox_request.
create or replace function public.my_attention(p_request_id uuid)
returns table (
    service_id uuid,
    request_id uuid,
    status public.service_status,
    final_amount_bob numeric,
    service_type_name text,
    created_at timestamptz,
    counterpart_id uuid,
    counterpart_name text,
    counterpart_phone text,
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
        cp.phone as counterpart_phone,
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

-- ---------------------------------------------------------------------------
-- 5. The push for an accepted offer. OFFER_ACCEPTED reaches the issuer of the
--    accepted offer -- the counterpart of whoever accepted -- and the tap opens
--    the attention of that request (RF-09.5). request_push_body is rewritten
--    whole with all seven branches, because NotificationEventParityTest only
--    scans the last migration that defines it.
-- ---------------------------------------------------------------------------

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
    elsif p_event = 'OFFER_ACCEPTED' then
        v_ttl := 86400;
        select o.amount_bob, o.issuer, o.professional_id
          into v_amount, v_issuer, v_professional_id
        from public.request_offers o
        where o.id = p_offer_id;
        if v_issuer is null then
            return null;
        end if;
        -- The one who is told is the issuer of the accepted offer: a professional
        -- whose offer the patient took, or a patient whose counter the
        -- professional took.
        if v_issuer = 'PROFESSIONAL' then
            v_role := 'PROFESSIONAL';
            select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
            into v_targets
            from public.device_tokens d
            where d.profile_id = v_professional_id
              and exists (
                  select 1
                  from public.professional_directory pd
                  where pd.id = d.profile_id
              );
        else
            v_role := 'PATIENT';
            select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
            into v_targets
            from public.device_tokens d
            where d.profile_id = v_request.patient_id;
        end if;
        -- The attention opens by request, so the body carries no professionalId.
        v_professional_id := null;
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

-- The acceptance push is queued on the service insert, so it rides the same
-- transaction: a rollback discards the queued pg_net row and no notification
-- leaves (criterion 7). It mirrors queue_offer_push, and a failure never fails
-- the write (docs/decisions.md, 2026-10-16).
create or replace function public.queue_acceptance_push()
returns trigger
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_url text;
    v_secret text;
    v_body jsonb;
begin
    select decrypted_secret into v_url
    from vault.decrypted_secrets
    where name = 'push_project_url';

    select decrypted_secret into v_secret
    from vault.decrypted_secrets
    where name = 'push_webhook_secret';

    if v_url is null or v_secret is null then
        return null;
    end if;

    v_body := public.request_push_body('OFFER_ACCEPTED', new.request_id, new.offer_id);
    if v_body is null then
        return null;
    end if;

    perform net.http_post(
        url := v_url || '/functions/v1/send-push',
        body := v_body,
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'x-webhook-secret', v_secret
        ),
        timeout_milliseconds := 10000
    );

    return null;
exception when others then
    raise warning 'acceptance push not queued for service %: %', new.id, sqlerrm;
    return null;
end;
$$;

revoke all on function public.queue_acceptance_push() from public, anon, authenticated;

create trigger services_push_acceptance
    after insert on public.services
    for each row
    execute function public.queue_acceptance_push();
