-- HT-16: the three gaps that multiple roles per person turns reachable.
--
-- All three were open before HU-34 and none was exploitable: the single role
-- rule removed the precondition for the first, and the other two are
-- unreachable because services has no insert policy at all, so no service and
-- therefore no review can exist yet. HU-34 removes the precondition, so they
-- are closed here rather than left for the sprint that would build on them.
--
-- "No client code reads that table" protects nothing: PostgREST exposes every
-- table, so anyone with a session can query service_requests whether or not a
-- screen exists. What actually held the first gap shut is that offering needs
-- verification_status = 'APPROVED', which only an administrator sets.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. RN-13: nobody is both sides of the same attention
-- ---------------------------------------------------------------------------

-- The inbox never showed the caller their own request because nobody could be
-- patient and professional at once. services.parties_are_different was the only
-- backstop, and it is reached far too late: by then the negotiation happened.
drop policy service_requests_select_inbox on public.service_requests;

create policy service_requests_select_inbox on public.service_requests
    for select to authenticated
    using (
        modality = 'IMMEDIATE'
        and status in ('PUBLISHED', 'NEGOTIATING')
        and patient_id <> (select auth.uid())
        and public.professional_covers(location, service_type_id)
    );

-- Same hole on the writing side. Only the PROFESSIONAL branch needs it: the
-- PATIENT branch already requires r.patient_id = auth.uid(), which is the
-- person acting as the patient they are.
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
                      and r.patient_id <> (select auth.uid())
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
-- 2. RN-14: reputation is per role
-- ---------------------------------------------------------------------------

-- The entry of 2026-09-11 put both columns on profiles and described the cost
-- of this shape exactly: "the patient would need a parallel column in patients
-- and the recalculation trigger would have to decide which to write according
-- to the recipient's role". That was the right call while a person held one
-- role. With two, one column mixes the reputation earned as a patient with the
-- reputation earned as a professional, and professional_directory publishes the
-- mixture. See docs/decisions.md, 2026-10-01.
alter table public.patients
    add column average_rating numeric(3, 2) not null default 0
        check (average_rating between 0 and 5),
    add column total_reviews integer not null default 0
        check (total_reviews >= 0);

alter table public.professionals
    add column average_rating numeric(3, 2) not null default 0
        check (average_rating between 0 and 5),
    add column total_reviews integer not null default 0
        check (total_reviews >= 0);

-- A no-op today, because reviews is empty and cannot be filled yet. It is
-- written correctly anyway so that applying these migrations in order on a
-- populated database produces the same state as applying them on an empty one.
update public.professionals pro
set average_rating = coalesce(agg.average_rating, 0),
    total_reviews = coalesce(agg.total_reviews, 0)
from (
    select
        s.professional_id as recipient_id,
        round(avg(r.rating)::numeric, 2) as average_rating,
        count(*)::integer as total_reviews
    from public.reviews r
    join public.services s on s.id = r.service_id
    where r.visible
      and r.recipient_id = s.professional_id
    group by s.professional_id
) as agg
where pro.id = agg.recipient_id;

update public.patients pat
set average_rating = coalesce(agg.average_rating, 0),
    total_reviews = coalesce(agg.total_reviews, 0)
from (
    select
        s.patient_id as recipient_id,
        round(avg(r.rating)::numeric, 2) as average_rating,
        count(*)::integer as total_reviews
    from public.reviews r
    join public.services s on s.id = r.service_id
    where r.visible
      and r.recipient_id = s.patient_id
    group by s.patient_id
) as agg
where pat.id = agg.recipient_id;

-- The side of each rating comes from the service that produced it, never from
-- the roles the person holds now. Somebody rated as a patient who activates the
-- professional role next month must not carry those ratings across.
create or replace function public.recalculate_reputation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_recipient_id uuid;
begin
    -- In a DELETE trigger NEW is not assigned, so it cannot be read at all.
    -- Deleting a service cascades into its reviews and reaches this path.
    if tg_op = 'DELETE' then
        v_recipient_id := old.recipient_id;
    else
        v_recipient_id := new.recipient_id;
    end if;

    update public.professionals pro
    set average_rating = coalesce(agg.average_rating, 0),
        total_reviews = coalesce(agg.total_reviews, 0)
    from (
        select
            round(avg(r.rating)::numeric, 2) as average_rating,
            count(*)::integer as total_reviews
        from public.reviews r
        join public.services s on s.id = r.service_id
        where r.recipient_id = v_recipient_id
          and r.visible
          and s.professional_id = v_recipient_id
    ) as agg
    where pro.id = v_recipient_id;

    update public.patients pat
    set average_rating = coalesce(agg.average_rating, 0),
        total_reviews = coalesce(agg.total_reviews, 0)
    from (
        select
            round(avg(r.rating)::numeric, 2) as average_rating,
            count(*)::integer as total_reviews
        from public.reviews r
        join public.services s on s.id = r.service_id
        where r.recipient_id = v_recipient_id
          and r.visible
          and s.patient_id = v_recipient_id
    ) as agg
    where pat.id = v_recipient_id;

    return null;
