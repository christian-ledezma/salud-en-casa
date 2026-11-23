-- HT-30: the platform no longer keeps a commission on each attention. A payment
-- is the agreed amount, whole, and belongs to the professional (INV-10).
-- docs/decisions.md, 2026-11-22.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. The functions stop reading the columns before the columns go.
-- ---------------------------------------------------------------------------

create or replace function public.accept_offer(p_offer_id uuid)
returns uuid
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_offer public.request_offers;
    v_request public.service_requests;
    v_service_id uuid;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    select * into v_offer from public.request_offers where id = p_offer_id;
    if not found then
        raise exception 'offer_not_found';
    end if;

    if v_offer.status <> 'PROPOSED' then
        raise exception 'offer_not_open';
    end if;

    if exists (
        select 1 from public.request_offers a where a.parent_offer_id = v_offer.id
    ) then
        raise exception 'offer_already_answered';
    end if;

    select * into v_request from public.service_requests where id = v_offer.request_id;
    if v_request.status not in ('PUBLISHED', 'NEGOTIATING') then
        raise exception 'request_not_open';
    end if;

    -- INV-06: a professional-issued offer is accepted by the patient; a patient's
    -- counter-offer, by the professional of the thread.
    if v_offer.issuer = 'PROFESSIONAL' then
        if v_caller <> v_request.patient_id then
            raise exception 'not_your_offer_to_accept';
        end if;
    else
        if v_caller <> v_offer.professional_id then
            raise exception 'not_your_offer_to_accept';
        end if;
    end if;

    insert into public.services (
        request_id, offer_id, patient_id, professional_id, final_amount_bob
    ) values (
        v_request.id, v_offer.id, v_request.patient_id, v_offer.professional_id, v_offer.amount_bob
    )
    returning id into v_service_id;

    insert into public.payments (service_id, total_amount_bob)
    values (v_service_id, v_offer.amount_bob);

    update public.request_offers set status = 'ACCEPTED' where id = v_offer.id;

    update public.service_requests
       set status = 'ACCEPTED',
           professional_id = v_offer.professional_id
     where id = v_request.id;

    return v_service_id;
end;
$$;

create or replace function public.guard_payment_is_frozen()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.service_id is distinct from old.service_id
        or new.total_amount_bob is distinct from old.total_amount_bob then
        raise exception 'payment_breakdown_is_frozen';
    end if;
    return new;
end;
$$;

create or replace function public.guard_payment_confirmation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_patient uuid;
    v_professional uuid;
    v_service_status public.service_status;
begin
    -- No authenticated user means the service key or a definer function is
    -- writing. Those already bypass row level security, so there is nothing to
    -- add here.
    if v_caller is null then
        return new;
    end if;

    select s.patient_id, s.professional_id, s.status
    into v_patient, v_professional, v_service_status
    from public.services s
    where s.id = new.service_id;

    if new.status is distinct from old.status
        and (old.status = 'CANCELLED' or (new.status = 'CANCELLED' and v_service_status <> 'CANCELLED')) then
        raise exception 'payment_cancellation_follows_the_service';
    end if;

    if public.is_admin() then
        return new;
    end if;

    if v_caller = v_patient then
        if new.professional_confirmed_at is distinct from old.professional_confirmed_at then
            raise exception 'each_party_confirms_separately';
        end if;
    elsif v_caller = v_professional then
        if new.patient_confirmed_at is distinct from old.patient_confirmed_at then
            raise exception 'each_party_confirms_separately';
        end if;
    else
        raise exception 'not_a_party_of_this_payment';
    end if;

    if (new.patient_confirmed_at is distinct from old.patient_confirmed_at
        or new.professional_confirmed_at is distinct from old.professional_confirmed_at)
        and v_service_status <> 'COMPLETED' then
        raise exception 'payment_confirmation_requires_a_completed_service';
    end if;

    if new.status = 'DISPUTED' and old.status is distinct from 'DISPUTED' then
        raise exception 'dispute_requires_the_guarded_resolution_path';
    end if;

    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- 2. The commission leaves the schema.
-- ---------------------------------------------------------------------------

alter table public.payments
    drop constraint total_equals_fee_plus_professional_amount,
    drop constraint settled_has_timestamp,
    drop constraint confirmations_match_status,
    drop column platform_fee_bob,
    drop column professional_amount_bob,
    drop column settled_at;

alter table public.platform_settings
    drop column platform_fee_percent;

-- ---------------------------------------------------------------------------
-- 3. SETTLED leaves payment_status. PostgreSQL cannot drop an enum value, so the
--    type is recreated, as document_type was in 20261005120000.
-- ---------------------------------------------------------------------------

alter table public.payments
    alter column status drop default;

alter type public.payment_status rename to payment_status_old;

create type public.payment_status as enum (
    'PENDING', 'PATIENT_CONFIRMED', 'BOTH_CONFIRMED', 'DISPUTED', 'CANCELLED'
);

alter table public.payments
    alter column status type public.payment_status
    using (status::text::public.payment_status);

alter table public.payments
    alter column status set default 'PENDING';

drop type public.payment_status_old;

-- INV-09: no unilateral confirmation closes a payment.
alter table public.payments
    add constraint confirmations_match_status check (
        (status in ('PENDING', 'CANCELLED')
            and patient_confirmed_at is null
            and professional_confirmed_at is null)
        or (status = 'PATIENT_CONFIRMED'
            and patient_confirmed_at is not null
            and professional_confirmed_at is null)
        or (status = 'BOTH_CONFIRMED'
            and patient_confirmed_at is not null
            and professional_confirmed_at is not null)
        or status = 'DISPUTED'
    );

-- ---------------------------------------------------------------------------
-- 4. INV-10 is an engine rule: a payment is born for the agreed amount, and
--    guard_payment_is_frozen keeps it there.
-- ---------------------------------------------------------------------------

create function public.guard_payment_matches_the_service()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.total_amount_bob is distinct from (
        select s.final_amount_bob from public.services s where s.id = new.service_id
    ) then
        raise exception 'payment_total_must_equal_the_agreed_amount';
    end if;
    return new;
end;
$$;

revoke all on function public.guard_payment_matches_the_service() from public, anon, authenticated;

create trigger payments_guard_total_matches_service
    before insert on public.payments
    for each row execute function public.guard_payment_matches_the_service();
