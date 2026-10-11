-- ---------------------------------------------------------------------------
-- The nearby search publishes a professional's whereabouts on a city block
-- grid, through every channel it has
-- ---------------------------------------------------------------------------
-- HU-11 draws the results on a map, and this function returned no coordinate
-- at all: only distance_m. The marker has nowhere to come from unless the base
-- point joins the projection, and a professional base may well be the person's
-- own home.
--
-- The resolution at which a point is published is the resolution of the finest
-- channel exposed, and there are three channels onto the same point here:
-- the marker, the returned distance, and the boundary of the requested radius.
-- All three are quantised to the same ~110 m grid, or none of them is worth
-- quantising (docs/decisions.md, 2026-10-09).
--
-- A function cannot change its return type in place, so it is dropped and
-- recreated, and the grants have to be written again: after a drop PostgreSQL
-- hands execute back to public by default.

drop function public.search_nearby_professionals(
    double precision, double precision, double precision, uuid, boolean, integer, integer
);

create function public.search_nearby_professionals(
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
    distance_m double precision,
    base_latitude double precision,
    base_longitude double precision
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
        -- Rounded to the nearest hundred metres. Exact metres plus a free
        -- origin parameter is a trilateration oracle on the base point.
        round(
            extensions.st_distance(
                a.location,
                -- Longitude first, latitude second. Inverting the order returns
                -- empty or absurd results without raising an error.
                extensions.st_setsrid(
                    extensions.st_makepoint(p_longitude, p_latitude), 4326
                )::extensions.geography
            )::numeric,
            -2
        )::double precision as distance_m,
        -- Three decimals is about 110 m at this latitude: a city block, which
        -- is all the map needs to say how many professionals are in a zone.
        round(extensions.st_y(a.location::extensions.geometry)::numeric, 3)::double precision
            as base_latitude,
        round(extensions.st_x(a.location::extensions.geometry)::numeric, 3)::double precision
            as base_longitude
    from public.professionals pro
    join public.profiles p on p.id = pro.id
    join public.addresses a on a.profile_id = pro.id and a.is_professional_base
    where
        -- INV-07: only approved and active professionals are visible.
        pro.verification_status = 'APPROVED'
        and p.active
        -- RN-13: a dual-role person never appears in their own results.
        and pro.id <> (select auth.uid())
        -- Index accelerated: this is the condition that uses idx_addresses_location.
        -- The radius lands on the same grid as everything else, or a binary
        -- search over it recovers the exact distance the rounding above hides.
        and extensions.st_dwithin(
            a.location,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography,
            (round(coalesce(p_radius_km, 5)::numeric, 1) * 1000)::double precision
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
    -- RF-06.4: ascending distance, ordered by the exact value and not by the
    -- rounded one. The tiebreaker is now mandatory rather than tidy: rounding
    -- to the hundred makes ties frequent, and offset pagination over an order
    -- with no second key repeats or skips a row between two pages.
    order by
        extensions.st_distance(
            a.location,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography
        ) asc,
        pro.id asc
    -- RNF-08: no query returns an unbounded collection, and a negative limit
    -- or offset clamps to the nearest valid value instead of reaching Postgres.
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;

revoke all on function public.search_nearby_professionals(
    double precision, double precision, double precision, uuid, boolean, integer, integer
) from public, anon;

grant execute on function public.search_nearby_professionals(
    double precision, double precision, double precision, uuid, boolean, integer, integer
) to authenticated;
