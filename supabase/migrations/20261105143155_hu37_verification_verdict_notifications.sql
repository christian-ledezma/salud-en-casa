-- HU-37: whoever uploaded a document is told when the administrator rules on
-- it, and a professional is told when they become APPROVED. Neither event
-- belongs to a service_requests row, unlike every push before this one.
-- docs/decisions.md, 2026-11-05, HU-37.

-- ---------------------------------------------------------------------------
-- 1. One push per verdict, not one per document. A joint verdict is a single
--    update statement over several rows, so the trigger fires once per
--    statement and groups by profile_id, the same shape as
--    notify_conversation_read.
-- ---------------------------------------------------------------------------

create or replace function public.queue_verification_document_push()
returns trigger
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_profile_id uuid;
    v_status text;
    v_count integer;
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

    for v_profile_id, v_status, v_count in
        select n.profile_id, n.status, count(*)
        from new_rows n
        join old_rows o on o.id = n.id
        where o.status is distinct from n.status
        group by n.profile_id, n.status
    loop
        v_body := public.request_push_body(
            p_event := 'DOCUMENT_VERDICT',
            p_profile_id := v_profile_id,
            p_document_count := v_count,
            p_status := v_status
        );
        if v_body is null then
            continue;
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
    end loop;

    return null;
exception when others then
    -- docs/decisions.md, 2026-10-16: a notification never fails the verdict.
    raise warning 'verification push not queued: %', sqlerrm;
    return null;
end;
$$;

revoke all on function public.queue_verification_document_push() from public, anon, authenticated;

create trigger verification_documents_push_verdict
    after update on public.verification_documents
    referencing old table as old_rows new table as new_rows
    for each statement
    execute function public.queue_verification_document_push();

-- ---------------------------------------------------------------------------
-- 2. The professional learns they are now searchable. Only the move into
--    APPROVED notifies; approve_professional_verification is the only writer.
-- ---------------------------------------------------------------------------

create or replace function public.queue_verification_approved_push()
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

    v_body := public.request_push_body(p_event := 'VERIFICATION_APPROVED', p_profile_id := new.id);
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
    raise warning 'verification push not queued for professional %: %', new.id, sqlerrm;
    return null;
end;
$$;

revoke all on function public.queue_verification_approved_push() from public, anon, authenticated;

create trigger professionals_push_verification_approved
    after update of verification_status on public.professionals
    for each row
    when (new.verification_status = 'APPROVED' and old.verification_status is distinct from 'APPROVED')
    execute function public.queue_verification_approved_push();

-- ---------------------------------------------------------------------------
-- 3. request_push_body gains the two events above, and three parameters that
--    name what they are about instead of a request. Neither has one, so the
--    lookup every other event relies on moves behind a branch.
--    NotificationEventParityTest only scans the last migration that defines
--    it, so it is rewritten whole. The four-argument version is dropped: two
--    signatures would make every four-argument call ambiguous.
-- ---------------------------------------------------------------------------

drop function public.request_push_body(text, uuid, uuid, uuid);

create function public.request_push_body(
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

revoke all on function public.request_push_body(text, uuid, uuid, uuid, uuid, integer, text) from public, anon, authenticated;
