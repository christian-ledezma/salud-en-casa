-- HU-18: the patient answers with another amount, the thread is linear and
-- taken in turns, rejecting closes it, and each side is told of the other's
-- answer both live and by push. docs/decisions.md, 2026-10-27.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. The thread is linear and taken in turns.
-- ---------------------------------------------------------------------------

-- The three-argument form required the parent to be issued by the professional,
-- so it served the patient's counter-offer but not the professional's answer to
-- it, which entered through its own branch with nothing looking at the parent.
-- The fourth argument names the issuer each branch expects. Two conditions join
-- it: the parent must still be PROPOSED, so a rejection closes the chain, and it
-- must not have been answered already, so the thread stays a single line and each
-- side waits its turn. "Already answered" is the parent having a child, not a
-- comparison of created_at: two rows written in the same transaction carry the
-- same now(), so time does not order a chain, and the one that answers is the one
-- that points at it.
drop policy request_offers_insert_participants on public.request_offers;

drop function public.offer_continues_thread(uuid, uuid, uuid);

create function public.offer_continues_thread(
    p_parent_offer_id uuid,
    p_request_id uuid,
    p_professional_id uuid,
    p_parent_issuer public.offer_issuer
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.request_offers parent
        where parent.id = p_parent_offer_id
          and parent.request_id = p_request_id
          and parent.professional_id = p_professional_id
          and parent.issuer = p_parent_issuer
          and parent.status = 'PROPOSED'
          and not exists (
              select 1
              from public.request_offers answer
              where answer.parent_offer_id = parent.id
          )
    );
$$;

revoke all on function public.offer_continues_thread(uuid, uuid, uuid, public.offer_issuer) from public, anon;
grant execute on function public.offer_continues_thread(uuid, uuid, uuid, public.offer_issuer) to authenticated;

-- A definer and not a subquery inside the policy: reading request_offers from
-- within its own policy is the recursion of 20261002135541.
create or replace function public.professional_already_offered_on(p_request_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.request_offers o
        where o.request_id = p_request_id
          and o.professional_id = (select auth.uid())
    );
$$;

revoke all on function public.professional_already_offered_on(uuid) from public, anon;
grant execute on function public.professional_already_offered_on(uuid) to authenticated;

-- An offer is born PROPOSED and without an expiry, whoever writes it. Until now
-- the policy did not look at status, and the guards that reserve ACCEPTED are
-- BEFORE UPDATE, so a client could have inserted a row already accepted and
-- walked around the atomic operation HU-19 is going to build.
create or replace function public.stamp_offer_created_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    -- The thread is read in the order the engine recorded, so the clock is the
    -- engine's. A definer or the scheduler, which have no caller, keep theirs.
    if (select auth.uid()) is not null then
        new.created_at := now();
    end if;
    return new;
end;
$$;

create trigger request_offers_stamp_created_at
    before insert on public.request_offers
    for each row execute function public.stamp_offer_created_at();

-- Each branch now validates the parent it expects. The professional's first
-- offer is one per request, the rule the screen already promised and the engine
-- did not enforce; their answer continues the thread.
create policy request_offers_insert_participants on public.request_offers
    for insert to authenticated
    with check (
        request_offers.status = 'PROPOSED'
        and request_offers.expires_at is null
        and (
        (
            request_offers.issuer = 'PROFESSIONAL'
            and request_offers.professional_id = (select auth.uid())
            and public.professional_may_offer_on(request_offers.request_id)
            and (
                (
                    request_offers.parent_offer_id is null
                    and not public.professional_already_offered_on(request_offers.request_id)
                )
                or (
                    request_offers.parent_offer_id is not null
                    and public.offer_continues_thread(
                        request_offers.parent_offer_id,
                        request_offers.request_id,
                        request_offers.professional_id,
                        'PATIENT'
                    )
                )
            )
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
                request_offers.professional_id,
                'PROFESSIONAL'
            )
        )
        )
    );

-- The two rules above are read-then-write, so under two writers at once both
-- reads can pass. These indexes are what makes them true: one answer per offer,
-- so the thread is a line and not a tree, and one first offer per professional
-- and request.
create unique index idx_request_offers_one_answer_per_parent
    on public.request_offers (parent_offer_id)
    where parent_offer_id is not null;

create unique index idx_request_offers_one_first_offer
    on public.request_offers (request_id, professional_id)
    where parent_offer_id is null;

-- ---------------------------------------------------------------------------
-- 2. Rejecting an offer.
-- ---------------------------------------------------------------------------

-- request_offers_update_participants had with check (true): either participant
-- could write any status but ACCEPTED, including taking a rejected offer back to
-- PROPOSED, and could write expires_at. HU-18 is the first story to write status
-- from the client, so the policy is narrowed to PROPOSED -> REJECTED by the
-- counterparty of the issuer. It does not stand in the way of HU-19: the atomic
-- acceptance will run in a definer, which no policy applies to.
drop policy request_offers_update_participants on public.request_offers;

create policy request_offers_reject_counterparty on public.request_offers
    for update to authenticated
    using (
        request_offers.status = 'PROPOSED'
        and (
            (
                request_offers.issuer = 'PROFESSIONAL'
                and exists (
                    select 1
                    from public.service_requests r
                    where r.id = request_offers.request_id
                      and r.patient_id = (select auth.uid())
                )
            )
            or (
                request_offers.issuer = 'PATIENT'
                and request_offers.professional_id = (select auth.uid())
            )
        )
    )
    with check (request_offers.status = 'REJECTED');

