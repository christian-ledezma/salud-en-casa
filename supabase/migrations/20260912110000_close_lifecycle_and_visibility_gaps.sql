-- Closes the gaps found by the review on pull request #1, verified individually
-- against the live remote database before being written here. Grouped by area;
-- see docs/decisions.md, 2026-09-12, for the reasoning behind each one.
--
-- Migrations already applied are never edited (see docs/decisions.md); this is
-- a new migration on top of identity, catalog, requests, delivery, functions,
-- extensions_and_types and the earlier hardening migration.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. The trigger depth guard from the previous hardening migration blocked the
-- legitimate write it was meant to allow through.
--
-- pg_trigger_depth() reports one less inside a trigger's own WHEN clause than
-- inside its function body: verified against this database with a throwaway
-- nested-trigger experiment before writing this fix. A direct client update
-- reads depth 0 in the WHEN clause (not 1), and the nested write issued by
-- recalculate_reputation() or increment_total_services() reads depth 1 (not
-- 2). The original `<= 1` therefore still matched the legitimate nested write
-- and raised on it, which meant submitting a review or completing a service
-- failed outright.
-- ---------------------------------------------------------------------------

create or replace trigger profiles_guard_server_managed_fields
    before update on public.profiles
    for each row
    when (not public.is_admin() and pg_trigger_depth() <= 0)
    execute function public.guard_profile_server_managed_fields();

create or replace trigger professionals_guard_server_managed_fields
    before update on public.professionals
    for each row
    when (not public.is_admin() and pg_trigger_depth() <= 0)
    execute function public.guard_professional_server_managed_fields();

-- ---------------------------------------------------------------------------
-- 2. patients_insert_own and professionals_insert_own checked only row
-- ownership, not the role chosen on profiles. A profile with role PROFESSIONAL
-- could still insert a patients row for the same id, and vice versa.
-- ---------------------------------------------------------------------------

drop policy patients_insert_own on public.patients;

create policy patients_insert_own on public.patients
    for insert to authenticated
    with check (
        id = (select auth.uid())
        and (select role from public.profiles where id = (select auth.uid())) = 'PATIENT'
    );

drop policy professionals_insert_own on public.professionals;

create policy professionals_insert_own on public.professionals
    for insert to authenticated
    with check (
        id = (select auth.uid())
        and (select role from public.profiles where id = (select auth.uid())) = 'PROFESSIONAL'
    );

-- ---------------------------------------------------------------------------
-- 3. service_requests_update_own let the request's own patient rewrite status
-- and professional_id directly, including ACCEPTED or COMPLETED with no offer
-- and no service ever created. RF-08.5 assigns that transition to a single
-- atomic operation that does not exist yet; until it does, the only status
-- change a direct client write may make is a self-cancellation allowed by
-- RF-07.6, and professional_id never changes after the row is created.
-- ---------------------------------------------------------------------------

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
        if new.status <> 'CANCELLED' or old.status not in ('PUBLISHED', 'NEGOTIATING') then
            raise exception 'request_status_change_requires_the_atomic_operation';
        end if;
    end if;

    return new;
end;
$$;

create trigger service_requests_guard_lifecycle
    before update on public.service_requests
    for each row execute function public.guard_request_lifecycle();

-- ---------------------------------------------------------------------------
-- 4. request_offers_update_participants allowed either participant to set an
-- offer straight to ACCEPTED, the same gap as above seen from the offer side.
-- Closed the same way: reserved for the atomic operation that RF-08.5
-- describes, which still needs to be built.
-- ---------------------------------------------------------------------------

create or replace function public.guard_offer_acceptance()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.status is distinct from old.status and new.status = 'ACCEPTED' then
        raise exception 'offer_acceptance_requires_the_atomic_operation';
    end if;
    return new;
end;
$$;

create trigger request_offers_guard_acceptance
    before update on public.request_offers
    for each row execute function public.guard_offer_acceptance();

-- ---------------------------------------------------------------------------
-- 5. request_offers_insert_participants let a professional offer on a request
-- outside their own coverage, bypassing the same professional_covers() check
-- the inbox policy already applies (RN-01, RN-02). It also let a patient issue
-- a standalone offer under any professional_id: RF-08.1 and RF-08.2 make the
-- professional the first mover, so a patient-issued row is always a
-- counteroffer and must reference one of that professional's offers on this
-- same request.
-- ---------------------------------------------------------------------------

drop policy request_offers_insert_participants on public.request_offers;

