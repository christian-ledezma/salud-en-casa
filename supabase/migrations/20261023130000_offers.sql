-- HU-17: a professional offers on a request, the patient sees it, and the first
-- offer moves the request to NEGOTIATING. This migration also settles the two
-- debts HU-16 left. docs/decisions.md, 2026-10-23.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. Debt 1 and 2 of HU-16: whether a professional may offer is decided by a
--    definer, so service_requests_select_addressee can be retired.
-- ---------------------------------------------------------------------------

-- Answers a boolean about the caller's own eligibility to offer, reading the
-- request past its policies so the offer policy no longer needs to. It returns
-- neither location nor identity, the same shape of answer as
-- offer_continues_thread (20261002135541). The scheduled branch decides by
-- addressee, not by coverage: coverage was decided at creation against the
-- rounded base, and re-deciding it here against the exact base is the ~80 m
-- edge defect HU-16 flagged (docs/decisions.md, 2026-10-23).
create or replace function public.professional_may_offer_on(p_request_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.service_requests r
        where r.id = p_request_id
          and r.status in ('PUBLISHED', 'NEGOTIATING')
          -- RN-13, INV-16: never an offer on one's own request.
          and r.patient_id <> (select auth.uid())
          -- RN-01: only an approved, active professional.
          and exists (
              select 1
              from public.professional_directory d
              where d.id = (select auth.uid())
          )
          and (
              (
                  r.modality = 'IMMEDIATE'
                  and (r.expires_at is null or r.expires_at > now())
                  and public.professional_covers_for(
                      (select auth.uid()), r.location, r.service_type_id)
              )
              or (
                  r.modality = 'SCHEDULED'
                  and r.professional_id = (select auth.uid())
                  and r.scheduled_at > now()
              )
          )
    );
$$;

revoke all on function public.professional_may_offer_on(uuid) from public, anon;
grant execute on function public.professional_may_offer_on(uuid) to authenticated;

-- The professional branch no longer subqueries service_requests, so it no
-- longer depends on a select policy over that table. The patient branch (a
-- counter-offer, exercised in HU-18) still reads the row, but through
-- select_own, which stays.
drop policy request_offers_insert_participants on public.request_offers;

create policy request_offers_insert_participants on public.request_offers
    for insert to authenticated
    with check (
        (
            request_offers.issuer = 'PROFESSIONAL'
            and request_offers.professional_id = (select auth.uid())
            and public.professional_may_offer_on(request_offers.request_id)
        )
        or (
            request_offers.issuer = 'PATIENT'
            and exists (
                select 1
                from public.service_requests r
                where r.id = request_offers.request_id
                  and r.patient_id = (select auth.uid())
                  and r.status in ('PUBLISHED', 'NEGOTIATING')
            )
            and request_offers.parent_offer_id is not null
            and public.offer_continues_thread(
                request_offers.parent_offer_id,
                request_offers.request_id,
                request_offers.professional_id
            )
        )
    );

-- The addressee no longer reads the whole scheduled request through the API:
-- its exact location, address, reference and the patient's identity stayed
-- readable until acceptance, against RF-08.6. professional_may_offer_on now
-- carries the only read the offer policy needed.
drop policy service_requests_select_addressee on public.service_requests;

-- ---------------------------------------------------------------------------
-- 2. The first offer moves the request to NEGOTIATING.
-- ---------------------------------------------------------------------------

-- The guard must admit PUBLISHED -> NEGOTIATING, or the trigger below would be
-- refused. It is harmless: it assigns no professional and creates no service,
-- and ACCEPTED plus the professional_id change stay reserved for the atomic
-- operation of HU-19. docs/decisions.md, 2026-10-23.
create or replace function public.guard_request_lifecycle()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        return new;
    end if;

    if new.professional_id is distinct from old.professional_id then
        raise exception 'request_professional_is_set_at_creation';
    end if;

    if new.status is distinct from old.status then
        if not (
            (new.status = 'CANCELLED' and old.status in ('PUBLISHED', 'NEGOTIATING'))
            or (old.status = 'PUBLISHED' and new.status = 'NEGOTIATING')
        ) then
            raise exception 'request_status_change_requires_the_atomic_operation';
        end if;
    end if;

    return new;
end;
$$;

create or replace function public.move_request_to_negotiating()
returns trigger
language plpgsql
volatile
security definer
set search_path = ''
as $$
begin
    update public.service_requests
       set status = 'NEGOTIATING'
     where id = new.request_id
       and status = 'PUBLISHED';
    return null;
