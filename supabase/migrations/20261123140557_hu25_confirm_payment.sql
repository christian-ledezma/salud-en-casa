-- HU-25: the patient confirms they paid the attention, naming the method. The
-- engine seals when, the confirmation is final, and the professional is told.
-- docs/decisions.md, 2026-11-23, HU-25.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. A confirmed payment names its method (RF-11.2).
-- ---------------------------------------------------------------------------

alter table public.payments
    add constraint confirmed_payment_names_its_method check (
        patient_confirmed_at is null or method is not null
    );

-- ---------------------------------------------------------------------------
-- 2. What each party may write. Rewritten whole, as in HU-24 and HT-30.
-- ---------------------------------------------------------------------------

create or replace function public.guard_payment_confirmation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_patient uuid;
    v_professional uuid;
    v_service_status public.service_status;
begin
    -- No authenticated user means the service key or a definer function is
    -- writing. Those already bypass row level security, so there is nothing to
    -- add here.
    if v_caller is null then
        return new;
    end if;

    select s.patient_id, s.professional_id, s.status
    into v_patient, v_professional, v_service_status
    from public.services s
    where s.id = new.service_id;

    if new.status is distinct from old.status
        and (old.status = 'CANCELLED' or (new.status = 'CANCELLED' and v_service_status <> 'CANCELLED')) then
        raise exception 'payment_cancellation_follows_the_service';
    end if;

    -- cancel_payment_with_service runs with the caller's claims, so a party
    -- cancelling the service reaches this point: the check above already
    -- proved the service is cancelled.
    if new.status = 'CANCELLED' and old.status is distinct from 'CANCELLED' then
        return new;
    end if;

    if public.is_admin() then
        return new;
    end if;

    if new.provider is distinct from old.provider
        or new.external_reference is distinct from old.external_reference then
        raise exception 'gateway_fields_are_written_by_the_server';
    end if;

    if v_caller = v_patient then
        if new.professional_confirmed_at is distinct from old.professional_confirmed_at then
            raise exception 'each_party_confirms_separately';
        end if;
        if new.patient_confirmed_at is distinct from old.patient_confirmed_at then
            raise exception 'confirmation_time_is_sealed_by_the_engine';
        end if;
        if new.status is distinct from old.status or new.method is distinct from old.method then
            if old.status <> 'PENDING' or new.status <> 'PATIENT_CONFIRMED' then
                raise exception 'patient_confirms_a_pending_payment_once';
            end if;
            if v_service_status <> 'COMPLETED' then
                raise exception 'payment_confirmation_requires_a_completed_service';
            end if;
            if new.method is null or new.method not in ('CASH', 'DIRECT_QR') then
                raise exception 'payment_method_not_allowed';
            end if;
        end if;
    elsif v_caller = v_professional then
        if new.patient_confirmed_at is distinct from old.patient_confirmed_at then
            raise exception 'each_party_confirms_separately';
        end if;
        if new.method is distinct from old.method then
            raise exception 'payment_method_is_declared_by_the_patient';
        end if;
        -- payments_stamp_confirmation seals the patient's side on any move to
        -- PATIENT_CONFIRMED, so that move is the patient's alone.
        if new.status is distinct from old.status and new.status <> 'BOTH_CONFIRMED' then
            raise exception 'each_party_confirms_separately';
        end if;
        if new.professional_confirmed_at is distinct from old.professional_confirmed_at
            and v_service_status <> 'COMPLETED' then
            raise exception 'payment_confirmation_requires_a_completed_service';
        end if;
    else
        raise exception 'not_a_party_of_this_payment';
    end if;

    if new.status = 'DISPUTED' and old.status is distinct from 'DISPUTED' then
        raise exception 'dispute_requires_the_guarded_resolution_path';
    end if;

    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- 3. The engine seals when the patient confirmed. The trigger name sorts after
--    payments_guard_confirmation, so the guard sees the row as the client sent it.
-- ---------------------------------------------------------------------------

create function public.stamp_payment_confirmation()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if old.status = 'PENDING' and new.status = 'PATIENT_CONFIRMED' then
        new.patient_confirmed_at := now();
    end if;
    return new;
end;
$$;

revoke all on function public.stamp_payment_confirmation() from public, anon, authenticated;

create trigger payments_stamp_confirmation
    before update on public.payments
    for each row execute function public.stamp_payment_confirmation();