create policy request_offers_insert_participants on public.request_offers
    for insert to authenticated
    with check (
        exists (
            select 1
            from public.service_requests r
            where r.id = request_offers.request_id
              and r.status in ('PUBLISHED', 'NEGOTIATING')
              and (
                  (
                      request_offers.issuer = 'PROFESSIONAL'
                      and request_offers.professional_id = (select auth.uid())
                      and exists (
                          select 1
                          from public.professional_directory d
                          where d.id = (select auth.uid())
                      )
                      and public.professional_covers(r.location, r.service_type_id)
                  )
                  or (
                      request_offers.issuer = 'PATIENT'
                      and r.patient_id = (select auth.uid())
                      and request_offers.parent_offer_id is not null
                      and exists (
                          select 1
                          from public.request_offers parent
                          where parent.id = request_offers.parent_offer_id
                            and parent.request_id = request_offers.request_id
                            and parent.professional_id = request_offers.professional_id
                            and parent.issuer = 'PROFESSIONAL'
                      )
                  )
              )
        )
    );

-- ---------------------------------------------------------------------------
-- 6. services_update_participants let either party record arrival or
-- completion and let either party attribute a cancellation to someone else.
-- RF-10.1 and RF-10.2 assign arrival and completion to the professional; RF-10.3
-- lets either party cancel, but the recorded author must be the caller.
-- ---------------------------------------------------------------------------

create or replace function public.guard_service_direct_update()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
begin
    if v_caller is null then
        return new;
    end if;

    if new.status is distinct from old.status then
        if new.status in ('IN_PROGRESS', 'COMPLETED') and v_caller <> old.professional_id then
            raise exception 'arrival_and_completion_are_recorded_by_the_professional';
        end if;
        if new.status = 'CANCELLED' and new.cancelled_by is distinct from v_caller then
            raise exception 'cancellation_author_must_be_the_caller';
        end if;
    end if;

    return new;
end;
$$;

create trigger services_guard_direct_update
    before update on public.services
    for each row execute function public.guard_service_direct_update();

-- ---------------------------------------------------------------------------
-- 7. payments_update_participants let either party confirm a payment before
-- the service was COMPLETED, and let either party move a payment straight to
-- DISPUTED at will. RF-11.5 ties a dispute to mismatched confirmations, a rule
-- this project has not built yet; closing the transition to direct writes
-- entirely is the safe default until that rule exists, the same way SETTLED
-- was already closed.
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
    if v_caller is null or public.is_admin() then
        return new;
    end if;

    select s.patient_id, s.professional_id, s.status
    into v_patient, v_professional, v_service_status
    from public.services s
    where s.id = new.service_id;

    if v_caller = v_patient then
        if new.professional_confirmed_at is distinct from old.professional_confirmed_at then
            raise exception 'each_party_confirms_separately';
        end if;
    elsif v_caller = v_professional then
        if new.patient_confirmed_at is distinct from old.patient_confirmed_at then
            raise exception 'each_party_confirms_separately';
        end if;
    else
        raise exception 'not_a_party_of_this_payment';
    end if;

    if (new.patient_confirmed_at is distinct from old.patient_confirmed_at
        or new.professional_confirmed_at is distinct from old.professional_confirmed_at)
        and v_service_status <> 'COMPLETED' then
        raise exception 'payment_confirmation_requires_a_completed_service';
    end if;

    if new.status = 'DISPUTED' and old.status is distinct from 'DISPUTED' then
        raise exception 'dispute_requires_the_guarded_resolution_path';
    end if;

    if new.settled_at is distinct from old.settled_at or new.status = 'SETTLED' then
        raise exception 'settlement_is_recorded_by_an_administrator';
    end if;

    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- 8. reviews_select_visible exposed both directions of a bidirectional rating.
-- RF-12.4 makes only the professional's reputation and comments public; a
-- patient's ratings stay visible to their own author, recipient and an
-- administrator through the other two policies on this table.
-- ---------------------------------------------------------------------------

drop policy reviews_select_visible on public.reviews;

create policy reviews_select_visible on public.reviews
    for select to authenticated
    using (
        visible
        and exists (
            select 1 from public.professional_directory d where d.id = reviews.recipient_id
        )
    );

-- ---------------------------------------------------------------------------
-- 9. patients_select_counterpart granted the professional of an accepted
-- service the whole patients row, including medical_notes and
-- emergency_contact. RN-06 and RF-08.6 reveal contact details, which live on
-- profiles and are already covered by profiles_select_counterpart; nothing in
-- this project's requirements calls for exposing clinical or emergency data to
-- the professional, so this policy is removed rather than narrowed.
-- ---------------------------------------------------------------------------

drop policy patients_select_counterpart on public.patients;

-- ---------------------------------------------------------------------------
-- 10. service_types_select_all and professional_services_select_public used
-- `true` or omitted the active flag, so a catalog entry or an offered service
-- a professional turned off stayed visible to every signed in user.
-- ---------------------------------------------------------------------------

