-- HU-36: the home and the professional base are different addresses.
--
-- professional_covers and search_nearby_professionals used the person's single
-- is_primary address as the centre of their coverage radius. For somebody who
-- holds one role that is right. For somebody who holds both, the home where they
-- want to be treated and the base they cover an area from collapse into one row,
-- and there is no way to say they are different places.
--
-- The column is added rather than is_primary being reinterpreted by role. The
-- same datum meaning two things depending on who reads it is how a schema nobody
-- can reason about gets built. See docs/decisions.md, 2026-10-01.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. The column, its index and its trigger, mirroring is_primary
-- ---------------------------------------------------------------------------

alter table public.addresses
    add column is_professional_base boolean not null default false;

-- RN-02: at most one professional base per person.
create unique index idx_addresses_profile_professional_base
    on public.addresses (profile_id)
    where is_professional_base;

create or replace function public.unmark_previous_professional_base()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    update public.addresses
    set is_professional_base = false
    where profile_id = new.profile_id
      and id <> new.id
      and is_professional_base;
    return new;
end;
$$;

create trigger addresses_unmark_previous_professional_base
    before insert or update of is_professional_base on public.addresses
    for each row
    when (new.is_professional_base)
    execute function public.unmark_previous_professional_base();

-- ---------------------------------------------------------------------------
-- 2. Preserve the behaviour the previous column produced
-- ---------------------------------------------------------------------------

-- Without this every approved professional would vanish from search the instant
-- this migration applies, because the new column is born false and the join that
-- finds them is an inner one. There is no first-address trigger for the base on
-- purpose: declaring where you work is a statement only the professional can
-- make, and a professional with no base is invisible, which is the same
-- condition HU-06 verified for the primary address.
update public.addresses a
set is_professional_base = true
where a.is_primary
  and exists (
      select 1
      from public.profile_roles r
      where r.profile_id = a.profile_id
        and r.role = 'PROFESSIONAL'
  );

-- ---------------------------------------------------------------------------
-- 3. The view carries the new column
-- ---------------------------------------------------------------------------

drop view public.my_addresses;

create view public.my_addresses
with (security_invoker = true) as
select
    a.id,
    a.profile_id,
    a.alias,
    a.address_text,
    a.reference,
    a.city,
    extensions.st_y(a.location::extensions.geometry) as latitude,
    extensions.st_x(a.location::extensions.geometry) as longitude,
    a.is_primary,
    a.is_professional_base,
    a.created_at,
    a.updated_at
from public.addresses a;

revoke all on public.my_addresses from anon;
grant select on public.my_addresses to authenticated;

-- ---------------------------------------------------------------------------
-- 4. The two proximity readers follow the base, not the home
-- ---------------------------------------------------------------------------

create or replace function public.professional_covers(
    p_location extensions.geography,
    p_service_type_id uuid
)
returns boolean
language sql
stable
set search_path = ''
as $$
    select exists (
        select 1
        from public.professionals pro
        join public.profiles p on p.id = pro.id
        join public.addresses a on a.profile_id = pro.id and a.is_professional_base
        where pro.id = (select auth.uid())
          and pro.verification_status = 'APPROVED'
          and p.active
          and extensions.st_dwithin(a.location, p_location, pro.coverage_radius_km * 1000)
          and exists (
              select 1
              from public.professional_services ps
              where ps.professional_id = pro.id
                and ps.service_type_id = p_service_type_id
                and ps.active
          )
    );
$$;

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
    join public.addresses a on a.profile_id = pro.id and a.is_professional_base
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
