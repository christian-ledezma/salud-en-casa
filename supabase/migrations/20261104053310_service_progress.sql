-- HU-23: the professional marks arrival and completion. docs/decisions.md,
-- 2026-11-04, HU-23.
--
-- guard_service_transition and guard_service_direct_update exist since
-- 20260911120400 and 20260912110000: they keep the order of the states and who may
-- move them. This migration gives those moves their effects, in the transaction of
-- the update: the dates, the request, the signal and the push. It also closes a
-- gap: either party could rewrite any column of their service.

-- ---------------------------------------------------------------------------
-- 1. What a client may write. HU-24 cancels with the last three columns.
-- ---------------------------------------------------------------------------

revoke update on public.services from anon, authenticated;
grant update (status, cancellation_reason, cancelled_by, cancelled_at) on public.services to authenticated;

-- ---------------------------------------------------------------------------
-- 2. The engine owns both dates.
-- ---------------------------------------------------------------------------

create function public.stamp_service_progress()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.status is distinct from old.status then
        if new.status = 'IN_PROGRESS' then
            new.started_at := now();
        elsif new.status = 'COMPLETED' then
            new.finished_at := now();
        end if;
    end if;
    return new;
end;
$$;

create trigger services_stamp_progress
    before update of status on public.services
    for each row execute function public.stamp_service_progress();

-- ---------------------------------------------------------------------------
-- 3. The request follows its service. Rewritten whole; the only addition is the
--    branch that admits it, proved by the state the service is already in.
-- ---------------------------------------------------------------------------

create or replace function public.guard_request_lifecycle()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    v_has_service boolean;
    v_service_status public.service_status;
begin
    if (select auth.uid()) is null then
        return new;
    end if;

    select s.status into v_service_status
    from public.services s
    where s.request_id = new.id;
    v_has_service := v_service_status is not null;

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
            or (new.status = 'IN_PROGRESS' and old.status = 'ACCEPTED' and v_service_status = 'IN_PROGRESS')
            or (new.status = 'COMPLETED' and old.status = 'IN_PROGRESS' and v_service_status = 'COMPLETED')
        ) then
            raise exception 'request_status_change_requires_the_atomic_operation';
        end if;
    end if;

    return new;
end;
$$;

create function public.move_request_with_service()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.status is distinct from old.status then
        if new.status = 'IN_PROGRESS' then
            update public.service_requests set status = 'IN_PROGRESS' where id = new.request_id;
        elsif new.status = 'COMPLETED' then
            update public.service_requests set status = 'COMPLETED' where id = new.request_id;
        end if;
    end if;
    return null;
end;
$$;

revoke all on function public.move_request_with_service() from public, anon, authenticated;

create trigger services_move_request
    after update of status on public.services
    for each row execute function public.move_request_with_service();

-- ---------------------------------------------------------------------------
-- 4. Live: a data-less signal on a topic of its own. The client hands out one
--    channel per topic, so sharing the conversation's would let leaving one screen
--    cut off the other (docs/decisions.md, 2026-11-04, HU-23).
-- ---------------------------------------------------------------------------

create function public.notify_attention_changed()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.status is distinct from old.status then
        perform realtime.send(
            '{}'::jsonb,
            'attention_changed',
            'attention:' || new.request_id::text,
            true
        );
    end if;
    return null;
end;
$$;

revoke all on function public.notify_attention_changed() from public, anon, authenticated;

create trigger services_notify_change
    after update of status on public.services
    for each row execute function public.notify_attention_changed();

-- Only the two parties of the service may listen. The topic is compared as text,
-- so a malformed topic matches nothing instead of failing a cast.
create policy attention_listen on realtime.messages
    for select to authenticated
    using (
        extension = 'broadcast'
        and exists (
            select 1
            from public.services s
            where 'attention:' || s.request_id::text = realtime.topic()
              and (
                  s.patient_id = (select auth.uid())
                  or s.professional_id = (select auth.uid())
              )
        )
    );

-- ---------------------------------------------------------------------------
-- 5. The push to the patient, with the professional's name and nothing written
--    by anybody (RF-09.6, RF-09.10). request_push_body is rewritten whole, because
--    NotificationEventParityTest only scans the last migration that defines it.
-- ---------------------------------------------------------------------------

create or replace function public.request_push_body(
    p_event text,
    p_request_id uuid,
    p_offer_id uuid default null,
    p_message_id uuid default null
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
    v_sender_id uuid;
    v_sender_name text;
    v_service_patient_id uuid;
    v_service_professional_id uuid;
    v_recipient_id uuid;
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
        'senderName', v_sender_name,
        'ttlSeconds', v_ttl,
        'targets', v_targets
    ));
end;
$$;

revoke all on function public.request_push_body(text, uuid, uuid, uuid) from public, anon, authenticated;

-- Queued on the update, so it rides the same transaction: a rollback discards the
-- queued pg_net row. A failure never fails the write (docs/decisions.md, 2026-10-16).
create function public.queue_service_progress_push()
returns trigger
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_event text;
    v_url text;
    v_secret text;
    v_body jsonb;
begin
    if new.status is not distinct from old.status then
        return null;
    end if;

    v_event := case new.status
        when 'IN_PROGRESS' then 'PROFESSIONAL_ARRIVED'
        when 'COMPLETED' then 'SERVICE_COMPLETED'
    end;
    if v_event is null then
        return null;
    end if;

    select decrypted_secret into v_url
    from vault.decrypted_secrets
    where name = 'push_project_url';

    select decrypted_secret into v_secret
    from vault.decrypted_secrets
    where name = 'push_webhook_secret';

    if v_url is null or v_secret is null then
        return null;
    end if;

    v_body := public.request_push_body(v_event, new.request_id);
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
    raise warning 'service progress push not queued for service %: %', new.id, sqlerrm;
    return null;
end;
$$;

revoke all on function public.queue_service_progress_push() from public, anon, authenticated;

create trigger services_push_progress
    after update of status on public.services
    for each row execute function public.queue_service_progress_push();

-- ---------------------------------------------------------------------------
-- 6. What the attention shows. The professional reads the exact address only
--    while it is of use: docs/decisions.md, 2026-11-04, HU-23.
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
    longitude double precision
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
        case when shows_address then extensions.st_x(r.location::extensions.geometry) end
    from public.services s
    join public.service_requests r on r.id = s.request_id
    join public.service_types t on t.id = r.service_type_id
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
