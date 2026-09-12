-- Stored functions: profile creation, proximity search and reputation.
--
-- Every function here declares `set search_path = ''` and fully qualifies the
-- objects it touches. A security definer function without that setting can be
-- redirected to another schema by whoever calls it.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- Profile creation on first sign in
-- ---------------------------------------------------------------------------

-- RF-01.3: the profile is created automatically with what the provider gives.
-- The role is left null, so the application must ask for it before anything
-- else (RF-01.4), and it can never arrive as ADMIN (RF-01.5).
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profiles (id, full_name, email, photo_url)
    values (
        new.id,
        coalesce(
            nullif(new.raw_user_meta_data ->> 'full_name', ''),
            nullif(new.raw_user_meta_data ->> 'name', ''),
            split_part(coalesce(new.email, 'unknown'), '@', 1)
        ),
        coalesce(new.email, ''),
        coalesce(
            nullif(new.raw_user_meta_data ->> 'avatar_url', ''),
            nullif(new.raw_user_meta_data ->> 'picture', '')
        )
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- Proximity search
-- ---------------------------------------------------------------------------

-- INV-08: proximity is always resolved here, inside the database, with PostGIS.
-- Never with an external proximity service and never by filtering distances on
-- the client.
--
-- Security definer because it has to read the primary address of other
-- professionals, which their own policy keeps private. It returns no contact
-- data: the phone number stays hidden until an offer is accepted (RN-06).
--
-- RN-02 makes the condition mutual. The professional must be inside the radius
-- the patient asked for, and the patient must be inside the coverage radius the
-- professional declared.
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
    -- RNF-08: no query returns an unbounded collection.
    limit least(coalesce(p_limit, 20), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

revoke all on function public.search_nearby_professionals(
    double precision, double precision, double precision, uuid, boolean, integer, integer
) from public, anon;

grant execute on function public.search_nearby_professionals(
    double precision, double precision, double precision, uuid, boolean, integer, integer
) to authenticated;

-- ---------------------------------------------------------------------------
-- Reputation
-- ---------------------------------------------------------------------------

-- RF-12.3: reputation is recomputed by the database on every new rating, so it
-- cannot drift because a client forgot to update it.
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

    update public.profiles p
    set average_rating = coalesce(agg.average_rating, 0),
        total_reviews = coalesce(agg.total_reviews, 0)
    from (
        select
            round(avg(r.rating)::numeric, 2) as average_rating,
            count(*)::integer as total_reviews
        from public.reviews r
        where r.recipient_id = v_recipient_id
          and r.visible
    ) as agg
    where p.id = v_recipient_id;
    return null;
end;
$$;

create trigger reviews_recalculate_reputation
    after insert or update or delete on public.reviews
    for each row execute function public.recalculate_reputation();

-- RF-02.6 and HU-12 show the total number of services a professional has
-- delivered. Counting it here keeps it consistent with the service record.
create or replace function public.increment_total_services()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.status = 'COMPLETED' and old.status is distinct from 'COMPLETED' then
        update public.professionals
        set total_services = total_services + 1
        where id = new.professional_id;
    end if;
    return null;
end;
$$;

create trigger services_increment_total_services
    after update of status on public.services
    for each row execute function public.increment_total_services();
