-- HU-06 review: the marked-address delete guard was not serialized against
-- first_address_is_primary.
--
-- With a single marked address A, deleting it while another address C is being
-- inserted left the person holding C and no primary: the guard counted zero
-- survivors while the insert still saw A, so neither path marked anything.
--
-- Same lock as 20260915132619_serialize_first_address, which fixed this from the
-- insert side. See docs/decisions.md, 2026-10-04.

set search_path = public, extensions;

create or replace function public.guard_marked_address_delete()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    v_survivors integer;
begin
    if not (old.is_primary or old.is_professional_base) then
        return old;
    end if;

    -- for no key update, not for update: it still conflicts with itself and with
    -- the for update that first_address_is_primary takes, which is all the
    -- serialization needs, while leaving the for key share that every foreign
    -- key insert against profiles takes. Deleting an address would otherwise
    -- stall unrelated inserts for that person until commit.
    perform 1 from public.profiles where id = old.profile_id for no key update;

    -- A cascade from profiles removes the parent row before the children, so its
    -- absence is what tells an account deletion from a single address deletion.
    if not found then
        return old;
    end if;

    select count(*) into v_survivors
    from public.addresses
    where profile_id = old.profile_id
      and id <> old.id;

    if v_survivors >= 2 then
        raise exception 'address_needs_successor';
    end if;

    return old;
end;
$$;

-- The guard already took the lock on this same row, so no address can appear
-- between the two and the survivor is unique when there is one.
create or replace function public.promote_last_address()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    v_survivor uuid;
begin
    if not (old.is_primary or old.is_professional_base) then
        return null;
    end if;

    if not exists (select 1 from public.profiles where id = old.profile_id) then
        return null;
    end if;

    select id into v_survivor
    from public.addresses
    where profile_id = old.profile_id;

    if v_survivor is null then
        return null;
    end if;

    update public.addresses
    set is_primary = is_primary or old.is_primary,
        is_professional_base = is_professional_base or old.is_professional_base
    where id = v_survivor;

    return null;
end;
$$;
