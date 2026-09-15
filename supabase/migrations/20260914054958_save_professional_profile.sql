-- HU-04: the professional profile is written in the same transaction as the
-- profile every person shares, and the coverage radius column is narrowed to
-- the range the story states.
--
-- available_now is deliberately absent from the function. RF-02.5 is a switch
-- the professional flips to say whether they are taking work right now, so it
-- is written on its own by professionals_update_own. Carrying it inside the
-- save would let a form submitted from a stale screen undo a switch flipped
-- seconds earlier.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- The coverage radius runs from 1 to 50 kilometres
-- ---------------------------------------------------------------------------

-- The column accepted anything above zero, so a radius of 200 metres was a
-- legal value with no professional behind it. The story fixes the range at 1
-- to 50 kilometres; leaving the column looser would put the rule in the client
-- alone, where nothing enforces it for a request that does not come from the
-- application.
alter table public.professionals
    drop constraint if exists professionals_coverage_radius_km_check;

alter table public.professionals
    add constraint professionals_coverage_radius_km_range
    check (coverage_radius_km >= 1 and coverage_radius_km <= 50);

-- ---------------------------------------------------------------------------
-- save_my_profile gains the professional columns
-- ---------------------------------------------------------------------------

-- The old signature is dropped instead of being left beside the new one.
-- PostgREST resolves a stored function by the arguments it receives, and two
-- functions of the same name that both accept the original five would be
-- ambiguous rather than overloaded.
drop function if exists public.save_my_profile(text, text, date, text, text);

-- Security invoker, like assign_my_role and like the version this replaces:
-- profiles_update_own, patients_update_own and professionals_update_own
-- already allow every write below, so the function adds atomicity and nothing
-- else.
--
-- The role is read here rather than trusted from the client, so an argument
-- sent by mistake is ignored instead of written. years_of_experience and
-- coverage_radius_km are assigned straight across: both columns are not null,
-- so a professional whose client omits them gets a refusal rather than a
-- silent no-op that leaves the old value in place.
create or replace function public.save_my_profile(
    p_full_name text,
    p_phone text default null,
    p_birth_date date default null,
    p_emergency_contact text default null,
    p_medical_notes text default null,
    p_professional_type public.professional_type default null,
    p_specialty text default null,
    p_biography text default null,
    p_years_of_experience integer default null,
    p_base_rate_bob numeric default null,
    p_coverage_radius_km numeric default null
)
returns void
language plpgsql
volatile
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_role public.user_role;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    select role into v_role
    from public.profiles
    where id = v_caller;

    if not found then
        raise exception 'profile_not_found';
    end if;

    update public.profiles
    set full_name = p_full_name,
        phone = p_phone
    where id = v_caller;

    if v_role = 'PATIENT' then
        update public.patients
        set birth_date = p_birth_date,
            emergency_contact = p_emergency_contact,
            medical_notes = p_medical_notes
        where id = v_caller;
    elsif v_role = 'PROFESSIONAL' then
        update public.professionals
        set professional_type = p_professional_type,
            specialty = p_specialty,
            biography = p_biography,
            years_of_experience = p_years_of_experience,
            base_rate_bob = p_base_rate_bob,
            coverage_radius_km = p_coverage_radius_km
        where id = v_caller;
    end if;
end;
$$;

revoke all on function public.save_my_profile(
    text, text, date, text, text, public.professional_type, text, text, integer, numeric, numeric
) from public, anon;

grant execute on function public.save_my_profile(
    text, text, date, text, text, public.professional_type, text, text, integer, numeric, numeric
) to authenticated;
