-- Closes three gaps found by security review after HT-04 shipped: a patient
-- or professional could inflate their own reputation and service count,
-- reactivate an account an administrator deactivated, or rewrite a message
-- they only received.
--
-- profiles_update_own and professionals_update_own check whose row it is,
-- never which columns changed, so nothing stopped the row's own owner from
-- writing average_rating, total_reviews, active or total_services by hand.
-- messages_update_patient and messages_update_professional have the same
-- shape: they check who is not the sender, not what changed.
--
-- Migrations already applied are never edited (see docs/decisions.md); this
-- is a new migration on top of identity, delivery, requests and functions.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- profiles: average_rating and total_reviews are written only by
-- recalculate_reputation(); active is meant to be administrative. Both must
-- stay closed to the row's own owner, but recalculate_reputation() itself
-- writes them from inside a request made by an ordinary patient or
-- professional submitting a review, where is_admin() is false. A guard that
-- only checked is_admin() would block that legitimate write along with the
-- one this migration exists to stop.
--
-- pg_trigger_depth() tells them apart. A direct client update reaches this
-- trigger at depth 1. recalculate_reputation() reaches it at depth 2, because
-- it runs from inside the AFTER trigger that reviews already fired. Nothing
-- an authenticated client controls can insert itself between those two
-- levels: triggers are schema objects, not something the anon or
-- authenticated role can attach.
-- ---------------------------------------------------------------------------

create or replace function public.guard_profile_server_managed_fields()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.average_rating is distinct from old.average_rating
        or new.total_reviews is distinct from old.total_reviews
        or new.active is distinct from old.active then
        raise exception 'reputation_and_active_status_are_managed_by_the_server';
    end if;
    return new;
end;
$$;

create trigger profiles_guard_server_managed_fields
    before update on public.profiles
    for each row
    when (not public.is_admin() and pg_trigger_depth() <= 1)
    execute function public.guard_profile_server_managed_fields();

-- ---------------------------------------------------------------------------
-- professionals: total_services is written only by
-- increment_total_services(), reached the same way, at trigger depth 2, from
-- the professional's own request that marks a service completed.
-- ---------------------------------------------------------------------------

create or replace function public.guard_professional_server_managed_fields()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.total_services is distinct from old.total_services then
        raise exception 'total_services_is_maintained_by_the_server';
    end if;
    return new;
end;
$$;

create trigger professionals_guard_server_managed_fields
    before update on public.professionals
    for each row
    when (not public.is_admin() and pg_trigger_depth() <= 1)
    execute function public.guard_professional_server_managed_fields();

-- ---------------------------------------------------------------------------
-- messages: the recipient may only mark a message as read (RF-09.2). There is
-- no administrator to bypass here and nothing ever writes this table from
-- inside another trigger, so a column level privilege is enough on its own;
-- no trigger and no depth check are needed. INV-12 stays untouched: this
-- does not add any policy that reads or writes messages for an administrator.
-- ---------------------------------------------------------------------------

revoke update on public.messages from authenticated;
grant update (read_at) on public.messages to authenticated;
