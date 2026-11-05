-- HT-29: answering an offer with its own amount is accepting it, so the thread
-- never holds a counter-offer equal to the offer it answers. The app turns that
-- amount into accept_offer before anything is written; this is what holds the
-- rule for any other writer. docs/decisions.md, 2026-11-04, HT-29.
--
-- A definer, so the parent is read whole and not through the caller's policies.
-- Only the two parties of the thread get this answer: a BEFORE trigger runs ahead
-- of the insert policy, and telling anybody else would let them find the amount
-- of someone else's offer one attempt at a time.
--
-- It only looks at new rows: the counter-offers that already repeat their parent
-- stay as they were written (INV-06).
create function public.guard_counter_differs_from_the_offer()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if exists (
        select 1
        from public.request_offers parent
        join public.service_requests r on r.id = parent.request_id
        where parent.id = new.parent_offer_id
          and parent.amount_bob = new.amount_bob
          and (select auth.uid()) in (r.patient_id, parent.professional_id)
    ) then
        raise exception 'counter_offer_repeats_the_offer_it_answers';
    end if;
    return new;
end;
$$;

revoke all on function public.guard_counter_differs_from_the_offer() from public, anon, authenticated;

create trigger request_offers_guard_counter_differs
    before insert on public.request_offers
    for each row
    when (new.parent_offer_id is not null)
    execute function public.guard_counter_differs_from_the_offer();
