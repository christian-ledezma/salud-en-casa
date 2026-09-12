-- Delivery: the agreed service, its payment and the two-way ratings.
-- This migration also adds the policies that could not exist earlier because
-- they depend on a service existing: revealing contact details to the
-- counterpart, and the professional side of the conversation.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- services
-- ---------------------------------------------------------------------------

create table public.services (
    id uuid primary key default gen_random_uuid(),
    request_id uuid not null unique references public.service_requests (id) on delete cascade,
    offer_id uuid not null unique references public.request_offers (id) on delete restrict,
    patient_id uuid not null references public.patients (id) on delete cascade,
    professional_id uuid not null references public.professionals (id) on delete cascade,
    -- INV-04 and RN-07: the agreed amount, frozen. It is never recomputed from
    -- the offer nor from the professional's current rate.
    final_amount_bob numeric(10, 2) not null check (final_amount_bob > 0),
    status public.service_status not null default 'ASSIGNED',
    started_at timestamptz,
    finished_at timestamptz,
    -- RF-10.3: cancelling always carries a reason and an author.
    cancellation_reason text,
    cancelled_by uuid references public.profiles (id) on delete set null,
    cancelled_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint parties_are_different check (patient_id <> professional_id),
    constraint in_progress_has_started check (
        status <> 'IN_PROGRESS' or started_at is not null
    ),
    constraint completed_has_finished check (
        status <> 'COMPLETED' or (started_at is not null and finished_at is not null)
    ),
    constraint cancelled_has_reason check (
        status <> 'CANCELLED'
        or (cancellation_reason is not null and cancelled_by is not null and cancelled_at is not null)
    ),
    constraint finished_after_started check (
        finished_at is null or started_at is null or finished_at >= started_at
    )
);

create index idx_services_patient_id_created_at on public.services (patient_id, created_at desc);
create index idx_services_professional_id_created_at on public.services (professional_id, created_at desc);
create index idx_services_status on public.services (status);

create trigger services_set_updated_at
    before update on public.services
    for each row execute function public.set_updated_at();

-- INV-04: the frozen amount stays frozen, and the two parties never change.
create or replace function public.guard_service_is_frozen()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.final_amount_bob is distinct from old.final_amount_bob
        or new.patient_id is distinct from old.patient_id
        or new.professional_id is distinct from old.professional_id
        or new.request_id is distinct from old.request_id
        or new.offer_id is distinct from old.offer_id then
        raise exception 'agreed_amount_and_parties_are_frozen';
    end if;
    return new;
end;
$$;

create trigger services_guard_frozen
    before update on public.services
    for each row execute function public.guard_service_is_frozen();

-- INV-05: a service is cancelled, never rewritten. COMPLETED and CANCELLED are
-- terminal, and the progression cannot skip steps.
create or replace function public.guard_service_transition()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.status = old.status then
        return new;
    end if;
    if old.status in ('COMPLETED', 'CANCELLED') then
        raise exception 'service_status_is_terminal';
    end if;
    if old.status = 'ASSIGNED' and new.status not in ('IN_PROGRESS', 'CANCELLED') then
        raise exception 'invalid_service_transition';
    end if;
    if old.status = 'IN_PROGRESS' and new.status not in ('COMPLETED', 'CANCELLED') then
        raise exception 'invalid_service_transition';
    end if;
    return new;
end;
$$;

create trigger services_guard_transition
    before update of status on public.services
    for each row execute function public.guard_service_transition();

alter table public.services enable row level security;

create policy services_select_participants on public.services
    for select to authenticated
    using (
        patient_id = (select auth.uid())
        or professional_id = (select auth.uid())
    );

create policy services_select_admin on public.services
    for select to authenticated
    using (public.is_admin());

create policy services_update_participants on public.services
    for update to authenticated
    using (
        patient_id = (select auth.uid())
        or professional_id = (select auth.uid())
    )
    with check (
        patient_id = (select auth.uid())
        or professional_id = (select auth.uid())
    );

-- No insert policy and no delete policy: a service is created by the server
-- side function that accepts an offer, in a single transaction (RF-08.5), and
-- the record is never removed (INV-05).

-- ---------------------------------------------------------------------------
-- payments
-- ---------------------------------------------------------------------------

create table public.payments (
    id uuid primary key default gen_random_uuid(),
    service_id uuid not null unique references public.services (id) on delete cascade,
    total_amount_bob numeric(10, 2) not null check (total_amount_bob > 0),
    platform_fee_bob numeric(10, 2) not null check (platform_fee_bob >= 0),
    professional_amount_bob numeric(10, 2) not null check (professional_amount_bob >= 0),
    method public.payment_method,
    provider text,
    external_reference text,
    status public.payment_status not null default 'PENDING',
    -- INV-09: each party confirms separately. Neither timestamp is written by
    -- the other side.
    patient_confirmed_at timestamptz,
    professional_confirmed_at timestamptz,
    settled_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    -- INV-10: an engine constraint, not an application validation.
    constraint total_equals_fee_plus_professional_amount check (
        total_amount_bob = platform_fee_bob + professional_amount_bob
    ),
    -- INV-09: no unilateral confirmation closes a payment.
    constraint confirmations_match_status check (
        (status = 'PENDING'
            and patient_confirmed_at is null
            and professional_confirmed_at is null)
        or (status = 'PATIENT_CONFIRMED'
            and patient_confirmed_at is not null
            and professional_confirmed_at is null)
        or (status in ('BOTH_CONFIRMED', 'SETTLED')
            and patient_confirmed_at is not null
            and professional_confirmed_at is not null)
        or status = 'DISPUTED'
    ),
    constraint settled_has_timestamp check (
        status <> 'SETTLED' or settled_at is not null
    )
);