-- ---------------------------------------------------------------------------
-- 4. Live: the attention topic also hears a payment change, so an open
--    attention rereads itself.
-- ---------------------------------------------------------------------------

create function public.notify_attention_payment_changed()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_request_id uuid;
begin
    if new.status is distinct from old.status then
        select s.request_id into v_request_id from public.services s where s.id = new.service_id;
        perform realtime.send(
            '{}'::jsonb,
            'attention_changed',
            'attention:' || v_request_id::text,
            true
        );
    end if;
    return null;
end;
$$;

revoke all on function public.notify_attention_payment_changed() from public, anon, authenticated;

create trigger payments_notify_change
    after update of status on public.payments
    for each row execute function public.notify_attention_payment_changed();

-- ---------------------------------------------------------------------------
-- 5. The professional is told the patient confirmed (RF-09.7). Queued inside
--    the transaction, so a rollback sends nothing.
-- ---------------------------------------------------------------------------

create function public.queue_payment_push()
returns trigger
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_request_id uuid;
    v_url text;
    v_secret text;
    v_body jsonb;
begin
    if old.patient_confirmed_at is not null or new.patient_confirmed_at is null then
        return null;
    end if;

    select s.request_id into v_request_id from public.services s where s.id = new.service_id;

    select decrypted_secret into v_url
    from vault.decrypted_secrets
    where name = 'push_project_url';

    select decrypted_secret into v_secret
    from vault.decrypted_secrets
    where name = 'push_webhook_secret';

    if v_url is null or v_secret is null then
        return null;
    end if;

    v_body := public.request_push_body('PATIENT_CONFIRMED_PAYMENT', v_request_id);
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
    raise warning 'payment push not queued for payment %: %', new.id, sqlerrm;
    return null;
end;
$$;

revoke all on function public.queue_payment_push() from public, anon, authenticated;

-- Not "after update of patient_confirmed_at": that column is set by
-- payments_stamp_confirmation, never named in the client's SET, and an OF list
-- only fires for columns the statement names.
create trigger payments_queue_push
    after update on public.payments
    for each row execute function public.queue_payment_push();

-- ---------------------------------------------------------------------------
-- 6. request_push_body gains PATIENT_CONFIRMED_PAYMENT and paymentMethod.
--    NotificationEventParityTest only scans the last migration that defines it,
--    so it is rewritten whole, with the same signature.
-- ---------------------------------------------------------------------------

