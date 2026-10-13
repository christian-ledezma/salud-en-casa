-- ---------------------------------------------------------------------------
-- The nearby search answers about the point it publishes, and about no other
-- ---------------------------------------------------------------------------
-- 20261009140000 quantised what the search sends out: the marker to a ~110 m
-- grid, the distance to the hundred metres, and the requested radius to a tenth
-- of a kilometre. It named three channels onto the professional's base point
-- and closed the three.
--
-- There is a fourth, and it is not a number the function returns. It is the
-- answer itself. p_latitude and p_longitude are free double precision
-- parameters, so a caller can hold the radius still -- 5.0 km, which the
-- snapping makes exactly 5000 m and therefore known -- and move the origin
-- instead. Bisecting along a ray finds a point that is exactly 5000 m from the
-- base, and three such points determine one circle, whose centre is the base.
-- Quantising the radius does not help against this: it only makes the radius
-- discrete, and a known radius is all trilateration needs when the origin is
-- continuous.
--
-- Measured on 2026-10-10 against this project: with the radius fixed at 5,0 km,
-- an origin 4999 m from the base returned the professional and one 5001 m away
-- did not. The boundary was sharp to the metre. The coverage radius gave an
-- even cheaper path, because professional_directory publishes
-- coverage_radius_km: knowing the radius, one boundary point is enough, and
-- that second dwithin was never snapped at all.
--
-- So the remedy is not another rounding. It is that the function stop touching
-- the exact point once it has read it: the base is snapped to the grid once,
-- and that snapped point -- the one the marker already publishes -- is what
-- decides who is inside, what distance is returned and in what order the rows
-- come out. The boundary a caller can find is then the circle around a point
-- they were given on the first call, and the ordering between two rows reveals
-- the bisector of two published markers. Neither says anything new.
--
-- The index is the cost to watch. st_dwithin over a computed expression cannot
-- use idx_addresses_location, because the operand is no longer the indexed
-- column, and losing it breaks INV-08 and the time RNF-01 asks for. So the
-- untouched column keeps an index-accelerated prefilter with slack, and the
-- snapped point decides afterwards. Half the diagonal of a 0,001 degree cell is
-- at most ~79 m, at the equator, where a degree of longitude is longest; 100 m
-- of slack covers it everywhere.
--
-- create or replace and not drop: the thirteen returned columns and the seven
-- parameters are the same, so the signature does not change and the grants of
-- 20261009140000 survive untouched.

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
    distance_m double precision,
    base_latitude double precision,
    base_longitude double precision
)
language sql
stable
security definer
set search_path = ''
as $$
    with snapped as (
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
            pro.coverage_radius_km,
            -- The base on the city block grid, built once. Longitude first and
            -- latitude second, as st_makepoint takes them; st_x is the
            -- longitude and st_y the latitude on the way out of the geometry.
            extensions.st_setsrid(
                extensions.st_makepoint(
                    round(extensions.st_x(a.location::extensions.geometry)::numeric, 3)::double precision,
                    round(extensions.st_y(a.location::extensions.geometry)::numeric, 3)::double precision
                ),
                4326
            )::extensions.geography as base
        from public.professionals pro
        join public.profiles p on p.id = pro.id
        join public.addresses a on a.profile_id = pro.id and a.is_professional_base
        where
            -- INV-07: only approved and active professionals are visible.
            pro.verification_status = 'APPROVED'
            and p.active
            -- RN-13: a dual-role person never appears in their own results.
            and pro.id <> (select auth.uid())
            -- Index accelerated, on the untouched column: this is the condition
            -- that uses idx_addresses_location. It only has to be generous
            -- enough to keep every row the snapped test below could accept.
            and extensions.st_dwithin(
                a.location,
                extensions.st_setsrid(
                    extensions.st_makepoint(p_longitude, p_latitude), 4326
                )::extensions.geography,
                (round(coalesce(p_radius_km, 5)::numeric, 1) * 1000)::double precision + 100
            )
            and extensions.st_dwithin(
                a.location,
                extensions.st_setsrid(
                    extensions.st_makepoint(p_longitude, p_latitude), 4326
                )::extensions.geography,
                pro.coverage_radius_km * 1000 + 100
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
    )
    select
        s.id,
        s.full_name,
        s.photo_url,
        s.professional_type,
        s.specialty,
        s.base_rate_bob,
        s.average_rating,
        s.total_reviews,
        s.total_services,
        s.available_now,
        round(
            extensions.st_distance(
                s.base,
                extensions.st_setsrid(
                    extensions.st_makepoint(p_longitude, p_latitude), 4326
                )::extensions.geography
            )::numeric,
            -2
        )::double precision as distance_m,
        extensions.st_y(s.base::extensions.geometry) as base_latitude,
        extensions.st_x(s.base::extensions.geometry) as base_longitude
    from snapped s
    where
        -- The conditions that decide, now about the published point. A caller
        -- bisecting on the origin recovers this circle, and its centre is the
        -- marker they already have.
        extensions.st_dwithin(
            s.base,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography,
            (round(coalesce(p_radius_km, 5)::numeric, 1) * 1000)::double precision
        )
        and extensions.st_dwithin(
            s.base,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography,
            s.coverage_radius_km * 1000
        )
    -- RF-06.4: ascending distance. Ordered by the snapped distance now, so the
    -- moment two rows swap places says nothing the two markers did not. Ties
    -- are frequent by construction, which is why the tiebreaker is mandatory.
    order by
        extensions.st_distance(
            s.base,
            extensions.st_setsrid(
                extensions.st_makepoint(p_longitude, p_latitude), 4326
            )::extensions.geography
        ) asc,
        s.id asc
    -- RNF-08: no query returns an unbounded collection, and a negative limit
    -- or offset clamps to the nearest valid value instead of reaching Postgres.
    limit least(greatest(coalesce(p_limit, 20), 0), 100)
    offset greatest(coalesce(p_offset, 0), 0);
$$;