drop policy service_types_select_all on public.service_types;

create policy service_types_select_all on public.service_types
    for select to authenticated
    using (active);

drop policy professional_services_select_public on public.professional_services;

create policy professional_services_select_public on public.professional_services
    for select to authenticated
    using (
        active
        and exists (
            select 1
            from public.professional_directory d
            where d.id = professional_services.professional_id
        )
    );

-- ---------------------------------------------------------------------------
-- 11. messages_select_patient and messages_insert_patient checked only that
-- the caller owns the request, not that a service exists yet. The professional
-- side already requires one; HU-21 opens the conversation once an offer is
-- accepted, which is exactly when a service exists for both parties.
-- ---------------------------------------------------------------------------

drop policy messages_select_patient on public.messages;

create policy messages_select_patient on public.messages
    for select to authenticated
    using (
        exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.patient_id = (select auth.uid())
        )
    );

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
        )
    );

drop policy messages_update_patient on public.messages;

create policy messages_update_patient on public.messages
    for update to authenticated
    using (
        sender_id <> (select auth.uid())
        and exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.patient_id = (select auth.uid())
        )
    )
    with check (sender_id <> (select auth.uid()));

-- ---------------------------------------------------------------------------
-- 12. search_nearby_professionals raised a Postgres error instead of an
-- application-level failure when called with a negative p_limit, because
-- least() alone never clamps the lower bound.
-- ---------------------------------------------------------------------------

create or replace function public.search_nearby_professionals(
    p_latitude double precision,
    p_longitude double precision,
    p_radius_km double precision default 5,
    p_service_type_id uuid default null,
    p_available_now boolean default null,
    p_limit integer default 20,
    p_offset integer default 0
)
returns table (
    id uuid,
    full_name text,
    photo_url text,
    professional_type public.professional_type,
    specialty text,
    base_rate_bob numeric,
    average_rating numeric,
    total_reviews integer,
    total_services integer,
    available_now boolean,
    distance_m double precision
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        pro.id,
        p.full_name,
        p.photo_url,
        pro.professional_type,
        pro.specialty,
        pro.base_rate_bob,
        p.average_rating,
        p.total_reviews,
        pro.total_services,
        pro.available_now,
        extensions.st_distance(
            a.location,
            -- Longitude first, latitude second. Inverting the order returns
            -- empty or absurd results without raising an error.
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography
        ) as distance_m
    from public.professionals pro
    join public.profiles p on p.id = pro.id
    join public.addresses a on a.profile_id = pro.id and a.is_primary
    where
        -- INV-07: only approved and active professionals are visible.
        pro.verification_status = 'APPROVED'
        and p.active
        -- Index accelerated: this is the condition that uses idx_addresses_location.
        and extensions.st_dwithin(
            a.location,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography,
            p_radius_km * 1000
        )
        -- RN-02: and inside the coverage the professional declared.
        and extensions.st_dwithin(
            a.location,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography,
            pro.coverage_radius_km * 1000
        )
        and (p_available_now is null or pro.available_now = p_available_now)
        and (
            p_service_type_id is null
            or exists (
                select 1
                from public.professional_services ps
                where ps.professional_id = pro.id
                  and ps.service_type_id = p_service_type_id
                  and ps.active
            )
        )
    -- RF-06.4: ascending distance.
    order by distance_m asc
    -- RNF-08: no query returns an unbounded collection, and a negative limit
    -- or offset clamps to the nearest valid value instead of reaching Postgres.
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

-- ---------------------------------------------------------------------------
-- 13. device_platform carried an IOS value that this project's declared
-- Android-only scope (CLAUDE.md, "Alcance de plataforma") never uses. Postgres
-- has no DROP VALUE for enums, so the type is rebuilt; device_tokens is empty
-- on the remote project today, verified before writing this, so the rewrite
-- carries no data.
-- ---------------------------------------------------------------------------

alter type public.device_platform rename to device_platform_old;
create type public.device_platform as enum ('ANDROID');

alter table public.device_tokens
    alter column platform type public.device_platform
    using (platform::text::public.device_platform);

drop type public.device_platform_old;

-- ---------------------------------------------------------------------------
-- 14. The cascade from profiles down to patients, service_requests and
-- services reached payments and reviews: deleting a profile could silently
-- erase historical services and payments, which INV-05 says are never
-- rewritten. There is no delete policy and no account-deletion feature yet, so
-- the minimal fix is to stop the cascade at the one link that reaches
-- protected history: once a service exists for a request, that request can no
-- longer be deleted out from under it.
-- ---------------------------------------------------------------------------

alter table public.services
    drop constraint services_request_id_fkey,
    add constraint services_request_id_fkey
        foreign key (request_id) references public.service_requests (id) on delete restrict;