end;
$$;

revoke all on function public.move_request_to_negotiating() from public, anon, authenticated;

create trigger request_offers_move_to_negotiating
    after insert on public.request_offers
    for each row
    execute function public.move_request_to_negotiating();

-- ---------------------------------------------------------------------------
-- 3. inbox_request: one row for the professional's detail screen, plus whether
--    they already offered. Same projection as request_inbox, so the exact
--    location, the address and the patient's identity never leave.
-- ---------------------------------------------------------------------------

create or replace function public.inbox_request(p_request_id uuid)
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
    already_offered boolean
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
        ) as already_offered
    from public.service_requests r
    join public.service_types t on t.id = r.service_type_id
    join public.addresses base
        on base.profile_id = (select auth.uid())
       and base.is_professional_base
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

-- ---------------------------------------------------------------------------
-- 4. my_request_offers: the offers on the caller's own request, joined to the
--    public projection of the professional (never the professionals table).
-- ---------------------------------------------------------------------------

create or replace function public.my_request_offers(p_request_id uuid)
returns table (
    id uuid,
    professional_id uuid,
    professional_name text,
    professional_photo_url text,
    professional_rating numeric,
    amount_bob numeric,
    message text,
    issuer public.offer_issuer,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        o.id,
        o.professional_id,
        d.full_name as professional_name,
        d.photo_url as professional_photo_url,
        d.average_rating as professional_rating,
        o.amount_bob,
        o.message,
        o.issuer,
        o.created_at
    from public.request_offers o
    join public.service_requests r on r.id = o.request_id
    left join public.professional_directory d on d.id = o.professional_id
    where o.request_id = p_request_id
      and r.patient_id = (select auth.uid())
    order by o.created_at asc, o.id asc;
$$;

revoke all on function public.my_request_offers(uuid) from public, anon;
grant execute on function public.my_request_offers(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 5. The push for a new professional offer. OFFER_RECEIVED joins the channel of
--    20261016122000. The body carries the service type and the amount, never
--    the offer's message (RF-09.10): the amount is structured data, like the
--    distance, not text a person wrote.
-- ---------------------------------------------------------------------------

-- The third parameter identifies the offer. request_push_body is rewritten whole
-- with all five branches, because NotificationEventParityTest only scans the last
-- migration that defines it. The two-argument form is dropped so the existing
-- two-argument calls in queue_request_push bind to this one through the default.
drop function if exists public.request_push_body(text, uuid);

create function public.request_push_body(
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
        select o.amount_bob into v_amount
        from public.request_offers o
        where o.id = p_offer_id;
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
        'amountBob', case when p_event = 'OFFER_RECEIVED' then v_amount::text end,
        'ttlSeconds', v_ttl,
        'targets', v_targets
    ));
end;
$$;

revoke all on function public.request_push_body(text, uuid, uuid) from public, anon, authenticated;

create or replace function public.queue_offer_push()
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

    v_body := public.request_push_body('OFFER_RECEIVED', new.request_id, new.id);
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
    -- docs/decisions.md, 2026-10-16: a notification never fails the write.
    raise warning 'offer push not queued for offer %: %', new.id, sqlerrm;
    return null;
end;
$$;

revoke all on function public.queue_offer_push() from public, anon, authenticated;

create trigger request_offers_push_insert
    after insert on public.request_offers
    for each row
    when (new.issuer = 'PROFESSIONAL')
    execute function public.queue_offer_push();

-- ---------------------------------------------------------------------------
-- 6. The patient re-queries live when an offer lands. Broadcast without data,
--    per patient, the pattern of 20261013120000 (docs/decisions.md, 2026-10-13).
-- ---------------------------------------------------------------------------

create or replace function public.notify_patient_offers()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_patient_id uuid;
begin
    select patient_id into v_patient_id
    from public.service_requests
    where id = new.request_id;

    perform realtime.send(
        '{}'::jsonb,
        'offers_changed',
        'patient_offers:' || v_patient_id::text,
        true
    );
    return null;
end;
$$;

create trigger request_offers_notify_patient
    after insert on public.request_offers
    for each row
    execute function public.notify_patient_offers();

-- supautils lets the postgres role add a policy to realtime.messages without
-- owning it. The topic carries no data; this controls who is told to re-query.
-- Each patient may listen only on their own topic.
create policy patient_offers_listen on realtime.messages
    for select to authenticated
    using (
        extension = 'broadcast'
        and realtime.topic() = 'patient_offers:' || (select auth.uid())::text
    );
