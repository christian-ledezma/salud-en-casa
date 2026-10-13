-- HU-13: a patient publishes an immediate service request that nearby
-- professionals can see (RF-07.1, RF-07.2), it expires on its own when nobody
-- offers within a configurable window (RF-07.5), and the patient cancels it
-- while it is still open (RF-07.6, already allowed by guard_request_lifecycle).
--
-- The service_requests table, the snapshot guard (INV-03) and the lifecycle
-- guard already exist from 20260911120300_requests.sql and
-- 20260912110000_close_lifecycle_and_visibility_gaps.sql. This migration adds
-- the configurable window, the sanctioned creation path, the read projection,
-- the time guard on the inbox and the expiry sweep.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- platform_settings: the one place the configurable request window lives
-- ---------------------------------------------------------------------------

-- RF-07.5 calls the expiry window "configurable". A single-row settings table
-- is the honest reading of that: the value is data, not a constant recompiled
-- into a function. The boolean primary key pins it to exactly one row. The
-- commission percentage of Sprint 8 will add a column here rather than invent a
-- second mechanism. See docs/decisions.md, 2026-10-12.
create table public.platform_settings (
    id boolean primary key default true check (id),
    immediate_request_ttl_minutes int not null default 30
        check (immediate_request_ttl_minutes > 0)
);

insert into public.platform_settings (id) values (true);

-- INV-02: row level security on from the migration that creates the table. The
-- window is not a secret, so any signed-in user may read it; it is maintained
-- by migration, so no write policy is granted to clients.
alter table public.platform_settings enable row level security;

create policy platform_settings_select_all on public.platform_settings
    for select to authenticated
    using (true);

-- ---------------------------------------------------------------------------
-- create_immediate_request: the only path an immediate request is born through
-- ---------------------------------------------------------------------------

-- security definer so the snapshot is frozen from a real address the caller
-- owns (INV-03) and expires_at is set from the configured window on the server,
-- never from a value the client could spoof. It mirrors the atomic acceptance
-- RF-08.5 will need. See docs/decisions.md, 2026-10-12.
-- The budget arrives as text, not numeric: PostgREST maps a JSON number onto a
-- numeric argument through a double, and money read back from a double is not
-- the money that was typed. The text is cast once, inside, where the column's
-- own numeric(10,2) and its > 0 check finish the job.
create or replace function public.create_immediate_request(
    p_address_id uuid,
    p_service_type_id uuid,
    p_description text,
    p_suggested_budget_bob text
)
returns uuid
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_location extensions.geography;
    v_address_text text;
    v_city text;
    v_reference text;
    v_ttl int;
    v_request_id uuid;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    if p_description is not null and char_length(p_description) > 1000 then
        raise exception 'description_too_long';
    end if;

    -- The snapshot comes from an address the caller owns, never from a point
    -- the client sends. A missing row is both "no such address" and "not
    -- yours": the policy on addresses would hide someone else's row anyway, but
    -- this runs as definer, so the ownership check is explicit here.
    select a.location, a.address_text, a.city, a.reference
    into v_location, v_address_text, v_city, v_reference
    from public.addresses a
    where a.id = p_address_id
      and a.profile_id = v_caller;

    if not found then
        raise exception 'address_not_owned';
    end if;

    if not exists (
        select 1
        from public.service_types t
        where t.id = p_service_type_id
          and t.active
    ) then
        raise exception 'service_type_unavailable';
    end if;

    select immediate_request_ttl_minutes
    into v_ttl
    from public.platform_settings
    where id;

    insert into public.service_requests (
        patient_id,
        service_type_id,
        address_id,
        location,
        address_text,
        city,
        reference,
        modality,
        description,
        suggested_budget_bob,
        expires_at
    ) values (
        v_caller,
        p_service_type_id,
        p_address_id,
        v_location,
        v_address_text,
        v_city,
        v_reference,
        'IMMEDIATE',
        p_description,
        nullif(p_suggested_budget_bob, '')::numeric,
        now() + make_interval(mins => v_ttl)
    )
    returning id into v_request_id;

    return v_request_id;
end;
$$;

revoke all on function public.create_immediate_request(uuid, uuid, text, text) from public;
grant execute on function public.create_immediate_request(uuid, uuid, text, text) to authenticated;

-- ---------------------------------------------------------------------------
-- An immediate request can only be born through the function above
-- ---------------------------------------------------------------------------

-- The definer function bypasses row level security, so tightening this policy
-- does not block it; it blocks a direct client insert that would set an
-- arbitrary location and expires_at, which is exactly what the snapshot and the
-- server-set window exist to prevent. The scheduled modality keeps the direct
-- path open until HU-16 decides its own creation route.
drop policy service_requests_insert_own on public.service_requests;

create policy service_requests_insert_own on public.service_requests
    for insert to authenticated
    with check (
        patient_id = (select auth.uid())
        and modality <> 'IMMEDIATE'
    );

-- ---------------------------------------------------------------------------
-- my_service_requests: what the patient reads back, without the geography hex
-- ---------------------------------------------------------------------------

-- Reading service_requests directly would hand the client the PostGIS
-- hexadecimal encoding of location. This projects the point as two numbers and
-- joins the service type name the list needs.
--
-- It is a definer view (security_invoker = false) and its WHERE is the rule,
-- like professional_directory: a security_invoker view would apply
-- service_types_select_all to the join, so a catalog type an administrator
-- deactivated would make the patient's own request vanish from their list. The
-- definer reads the name past that policy; auth.uid() still returns the caller,
-- so the ownership filter is honest. The join makes it non-updatable; the revoke
-- that names authenticated and not only anon is the belt and braces supabase.md
-- requires for a definer view.
create view public.my_service_requests
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
    r.created_at
from public.service_requests r
join public.service_types t on t.id = r.service_type_id
where r.patient_id = (select auth.uid());

revoke all on public.my_service_requests from anon, authenticated;
grant select on public.my_service_requests to authenticated;

-- ---------------------------------------------------------------------------
-- The inbox hides a request the moment it is past due, not only once swept
-- ---------------------------------------------------------------------------

-- The sweep below runs once a minute, so without this a request would linger in
-- the inbox for up to a minute after it expired. The time guard closes that
-- window; the sweep still writes the real EXPIRED state the patient sees.
drop policy service_requests_select_inbox on public.service_requests;

create policy service_requests_select_inbox on public.service_requests
    for select to authenticated
    using (
        modality = 'IMMEDIATE'
        and status in ('PUBLISHED', 'NEGOTIATING')
        and (expires_at is null or expires_at > now())
        and public.professional_covers(location, service_type_id)
    );

-- ---------------------------------------------------------------------------
-- The sweep: a past-due immediate request with no offers becomes EXPIRED
-- ---------------------------------------------------------------------------

-- RF-07.5: "queda expirada" is a real state transition, not a value derived at
-- read time, so a job writes it. It runs as the scheduling role with no
-- auth.uid(), which guard_request_lifecycle lets through its first branch. A
-- request that already drew an offer is a negotiation, not an abandoned one, so
-- it is left alone. See docs/decisions.md, 2026-10-12.
create extension if not exists pg_cron;

select cron.schedule(
    'expire_immediate_requests',
    '* * * * *',
    $$
    update public.service_requests
       set status = 'EXPIRED'
     where modality = 'IMMEDIATE'
       and status = 'PUBLISHED'
       and expires_at < now()
       and not exists (
           select 1
           from public.request_offers o
           where o.request_id = service_requests.id
       )
    $$
);
