-- HU-21: the conversation of an attention. docs/decisions.md, 2026-11-03, HU-21.
--
-- messages, its policies and the read_at column grant exist since 20260911120300,
-- 20260911120400, 20260912090000 and 20260912110000. This migration closes the
-- gaps the story found: writing on a cancelled attention, dates chosen by the
-- client, no live signal, and no push. There is still no administrator policy on
-- messages, here or anywhere (INV-12, RN-11).

-- ---------------------------------------------------------------------------
-- 1. Keyset pages read (request_id, created_at, id); id breaks a tie in time.
-- ---------------------------------------------------------------------------

drop index public.idx_messages_request_id_created_at;

create index idx_messages_request_id_created_at_id
    on public.messages (request_id, created_at desc, id desc);

-- A page of the conversation, older than one cursor or newer than another. A row
-- comparison walks the index and stops at the limit; the or-filter PostgREST can
-- write reads every older row and sorts them. Invoker: the select policies decide.
create or replace function public.conversation_page(
    p_request_id uuid,
    p_limit integer,
    p_before_created_at timestamptz default null,
    p_before_id uuid default null,
    p_after_created_at timestamptz default null,
    p_after_id uuid default null
)
returns setof public.messages
language sql
stable
security invoker
set search_path = ''
as $$
    (
        select m.*
        from public.messages m
        where m.request_id = p_request_id
          and p_after_created_at is null
          and (
              p_before_created_at is null
              or (m.created_at, m.id) < (p_before_created_at, p_before_id)
          )
        order by m.created_at desc, m.id desc
        limit least(p_limit, 100)
    )
    union all
    (
        select m.*
        from public.messages m
        where m.request_id = p_request_id
          and p_after_created_at is not null
          and (m.created_at, m.id) > (p_after_created_at, p_after_id)
        order by m.created_at asc, m.id asc
        limit least(p_limit, 100)
    );
$$;

revoke all on function public.conversation_page(uuid, integer, timestamptz, uuid, timestamptz, uuid) from public, anon;
grant execute on function public.conversation_page(uuid, integer, timestamptz, uuid, timestamptz, uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 2. The engine owns both dates. created_at orders the thread and anchors the
--    cursor; read_at is what the sender sees as "Leído".
-- ---------------------------------------------------------------------------

create or replace function public.stamp_message_created_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if (select auth.uid()) is not null then
        new.created_at := now();
    end if;
    new.read_at := null;
    return new;
end;
$$;

create trigger messages_stamp_created_at
    before insert on public.messages
    for each row execute function public.stamp_message_created_at();

create or replace function public.stamp_message_read_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if old.read_at is not null then
        new.read_at := old.read_at;
    elsif new.read_at is not null then
        new.read_at := now();
    end if;
    return new;
end;
$$;

create trigger messages_stamp_read_at
    before update on public.messages
    for each row execute function public.stamp_message_read_at();

-- ---------------------------------------------------------------------------
-- 3. A cancelled attention is read only; a completed one stays open.
-- ---------------------------------------------------------------------------

drop policy messages_insert_patient on public.messages;

create policy messages_insert_patient on public.messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.patient_id = (select auth.uid())
              and s.status <> 'CANCELLED'
        )
    );

drop policy messages_insert_professional on public.messages;

create policy messages_insert_professional on public.messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.professional_id = (select auth.uid())
              and s.status <> 'CANCELLED'
        )
    );

-- ---------------------------------------------------------------------------
-- 4. Marking read. Invoker, so the update policies still decide who may; the
--    client sends no date for the trigger to discard.
-- ---------------------------------------------------------------------------

create or replace function public.mark_conversation_read(p_request_id uuid)
returns integer
language plpgsql
volatile
security invoker
set search_path = ''
as $$
declare
    v_marked integer;
begin
    update public.messages
    set read_at = now()
    where request_id = p_request_id
      and read_at is null
      and sender_id <> (select auth.uid());
    get diagnostics v_marked = row_count;
    return v_marked;
end;
$$;

revoke all on function public.mark_conversation_read(uuid) from public, anon;
grant execute on function public.mark_conversation_read(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 5. Live. A data-less Broadcast to the private topic of the conversation, the
--    pattern of docs/decisions.md, 2026-10-13; both parties re-query.
-- ---------------------------------------------------------------------------

create or replace function public.notify_conversation_message()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform realtime.send(
        '{}'::jsonb,
        'conversation_changed',
        'conversation:' || new.request_id::text,
        true
    );
    return null;
end;
$$;

revoke all on function public.notify_conversation_message() from public, anon, authenticated;

create trigger messages_notify_insert
    after insert on public.messages
    for each row
    execute function public.notify_conversation_message();

-- One signal per conversation and statement: marking thirty messages read is one
-- update, and it would otherwise send thirty.
create or replace function public.notify_conversation_read()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_request_id uuid;
begin
    for v_request_id in
        select distinct n.request_id
        from new_rows n
        join old_rows o on o.id = n.id
        where o.read_at is null
          and n.read_at is not null
    loop
        perform realtime.send(
            '{}'::jsonb,
            'conversation_changed',
            'conversation:' || v_request_id::text,
            true
        );
    end loop;
    return null;
end;
$$;

revoke all on function public.notify_conversation_read() from public, anon, authenticated;

create trigger messages_notify_read
    after update on public.messages
    referencing old table as old_rows new table as new_rows
    for each statement
    execute function public.notify_conversation_read();

-- Only the two parties of the service may listen. The topic is compared as text,
-- so a malformed topic matches nothing instead of failing a cast.
create policy conversation_listen on realtime.messages
    for select to authenticated
    using (
        extension = 'broadcast'
        and exists (
            select 1
            from public.services s
            where 'conversation:' || s.request_id::text = realtime.topic()
              and (
                  s.patient_id = (select auth.uid())
                  or s.professional_id = (select auth.uid())
              )
        )
    );

-- ---------------------------------------------------------------------------
-- 6. The push for a new message. MESSAGE_RECEIVED reaches the other party of the
--    service with the sender's name and never the content (RF-09.6, RF-09.10).
--    request_push_body gains p_message_id and is rewritten whole with all eight
--    branches, because NotificationEventParityTest only scans the last migration
--    that defines it. The three-argument version is dropped: two signatures would
--    make every three-argument call ambiguous.
-- ---------------------------------------------------------------------------

drop function public.request_push_body(text, uuid, uuid);

create function public.request_push_body(
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

-- Queued on the message insert, so it rides the same transaction: a rollback
-- discards the queued pg_net row. A failure never fails the write
-- (docs/decisions.md, 2026-10-16).
create or replace function public.queue_message_push()
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

    v_body := public.request_push_body('MESSAGE_RECEIVED', new.request_id, null, new.id);
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
    raise warning 'message push not queued for message %: %', new.id, sqlerrm;
    return null;
end;
$$;

revoke all on function public.queue_message_push() from public, anon, authenticated;

create trigger messages_push_insert
    after insert on public.messages
    for each row
    execute function public.queue_message_push();