end;
$$;

-- ---------------------------------------------------------------------------
-- 3. The readers of the reputation follow it to its new home
-- ---------------------------------------------------------------------------

-- The view has to be redefined before the columns it reads can be dropped.
--
-- create or replace, not drop and create: four policies depend on this view --
-- availability_slots_select_public, professional_services_select_public,
-- request_offers_insert_participants and reviews_select_visible -- and a drop
-- is refused while they exist. Replacing keeps the view's identity and with it
-- those dependencies, which is also why the column list has to stay in the same
-- order with the same names and types. Only the source of average_rating and
-- total_reviews changes, from profiles to professionals, and both keep their
-- numeric(3,2) and integer types.
--
-- security_invoker stays false: the view exists to show other people's rows,
-- and its own filter is what enforces INV-07.
create or replace view public.professional_directory
with (security_invoker = false) as
select
    pro.id,
    p.full_name,
    p.photo_url,
    pro.average_rating,
    pro.total_reviews,
    pro.professional_type,
    pro.specialty,
    pro.biography,
    pro.base_rate_bob,
    pro.years_of_experience,
    pro.coverage_radius_km,
    pro.available_now,
    pro.total_services
from public.professionals pro
join public.profiles p on p.id = pro.id
where pro.verification_status = 'APPROVED'
  and p.active;

revoke all on public.professional_directory from anon;
grant select on public.professional_directory to authenticated;

-- RF-12.4 makes only the professional's reputation and comments public. The
-- previous version keyed on "the recipient is in professional_directory"
-- without looking at which side of the service they were, so every rating a
-- dual-role person received as a patient would become publicly readable the
-- moment they were approved as a professional. Their own ratings as a patient
-- stay visible to their author, their recipient and an administrator through
-- the other two policies on this table.
drop policy reviews_select_visible on public.reviews;

create policy reviews_select_visible on public.reviews
    for select to authenticated
    using (
        visible
        and exists (
            select 1
            from public.services s
            where s.id = reviews.service_id
              and s.professional_id = reviews.recipient_id
        )
        and exists (
            select 1 from public.professional_directory d where d.id = reviews.recipient_id
        )
    );

-- ---------------------------------------------------------------------------
-- 4. The write guards move with the columns
-- ---------------------------------------------------------------------------

-- profiles keeps only active. The depth check stays <= 0 for the reason H2
-- documents: recalculate_reputation reaches the row from inside another
-- trigger, and the guard must not fire on that legitimate nested write.
create or replace function public.guard_profile_server_managed_fields()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.active is distinct from old.active then
        raise exception 'active_status_is_managed_by_the_server';
    end if;
    return new;
end;
$$;

create or replace function public.guard_professional_server_managed_fields()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.total_services is distinct from old.total_services then
        raise exception 'total_services_is_maintained_by_the_server';
    end if;
    if new.average_rating is distinct from old.average_rating
        or new.total_reviews is distinct from old.total_reviews then
        raise exception 'reputation_is_maintained_by_the_server';
    end if;
    return new;
end;
$$;

-- patients had nothing to protect until now, so this guard is new rather than
-- extended.
create or replace function public.guard_patient_server_managed_fields()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.average_rating is distinct from old.average_rating
        or new.total_reviews is distinct from old.total_reviews then
        raise exception 'reputation_is_maintained_by_the_server';
    end if;
    return new;
end;
$$;

create trigger patients_guard_server_managed_fields
    before update on public.patients
    for each row
    when (not public.is_admin() and pg_trigger_depth() <= 0)
    execute function public.guard_patient_server_managed_fields();

alter table public.profiles
    drop column average_rating,
    drop column total_reviews;

-- ---------------------------------------------------------------------------
-- 5. Nobody finds themselves in their own search
-- ---------------------------------------------------------------------------

-- auth.uid() reads the session's JWT claim, which is set at the connection
-- level, so it is still the caller inside this security definer function.
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
        pro.average_rating,
        pro.total_reviews,
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
        -- RN-13: a dual-role person never appears in their own results.
        and pro.id <> (select auth.uid())
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
