-- HU-20: the coverage rule, written once, for any professional.
-- docs/decisions.md, 2026-10-16, "el motor decide y encola".

set search_path = public, extensions;

create or replace function public.professional_covers_for(
    p_professional_id uuid,
    p_location extensions.geography,
    p_service_type_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.professionals pro
        join public.profiles p on p.id = pro.id
        join public.addresses a on a.profile_id = pro.id and a.is_professional_base
        where pro.id = p_professional_id
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

-- docs/decisions.md, 2026-10-16: no client may execute it.
revoke all on function public.professional_covers_for(uuid, extensions.geography, uuid)
    from public, anon, authenticated;

-- Definer: an invoker cannot call a function it may not execute.
create or replace function public.professional_covers(
    p_location extensions.geography,
    p_service_type_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select public.professional_covers_for((select auth.uid()), p_location, p_service_type_id);
$$;

revoke all on function public.professional_covers(extensions.geography, uuid) from public, anon;
grant execute on function public.professional_covers(extensions.geography, uuid) to authenticated;