create index idx_payments_status on public.payments (status);
create index idx_payments_settled_at on public.payments (settled_at);

create trigger payments_set_updated_at
    before update on public.payments
    for each row execute function public.set_updated_at();

-- INV-05: a payment is disputed, never rewritten. The breakdown is fixed when
-- the service is agreed.
create or replace function public.guard_payment_is_frozen()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.service_id is distinct from old.service_id
        or new.total_amount_bob is distinct from old.total_amount_bob
        or new.platform_fee_bob is distinct from old.platform_fee_bob
        or new.professional_amount_bob is distinct from old.professional_amount_bob then
        raise exception 'payment_breakdown_is_frozen';
    end if;
    return new;
end;
$$;

create trigger payments_guard_frozen
    before update on public.payments
    for each row execute function public.guard_payment_is_frozen();

alter table public.payments enable row level security;

create policy payments_select_participants on public.payments
    for select to authenticated
    using (
        exists (
            select 1
            from public.services s
            where s.id = payments.service_id
              and (s.patient_id = (select auth.uid()) or s.professional_id = (select auth.uid()))
        )
    );

create policy payments_select_admin on public.payments
    for select to authenticated
    using (public.is_admin());

-- INV-09: each party confirms separately. Neither side may write the other
-- side's confirmation, and settlement is recorded by an administrator. Without
-- this, the row policy alone would let a patient close a payment on their own.
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
begin
    -- No authenticated user means the service key or a definer function is
    -- writing. Those already bypass row level security, so there is nothing to
    -- add here.
    if v_caller is null or public.is_admin() then
        return new;
    end if;

    select s.patient_id, s.professional_id
    into v_patient, v_professional
    from public.services s
    where s.id = new.service_id;

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

    if new.settled_at is distinct from old.settled_at or new.status = 'SETTLED' then
        raise exception 'settlement_is_recorded_by_an_administrator';
    end if;

    return new;
end;
$$;

create trigger payments_guard_confirmation
    before update on public.payments
    for each row execute function public.guard_payment_confirmation();

create policy payments_update_participants on public.payments
    for update to authenticated
    using (
        exists (
            select 1
            from public.services s
            where s.id = payments.service_id
              and (s.patient_id = (select auth.uid()) or s.professional_id = (select auth.uid()))
        )
    )
    with check (true);

-- RF-11.5 and RF-11.7: the administrator resolves disputes and settles.
create policy payments_update_admin on public.payments
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

-- ---------------------------------------------------------------------------
-- reviews
-- ---------------------------------------------------------------------------

-- INV-11: one rating per service and author, between 1 and 5, and the author is
-- never the recipient.
create table public.reviews (
    id uuid primary key default gen_random_uuid(),
    service_id uuid not null references public.services (id) on delete cascade,
    author_id uuid not null references public.profiles (id) on delete cascade,
    recipient_id uuid not null references public.profiles (id) on delete cascade,
    rating smallint not null check (rating between 1 and 5),
    comment text check (comment is null or char_length(comment) <= 1000),
    visible boolean not null default true,
    created_at timestamptz not null default now(),
    unique (service_id, author_id),
    constraint author_is_not_recipient check (author_id <> recipient_id)
);

create index idx_reviews_recipient_id_created_at on public.reviews (recipient_id, created_at desc);
create index idx_reviews_service_id on public.reviews (service_id);

alter table public.reviews enable row level security;

-- RF-12.4 and HU-30: reputation is public to signed in users.
create policy reviews_select_visible on public.reviews
    for select to authenticated
    using (visible);

create policy reviews_select_own on public.reviews
    for select to authenticated
    using (author_id = (select auth.uid()) or recipient_id = (select auth.uid()));

create policy reviews_select_admin on public.reviews
    for select to authenticated
    using (public.is_admin());

-- RN-10: only over a completed service, only by one of its two parties, and
-- only about the other one.
create policy reviews_insert_participant on public.reviews
    for insert to authenticated
    with check (
        author_id = (select auth.uid())
        and exists (
            select 1
            from public.services s
            where s.id = reviews.service_id
              and s.status = 'COMPLETED'
              and (
                  (s.patient_id = (select auth.uid()) and s.professional_id = reviews.recipient_id)
                  or (s.professional_id = (select auth.uid()) and s.patient_id = reviews.recipient_id)
              )
        )
    );

-- No update and no delete policy: a rating is part of the record.

-- ---------------------------------------------------------------------------
-- Policies that depend on a service existing
-- ---------------------------------------------------------------------------

-- RN-06 and RF-08.6: contact details are revealed only after an offer has been
-- accepted, which is exactly when a service exists for both parties.
create or replace function public.shares_service_with(p_profile_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.services s
        where (s.patient_id = (select auth.uid()) and s.professional_id = p_profile_id)
           or (s.professional_id = (select auth.uid()) and s.patient_id = p_profile_id)
    );
$$;

create policy profiles_select_counterpart on public.profiles
    for select to authenticated
    using (public.shares_service_with(id));

create policy patients_select_counterpart on public.patients
    for select to authenticated
    using (public.shares_service_with(id));

create policy professionals_select_counterpart on public.professionals
    for select to authenticated
    using (public.shares_service_with(id));

-- HU-21: the professional side of the conversation. There is no administrator
-- policy on messages, here or anywhere else (INV-12, RN-11).
create policy messages_select_professional on public.messages
    for select to authenticated
    using (
        exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.professional_id = (select auth.uid())
        )
    );

create policy messages_insert_professional on public.messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.professional_id = (select auth.uid())
        )
    );

create policy messages_update_professional on public.messages
    for update to authenticated
    using (
        sender_id <> (select auth.uid())
        and exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.professional_id = (select auth.uid())
        )
    )
    with check (sender_id <> (select auth.uid()));
