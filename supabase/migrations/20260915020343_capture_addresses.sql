-- HU-05: the address is captured on a map, so the stored point has to be
-- readable as a pair of numbers, the first address a person registers has to be
-- the primary one, and the reference has to be bounded like the text columns
-- beside it.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- my_addresses: the stored point, readable
-- ---------------------------------------------------------------------------

-- PostgREST hands a geography column back as the hexadecimal encoding PostGIS
-- keeps on disk, which the client would have to decode before it could draw a
-- marker. The view projects the same point as two numbers and changes nothing
-- else. It is security_invoker, so addresses_select_own decides which rows it
-- returns (INV-13); professional_directory is deliberately the opposite,
-- because that one exists to show other people's rows.
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
    a.created_at,
    a.updated_at
from public.addresses a;

revoke all on public.my_addresses from anon;
grant select on public.my_addresses to authenticated;

-- ---------------------------------------------------------------------------
-- The first address a person registers is the primary one
-- ---------------------------------------------------------------------------

-- is_primary comes in false by default and search_nearby_professionals joins
-- addresses on that column, so a professional whose only address was not the
-- primary one would be invisible to the search. Choosing among several is
-- RF-03.4 and belongs to HU-06; having one is not a choice, so the database
-- makes it rather than the form.
create or replace function public.first_address_is_primary()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if not exists (
        select 1 from public.addresses where profile_id = new.profile_id
    ) then
        new.is_primary := true;
    end if;
    return new;
end;
$$;

-- Before-row triggers fire in name order, and this name places it ahead of
-- addresses_unmark_previous_primary, so the flag it raises is the one that
-- trigger evaluates.
create trigger addresses_first_is_primary
    before insert on public.addresses
    for each row execute function public.first_address_is_primary();

-- ---------------------------------------------------------------------------
-- The reference is bounded like the columns beside it
-- ---------------------------------------------------------------------------

-- alias and address_text carry length checks from the migration that created
-- them; reference was left open, which made the one free text field of the form
-- the only column a client could use to store an arbitrary amount of data. An
-- empty reference is no reference, so the lower bound is one character and the
-- application sends null.
alter table public.addresses
    add constraint addresses_reference_length
    check (reference is null or char_length(reference) between 1 and 300);