create or replace function public.request_push_body(
    p_event text,
    p_request_id uuid default null,
    p_offer_id uuid default null,
    p_message_id uuid default null,
    p_profile_id uuid default null,
    p_document_count integer default null,
    p_status text default null
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
    v_method text;
    v_issuer public.offer_issuer;
    v_professional_id uuid;
    v_sender_id uuid;
    v_sender_name text;
    v_service_patient_id uuid;
    v_service_professional_id uuid;
    v_recipient_id uuid;
    v_targets jsonb;
begin
    -- NotificationEventParityTest scans for the literal "p_event = '…'" shape,
    -- so both names are spelled out rather than passed to IN.
    if p_event = 'DOCUMENT_VERDICT' or p_event = 'VERIFICATION_APPROVED' then
        v_ttl := 86400;
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = p_profile_id;

        if v_targets is null then
            return null;
        end if;

        return jsonb_strip_nulls(jsonb_build_object(
            'event', p_event,
            'status', p_status,
            'documentCount', p_document_count,
            'ttlSeconds', v_ttl,
            'targets', v_targets
        ));
    end if;

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
    elsif p_event = 'MESSAGE_RECEIVED' then
        v_ttl := 86400;
        select m.sender_id, p.full_name, s.patient_id, s.professional_id
          into v_sender_id, v_sender_name, v_service_patient_id, v_service_professional_id
        from public.messages m
        join public.services s on s.request_id = m.request_id
        join public.profiles p on p.id = m.sender_id
        where m.id = p_message_id
          and m.request_id = p_request_id;
        if v_sender_id is null then
            return null;
        end if;
        if v_sender_id = v_service_patient_id then
            v_role := 'PROFESSIONAL';
            v_recipient_id := v_service_professional_id;
        else
            v_role := 'PATIENT';
            v_recipient_id := v_service_patient_id;
        end if;
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = v_recipient_id;
    elsif p_event = 'PROFESSIONAL_ARRIVED' or p_event = 'SERVICE_COMPLETED' then
        v_role := 'PATIENT';
        v_ttl := 86400;
        select p.full_name into v_sender_name
        from public.services s
        join public.profiles p on p.id = s.professional_id
        where s.request_id = p_request_id;
        if v_sender_name is null then
            return null;
        end if;
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = v_request.patient_id;
    elsif p_event = 'ATTENTION_CANCELLED' then
        v_ttl := 86400;
        select s.cancelled_by, p.full_name, s.patient_id, s.professional_id
          into v_sender_id, v_sender_name, v_service_patient_id, v_service_professional_id
        from public.services s
        join public.profiles p on p.id = s.cancelled_by
        where s.request_id = p_request_id;
        if v_sender_id is null then
            return null;
        end if;
        if v_sender_id = v_service_patient_id then
            v_role := 'PROFESSIONAL';
            v_recipient_id := v_service_professional_id;
        else
            v_role := 'PATIENT';
            v_recipient_id := v_service_patient_id;
        end if;
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = v_recipient_id;
    elsif p_event = 'PATIENT_CONFIRMED_PAYMENT' then
        v_role := 'PROFESSIONAL';
        v_ttl := 86400;
        -- The method is a system value, not text a user wrote (RF-09.10).
        select p.full_name, s.professional_id, pay.total_amount_bob, pay.method::text
          into v_sender_name, v_recipient_id, v_amount, v_method
        from public.services s
        join public.payments pay on pay.service_id = s.id
        join public.profiles p on p.id = s.patient_id
        where s.request_id = p_request_id
          and pay.patient_confirmed_at is not null;
        if v_sender_name is null then
            return null;
        end if;
        select jsonb_agg(jsonb_build_object('recipientId', d.profile_id, 'fid', d.token))
        into v_targets
        from public.device_tokens d
        where d.profile_id = v_recipient_id;
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
        'paymentMethod', v_method,
        'professionalId', case when v_professional_id is not null then v_professional_id::text end,
        'senderName', v_sender_name,
        'ttlSeconds', v_ttl,
        'targets', v_targets
    ));
end;
$$;

revoke all on function public.request_push_body(text, uuid, uuid, uuid, uuid, integer, text) from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- 7. my_attention carries the payment, as its reader may see it.
-- ---------------------------------------------------------------------------

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
    i_am_patient boolean,
    scheduled_at timestamptz,
    started_at timestamptz,
    finished_at timestamptz,
    description text,
    city text,
    address_text text,
    reference text,
    latitude double precision,
    longitude double precision,
    cancellation_reason text,
    cancelled_at timestamptz,
    cancelled_by_me boolean,
    conversation_open boolean,
    conversation_writable_until timestamptz,
    payment_status public.payment_status,
    payment_method public.payment_method,
    patient_confirmed_at timestamptz,
    professional_confirmed_at timestamptz
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
        (s.patient_id = (select auth.uid())) as i_am_patient,
        r.scheduled_at,
        s.started_at,
        s.finished_at,
        r.description,
        r.city,
        case when shows_address then r.address_text end,
        case when shows_address then r.reference end,
        case when shows_address then extensions.st_y(r.location::extensions.geometry) end,
        case when shows_address then extensions.st_x(r.location::extensions.geometry) end,
        s.cancellation_reason,
        s.cancelled_at,
        s.cancelled_by = (select auth.uid()),
        public.conversation_is_open(s.status, s.finished_at),
        case when s.status = 'COMPLETED' then public.conversation_writable_until(s.finished_at) end,
        pay.status,
        pay.method,
        pay.patient_confirmed_at,
        pay.professional_confirmed_at
    from public.services s
    join public.service_requests r on r.id = s.request_id
    join public.service_types t on t.id = r.service_type_id
    join public.payments pay on pay.service_id = s.id
    join public.profiles cp
        on cp.id = case
            when s.patient_id = (select auth.uid()) then s.professional_id
            else s.patient_id
        end
    cross join lateral (
        select (
            s.patient_id = (select auth.uid())
            or s.status in ('ASSIGNED', 'IN_PROGRESS')
        ) as shows_address
    ) visibility
    where s.request_id = p_request_id
      and (s.patient_id = (select auth.uid()) or s.professional_id = (select auth.uid()));
$$;

revoke all on function public.my_attention(uuid) from public, anon;
grant execute on function public.my_attention(uuid) to authenticated;
