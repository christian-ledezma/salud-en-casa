-- HU-20: request events queue a push delivery through pg_net to the send-push
-- function. docs/decisions.md, 2026-10-16, "el motor decide y encola".

set search_path = public, extensions;

create extension if not exists pg_net;

-- ---------------------------------------------------------------------------
-- Recipients of an immediate request
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
      and public.professional_covers_for(pro.id, r.location, r.service_type_id);
$$;

revoke all on function public.immediate_request_recipients(uuid) from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- The delivery for one event
-- ---------------------------------------------------------------------------

-- java.time.Instant.parse rejects PostgreSQL's +00:00, hence the literal Z.
create or replace function public.request_push_body(p_event text, p_request_id uuid)
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
        'ttlSeconds', v_ttl,
        'targets', v_targets
    ));
end;
$$;

revoke all on function public.request_push_body(text, uuid) from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- The trigger
-- ---------------------------------------------------------------------------

create or replace function public.queue_request_push()
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
    if tg_op = 'INSERT' then
        v_event := case when new.modality = 'IMMEDIATE' then 'REQUEST_NEARBY' else 'APPOINTMENT_SCHEDULED' end;
    elsif new.status = 'EXPIRED' then
        v_event := 'REQUEST_EXPIRED';
    elsif new.status = 'CANCELLED'
          and new.modality = 'SCHEDULED'
          and old.status in ('PUBLISHED', 'NEGOTIATING') then
        v_event := 'APPOINTMENT_CANCELLED';
    else
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

    v_body := public.request_push_body(v_event, new.id);
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
    -- docs/decisions.md, 2026-10-16: a notification never fails the request.
    raise warning 'push not queued for request %: %', new.id, sqlerrm;
    return null;
end;
$$;

revoke all on function public.queue_request_push() from public, anon, authenticated;

create trigger service_requests_push_insert
    after insert on public.service_requests
    for each row
    execute function public.queue_request_push();

create trigger service_requests_push_update
    after update of status on public.service_requests
    for each row
    when (old.status is distinct from new.status and new.status in ('CANCELLED', 'EXPIRED'))
    execute function public.queue_request_push();