-- What is rejected is the offer on the table. Rejecting one that was already
-- answered would leave a chain with a rejected row in the middle and a live one
-- at the end, a state no screen can tell the person about. The condition lives in
-- a trigger and not in the policy, because reading request_offers from inside its
-- own policy is the recursion of 20261002135541.
create or replace function public.guard_offer_rejection()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.status = 'REJECTED' and old.status is distinct from new.status
        and exists (
            select 1
            from public.request_offers answer
            where answer.parent_offer_id = old.id
        ) then
        raise exception 'answered_offer_cannot_be_rejected';
    end if;
    return new;
end;
$$;

create trigger request_offers_guard_rejection
    before update on public.request_offers
    for each row execute function public.guard_offer_rejection();

-- With the policy above, status is the only column the client needs to write.
-- expires_at joins the frozen ones: nothing fills it (docs/decisions.md,
-- 2026-10-23), and leaving it writable left a column no rule governed.
create or replace function public.guard_offer_is_append_only()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.id is distinct from old.id
        or new.request_id is distinct from old.request_id
        or new.professional_id is distinct from old.professional_id
        or new.parent_offer_id is distinct from old.parent_offer_id
        or new.issuer is distinct from old.issuer
        or new.amount_bob is distinct from old.amount_bob
        or new.message is distinct from old.message
        or new.expires_at is distinct from old.expires_at
        or new.created_at is distinct from old.created_at then
        raise exception 'issued_offer_cannot_be_edited';
    end if;
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- 3. my_request_offers carries the thread: the parent and the outcome.
-- ---------------------------------------------------------------------------

-- The professional gains no function: request_offers_select_participants already
-- lets them read the rows of their own thread, counter-offers included, and that
-- read carries nothing about the patient (RF-08.6).
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
    created_at timestamptz
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
-- 4. Live in both directions. The professional gains the mirror topic of the
--    patient's, the pattern of docs/decisions.md, 2026-10-23.
-- ---------------------------------------------------------------------------

create or replace function public.notify_offer_parties()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_patient_id uuid;
    v_tell_patient boolean;
begin
    if tg_op = 'INSERT' then
        v_tell_patient := new.issuer = 'PROFESSIONAL';
    else
        -- A rejection is written by the counterparty, so the one who has to
        -- re-query is the issuer of the offer that was rejected.
        v_tell_patient := new.issuer = 'PATIENT';
    end if;

    if v_tell_patient then
        select patient_id into v_patient_id
        from public.service_requests
        where id = new.request_id;

        perform realtime.send(
            '{}'::jsonb,
            'offers_changed',
            'patient_offers:' || v_patient_id::text,
            true
        );
    else
        perform realtime.send(
            '{}'::jsonb,
            'offers_changed',
            'professional_offers:' || new.professional_id::text,
            true
        );
    end if;
    return null;
end;
$$;

revoke all on function public.notify_offer_parties() from public, anon, authenticated;

drop trigger request_offers_notify_patient on public.request_offers;
drop function public.notify_patient_offers();

create trigger request_offers_notify_insert
    after insert on public.request_offers
    for each row
    execute function public.notify_offer_parties();

create trigger request_offers_notify_status
    after update of status on public.request_offers
    for each row
    when (old.status is distinct from new.status)
    execute function public.notify_offer_parties();

-- The mirror of patient_offers_listen (20261023130000). The topic carries no
-- data; this controls who is told to re-query, and each professional may listen
-- only on their own topic.
create policy professional_offers_listen on realtime.messages
    for select to authenticated
    using (
        extension = 'broadcast'
        and realtime.topic() = 'professional_offers:' || (select auth.uid())::text
    );

-- ---------------------------------------------------------------------------
-- 5. The push for a counter-offer. COUNTER_OFFER_RECEIVED is its own event
--    (RF-09.5) and its recipient is the counterparty of the issuer, so the same
--    event reaches a patient or a professional. It carries the amount and the
--    professional of the thread, which is what the phone opens the thread with,
--    never the offer's message (RF-09.10).
-- ---------------------------------------------------------------------------

-- Rewritten whole with all six branches, because NotificationEventParityTest
-- only scans the last migration that defines it.
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
        select o.amount_bob into v_amount
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
        'professionalId', case
            when p_event = 'COUNTER_OFFER_RECEIVED' then v_professional_id::text
        end,
        'ttlSeconds', v_ttl,
        'targets', v_targets
    ));
end;
$$;

revoke all on function public.request_push_body(text, uuid, uuid) from public, anon, authenticated;

-- The event comes out of the row: a first offer is OFFER_RECEIVED and goes to
-- the patient; one with a parent is COUNTER_OFFER_RECEIVED and goes to the other
-- side. The trigger no longer filters by issuer, because the patient's
-- counter-offer is exactly what has to reach the professional.
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

    v_body := public.request_push_body(
        case
            when new.parent_offer_id is null then 'OFFER_RECEIVED'
            else 'COUNTER_OFFER_RECEIVED'
        end,
        new.request_id,
        new.id
    );
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

drop trigger request_offers_push_insert on public.request_offers;

create trigger request_offers_push_insert
    after insert on public.request_offers
    for each row
    execute function public.queue_offer_push();
