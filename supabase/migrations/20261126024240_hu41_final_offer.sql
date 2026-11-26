-- HU-41: either side may mark an offer as their last one, and the other side can
-- then only accept it or reject it. docs/decisions.md, 2026-11-25, HU-41.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. The mark.
-- ---------------------------------------------------------------------------

-- The offers already issued were none of them final, so the default states what
-- they were and rewrites nothing of them (INV-06).
alter table public.request_offers
    add column is_final boolean not null default false;

-- The mark joins the frozen columns: a final offer that could be taken back would
-- be a bluff the engine helps to make.
create or replace function public.guard_offer_is_append_only()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.id is distinct from old.id
        or new.request_id is distinct from old.request_id
        or new.professional_id is distinct from old.professional_id
        or new.parent_offer_id is distinct from old.parent_offer_id
        or new.issuer is distinct from old.issuer
        or new.amount_bob is distinct from old.amount_bob
        or new.message is distinct from old.message
        or new.is_final is distinct from old.is_final
        or new.expires_at is distinct from old.expires_at
        or new.created_at is distinct from old.created_at then
        raise exception 'issued_offer_cannot_be_edited';
    end if;
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- 2. A final offer is answered by accepting or rejecting it, never by another
--    amount (RN-15).
-- ---------------------------------------------------------------------------

-- A trigger and not a condition in the insert policy, so the refusal names its
-- reason: the screen that lost the race against the other side's final offer
-- tells the person why. A definer, so the parent is read whole; and only the two
-- parties of the thread get the named answer, the rule of
-- guard_counter_differs_from_the_offer (20261105024734). The name sorts it ahead
-- of the other insert guards, which fire in name order, so a counter to a final
-- offer hears this reason and not the floor's.
create function public.guard_answer_to_a_final_offer()
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
          and parent.is_final
          and (select auth.uid()) in (r.patient_id, parent.professional_id)
    ) then
        raise exception 'final_offer_admits_no_counter';
    end if;
    return new;
end;
$$;

revoke all on function public.guard_answer_to_a_final_offer() from public, anon, authenticated;

create trigger request_offers_guard_answer_to_a_final_offer
    before insert on public.request_offers
    for each row
    when (new.parent_offer_id is not null)
    execute function public.guard_answer_to_a_final_offer();

-- ---------------------------------------------------------------------------
-- 3. my_request_offers carries the mark to the patient.
-- ---------------------------------------------------------------------------

-- The return type changes, so the function is dropped and created again; the body
-- is the one of 20261027150000 with is_final appended. The professional reads the
-- column straight off request_offers, under the policy that already lets them.
drop function public.my_request_offers(uuid);

create function public.my_request_offers(p_request_id uuid)
returns table (
    id uuid,
    parent_offer_id uuid,
    professional_id uuid,
    professional_name text,
    professional_photo_url text,
    professional_rating numeric,
    amount_bob numeric,
    message text,
    issuer public.offer_issuer,
    status public.offer_status,
    created_at timestamptz,
    distance_m double precision,
    suggested_budget_bob numeric,
    is_final boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        o.id,
        o.parent_offer_id,
        o.professional_id,
        d.full_name as professional_name,
        d.photo_url as professional_photo_url,
        d.average_rating as professional_rating,
        o.amount_bob,
        o.message,
        o.issuer,
        o.status,
        o.created_at,
        round(
            extensions.st_distance(
                extensions.st_setsrid(
                    extensions.st_makepoint(
                        round(extensions.st_x(base.location::extensions.geometry)::numeric, 3)::double precision,
                        round(extensions.st_y(base.location::extensions.geometry)::numeric, 3)::double precision
                    ),
                    4326
                )::extensions.geography,
                r.location
            )::numeric,
            -2
        )::double precision as distance_m,
        r.suggested_budget_bob,
        o.is_final
    from public.request_offers o
    join public.service_requests r on r.id = o.request_id
    left join public.professional_directory d on d.id = o.professional_id
    left join public.addresses base
        on base.profile_id = d.id
       and base.is_professional_base
    where o.request_id = p_request_id
      and r.patient_id = (select auth.uid())
    order by o.created_at asc, o.id asc;
$$;

revoke all on function public.my_request_offers(uuid) from public, anon;
grant execute on function public.my_request_offers(uuid) to authenticated;
