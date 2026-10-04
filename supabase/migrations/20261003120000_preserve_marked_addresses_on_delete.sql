-- HU-06: deleting a marked address no longer leaves the person without one.
--
-- addresses_first_is_primary only fires on insert, so deleting the primary
-- address promoted nobody. The person kept addresses and had none primary,
-- which is the state the author's own rows were found in on 2026-10-02.
--
-- is_professional_base has the same shape and worse consequences: the join in
-- search_nearby_professionals is an inner one, so losing the base removes the
-- professional from every search without anything saying so (RN-02).
--
-- The rule, decided with the stakeholder on 2026-10-03: with exactly one
-- address left, it inherits the marks; with two or more, the person chooses the
-- successor before deleting. See docs/decisions.md, 2026-10-03.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. Two or more survivors: the engine refuses and the person chooses
-- ---------------------------------------------------------------------------

-- The client sets the successor first, which clears the mark on the row being
-- deleted, so this never fires for anybody coming through the screen. It fires
-- for everything else, because PostgREST exposes the table whether or not the
-- application has a screen for it.
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

    -- A cascade from profiles deletes every address of the person, and the
    -- referential action runs after the parent row is gone, so its absence is
    -- what tells this apart from someone deleting one address. Without it,
    -- deleting an account with three addresses would abort on the primary one.
    if not exists (select 1 from public.profiles where id = old.profile_id) then
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

create trigger addresses_guard_marked_delete
    before delete on public.addresses
    for each row execute function public.guard_marked_address_delete();

-- ---------------------------------------------------------------------------
-- 2. Exactly one survivor: it inherits whatever the deleted row carried
-- ---------------------------------------------------------------------------

-- The guard above has already run, so when a mark was carried there is at most
-- one row left to find. Both marks move together because a person who has one
-- address left has no choice to make about either.
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

    -- Same cascade as above: there is nobody left to promote, and the rows that
    -- look like survivors are on their way out too.
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

create trigger addresses_promote_last_address
    after delete on public.addresses
    for each row execute function public.promote_last_address();

-- ---------------------------------------------------------------------------
-- 3. Repair the rows the missing trigger already left behind
-- ---------------------------------------------------------------------------

-- The oldest address is the one addresses_first_is_primary would have marked,
-- so restoring the invariant that way reproduces what should have happened
-- rather than inventing a choice.
--
-- is_professional_base is deliberately NOT repaired the same way. A primary
-- address is where the person is attended and every address of theirs is a
-- candidate; a professional base is where they work, and picking one for them
-- would put them in search results centred on an address they never chose for
-- that. A professional without a base simply does not appear, which is what
-- HU-36's fourth criterion already states.
with first_address as (
    select distinct on (a.profile_id) a.id
    from public.addresses a
    where not exists (
        select 1
        from public.addresses p
        where p.profile_id = a.profile_id
          and p.is_primary
    )
    order by a.profile_id, a.created_at, a.id
)
update public.addresses
set is_primary = true
where id in (select id from first_address);
