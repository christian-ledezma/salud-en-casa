-- HU-15: reject a new or edited availability slot that overlaps another one
-- the same professional already has in the same day.
--
-- The schema already has `end_time > start_time` and the composite UNIQUE
-- (professional_id, day_of_week, start_time, end_time) from the catalog
-- migration, so an exact duplicate is covered. What is not covered is a
-- partial overlap: (10:00-12:00) against (11:00-13:00), which contradicts
-- the slot the professional already declared.
--
-- A trigger using the native OVERLAPS operator on `time` is enough here and
-- avoids adding the btree_gist extension that an EXCLUDE constraint would
-- need. It raises errcode 23505 so the client reuses the unique-violation
-- path it already has.

create or replace function public.reject_overlapping_slot()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if exists (
        select 1
        from public.availability_slots s
        where s.professional_id = new.professional_id
          and s.day_of_week = new.day_of_week
          and s.id <> new.id
          and (s.start_time, s.end_time) overlaps (new.start_time, new.end_time)
    ) then
        raise exception 'availability_slot_overlaps'
            using errcode = '23505';
    end if;
    return new;
end;
$$;

create trigger availability_slots_reject_overlap
    before insert or update on public.availability_slots
    for each row execute function public.reject_overlapping_slot();
