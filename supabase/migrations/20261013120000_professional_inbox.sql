-- HU-14: the professional's open inbox. A verified professional sees the
-- immediate requests published inside their coverage in real time (RF-07.4),
-- paginated (RNF-08), and an accepted one leaves the inbox because its status no
-- longer matches (RN-04).
--
-- service_requests, the coverage function professional_covers and the inbox
-- policy already exist. This migration adds:
--   a) the restoration of the own-request exclusion the inbox policy lost in
--      20261012120000 (a regression of RN-13/INV-16);
--   b) the read projection the professional reads the inbox through, without the
--      exact location, the address text or the patient's identity;
--   c) a broadcast that tells the client to re-query, carrying no data;
--   d) the realtime.messages policy that decides who may listen.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- a) Restore the own-request exclusion on the inbox policy
-- ---------------------------------------------------------------------------

-- 20261002041901 added patient_id <> auth.uid() so a dual-role person never sees
-- their own request in the professional inbox (RN-13, INV-16).
-- 20261012120000 recreated this policy to add the time guard and dropped that
-- condition. Both guards belong here, together.
drop policy service_requests_select_inbox on public.service_requests;

create policy service_requests_select_inbox on public.service_requests
    for select to authenticated
    using (
        modality = 'IMMEDIATE'
        and status in ('PUBLISHED', 'NEGOTIATING')
        and patient_id <> (select auth.uid())
        and (expires_at is null or expires_at > now())
        and public.professional_covers(location, service_type_id)
    );

-- ---------------------------------------------------------------------------
-- b) immediate_inbox: what the professional reads, without the geography or the
--    patient's identity
-- ---------------------------------------------------------------------------

-- security definer and not a view because the distance is measured from the
-- caller's own base, which a plain view could not resolve per row, and because
-- the projection must decide with the same coverage rule the policy uses.
-- professional_covers already folds in "approved and active", "inside the
-- declared radius" and "offers this service type", so a type the professional
-- does not provide, a request out of radius and an unverified caller all fall
-- out here, exactly as in the policy.
--
-- The distance is rounded to the hundred metres, like search_nearby_professionals
-- (docs/decisions.md, 2026-10-11). The origin here is the professional's own
-- fixed base, not a free parameter the caller can sweep, so the bisection channel
-- HU-11 closed does not apply; the rounding is the same defence in depth. The
-- exact location, the address text, the reference and the patient's id never
-- leave the function: before acceptance the professional decides on distance and
-- type, not on where the patient lives (RF-08.6).
create or replace function public.immediate_inbox(
    p_limit int default 20,
    p_offset int default 0
)
returns table (
    id uuid,
    service_type_name text,
    city text,
    description text,
    suggested_budget_bob numeric,
    distance_m double precision,
    status public.request_status,
    created_at timestamptz
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
        r.created_at
    from public.service_requests r
    join public.service_types t on t.id = r.service_type_id
    join public.addresses base
        on base.profile_id = (select auth.uid())
       and base.is_professional_base
    where r.modality = 'IMMEDIATE'
      and r.status in ('PUBLISHED', 'NEGOTIATING')
      and r.patient_id <> (select auth.uid())
      and (r.expires_at is null or r.expires_at > now())
      and public.professional_covers(r.location, r.service_type_id)
    -- The newest on top: a professional watches for what just arrived.
    order by r.created_at desc, r.id asc
    -- RNF-08: a short page ends the list, a negative bound clamps instead of
    -- reaching Postgres, mirroring search_nearby_professionals.
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

revoke all on function public.immediate_inbox(int, int) from public;
grant execute on function public.immediate_inbox(int, int) to authenticated;

-- ---------------------------------------------------------------------------
-- c) The broadcast that tells covering clients to re-query
-- ---------------------------------------------------------------------------

-- supabase-kt does not expose the per-subscription column filter the JS client
-- has, so Postgres Changes cannot both filter by coverage (its RLS needs the
-- location in the WAL record) and keep that location off the socket. A broadcast
-- carrying no row does both: the client treats it as "re-query the inbox", and
-- immediate_inbox is what actually filters and projects. See docs/decisions.md,
-- 2026-10-13. The payload is empty on purpose; the event name is the whole
-- message.
create or replace function public.notify_immediate_inbox()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform realtime.send(
        '{}'::jsonb,
        'inbox_changed',
        'immediate_inbox',
        true
    );
    return null;
end;
$$;

-- Two triggers over one function: a WHEN on a combined insert-or-update trigger
-- cannot read OLD, and the update only needs to ping when the status moved (the
-- pg_cron sweep flipping a request to EXPIRED is such a move, so it disappears
-- live too). The insert always pings: a brand new immediate request is the case
-- the criterion is about.
create trigger service_requests_notify_inbox_insert
    after insert on public.service_requests
    for each row
    when (new.modality = 'IMMEDIATE')
    execute function public.notify_immediate_inbox();

create trigger service_requests_notify_inbox_update
    after update on public.service_requests
    for each row
    when (new.modality = 'IMMEDIATE' and old.status is distinct from new.status)
    execute function public.notify_immediate_inbox();

-- ---------------------------------------------------------------------------
-- d) Who may listen on the immediate_inbox topic
-- ---------------------------------------------------------------------------

-- realtime.messages already has row level security enabled by the platform;
-- running "alter table realtime.messages enable row level security" here would
-- fail with 42501 (not owner) and abort the whole migration. supautils lets the
-- postgres role add a policy to it without owning it. Only an approved
-- professional may receive the inbox broadcast; professional_directory is the
-- established "is an approved professional" test (it is the same existence check
-- request_offers_insert_participants uses). The topic carries no data, so this
-- controls who is told to re-query, not what they are told.
create policy immediate_inbox_listen on realtime.messages
    for select to authenticated
    using (
        extension = 'broadcast'
        and realtime.topic() = 'immediate_inbox'
        and exists (
            select 1
            from public.professional_directory d
            where d.id = (select auth.uid())
        )
    );
