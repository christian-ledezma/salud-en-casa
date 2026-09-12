-- Requests, the offer thread and the conversation attached to a request.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- service_requests
-- ---------------------------------------------------------------------------

create table public.service_requests (
    id uuid primary key default gen_random_uuid(),
    patient_id uuid not null references public.patients (id) on delete cascade,
    -- Only a scheduled request is aimed at a specific professional (RF-07.3).
    professional_id uuid references public.professionals (id) on delete set null,
    service_type_id uuid not null references public.service_types (id) on delete restrict,
    -- Kept only for traceability. The request never reads its location back
    -- from here: the columns below are the snapshot.
    address_id uuid references public.addresses (id) on delete set null,
    -- INV-03: where the service was asked for, frozen at creation time. It is
    -- never recomputed from addresses when reading.
    location extensions.geography(Point, 4326) not null,
    address_text text not null,
    city text not null,
    reference text,
    modality public.request_modality not null,
    scheduled_at timestamptz,
    description text,
    suggested_budget_bob numeric(10, 2) check (suggested_budget_bob > 0),
    status public.request_status not null default 'PUBLISHED',
    -- RF-07.5: an immediate request expires if nobody offers.
    expires_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint scheduled_request_is_complete check (
        modality <> 'SCHEDULED'
        or (scheduled_at is not null and professional_id is not null)
    ),
    constraint immediate_request_expires check (
        modality <> 'IMMEDIATE' or expires_at is not null
    )
);

-- Without this index the proximity query degrades to a sequential scan (INV-08).
create index idx_service_requests_location on public.service_requests using gist (location);
create index idx_service_requests_patient_id on public.service_requests (patient_id);
create index idx_service_requests_professional_id on public.service_requests (professional_id);
create index idx_service_requests_status_created_at on public.service_requests (status, created_at desc);

create trigger service_requests_set_updated_at
    before update on public.service_requests
    for each row execute function public.set_updated_at();

-- INV-03: the snapshot is a snapshot. Moving or editing the address afterwards
-- must not move a service that was already requested somewhere else.
create or replace function public.guard_request_location()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    -- Compared as text on purpose: the equality operator on spatial types
    -- compares bounding boxes, not the exact point.
    if new.location::text is distinct from old.location::text
        or new.address_text is distinct from old.address_text then
        raise exception 'request_location_is_a_snapshot';
    end if;
    return new;
end;
$$;

create trigger service_requests_guard_location
    before update on public.service_requests
    for each row execute function public.guard_request_location();

-- Whether the calling professional covers a given point for a given service
-- type. It deliberately does not read service_requests: a policy on that table
-- calling a function that queried it would recurse. RN-02 fixes the rule: the
-- professional's primary address and their declared coverage radius.
create or replace function public.professional_covers(
    p_location extensions.geography,
    p_service_type_id uuid
)
returns boolean
language sql
stable
set search_path = ''
as $$
    select exists (
        select 1
        from public.professionals pro
        join public.profiles p on p.id = pro.id
        join public.addresses a on a.profile_id = pro.id and a.is_primary
        where pro.id = (select auth.uid())
          and pro.verification_status = 'APPROVED'
          and p.active
          and extensions.st_dwithin(a.location, p_location, pro.coverage_radius_km * 1000)
          and exists (
              select 1
              from public.professional_services ps
              where ps.professional_id = pro.id
                and ps.service_type_id = p_service_type_id
                and ps.active
          )
    );
$$;

alter table public.service_requests enable row level security;

create policy service_requests_select_own on public.service_requests
    for select to authenticated
    using (patient_id = (select auth.uid()));

-- The professional a scheduled request is aimed at.
create policy service_requests_select_addressee on public.service_requests
    for select to authenticated
    using (professional_id = (select auth.uid()));

-- RF-07.4 and HU-14: the open inbox. An accepted request leaves the inbox
-- because the status no longer matches (RN-04).
create policy service_requests_select_inbox on public.service_requests
    for select to authenticated
    using (
        modality = 'IMMEDIATE'
        and status in ('PUBLISHED', 'NEGOTIATING')
        and public.professional_covers(location, service_type_id)
    );

-- RF-13.3: the administrator sees the central record.
create policy service_requests_select_admin on public.service_requests
    for select to authenticated
    using (public.is_admin());

create policy service_requests_insert_own on public.service_requests
    for insert to authenticated
    with check (patient_id = (select auth.uid()));

create policy service_requests_update_own on public.service_requests
    for update to authenticated
    using (patient_id = (select auth.uid()))
    with check (patient_id = (select auth.uid()));

create policy service_requests_update_admin on public.service_requests
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

-- ---------------------------------------------------------------------------
-- request_offers
-- ---------------------------------------------------------------------------

-- INV-06: append only. A counteroffer is a new row pointing at the previous one
-- through parent_offer_id. An issued offer is never edited (RN-05).
create table public.request_offers (
    id uuid primary key default gen_random_uuid(),
    request_id uuid not null references public.service_requests (id) on delete cascade,
    professional_id uuid not null references public.professionals (id) on delete cascade,
    parent_offer_id uuid references public.request_offers (id) on delete restrict,
    issuer public.offer_issuer not null,
    amount_bob numeric(10, 2) not null check (amount_bob > 0),
    message text,
    status public.offer_status not null default 'PROPOSED',
    expires_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_request_offers_request_id_created_at
    on public.request_offers (request_id, created_at);
create index idx_request_offers_professional_id on public.request_offers (professional_id);
create index idx_request_offers_parent_offer_id on public.request_offers (parent_offer_id);

-- The amount and the authorship of an issued offer are immutable. Only the
-- outcome of the negotiation may change (RN-05, INV-06).
create or replace function public.guard_offer_is_append_only()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.request_id is distinct from old.request_id
        or new.professional_id is distinct from old.professional_id
        or new.parent_offer_id is distinct from old.parent_offer_id
        or new.issuer is distinct from old.issuer
        or new.amount_bob is distinct from old.amount_bob
        or new.message is distinct from old.message
        or new.created_at is distinct from old.created_at then
        raise exception 'issued_offer_cannot_be_edited';
    end if;
    return new;
end;
$$;

create trigger request_offers_guard_append_only
    before update on public.request_offers
    for each row execute function public.guard_offer_is_append_only();

alter table public.request_offers enable row level security;

create policy request_offers_select_participants on public.request_offers
    for select to authenticated
    using (
        professional_id = (select auth.uid())
        or exists (
            select 1
            from public.service_requests r
            where r.id = request_offers.request_id
              and r.patient_id = (select auth.uid())
        )
    );

create policy request_offers_select_admin on public.request_offers
    for select to authenticated
    using (public.is_admin());

-- A professional offers on an open request only while verified and active
-- (INV-07, RN-01) and only while the request admits offers (RN-04). The patient
-- counteroffers on their own request.
create policy request_offers_insert_participants on public.request_offers
    for insert to authenticated
    with check (
        exists (
            select 1
            from public.service_requests r
            where r.id = request_offers.request_id
              and r.status in ('PUBLISHED', 'NEGOTIATING')
              and (
                  (
                      request_offers.issuer = 'PROFESSIONAL'
                      and request_offers.professional_id = (select auth.uid())
                      and exists (
                          select 1
                          from public.professional_directory d
                          where d.id = (select auth.uid())
                      )
                  )
                  or (
                      request_offers.issuer = 'PATIENT'
                      and r.patient_id = (select auth.uid())
                  )
              )
        )
    );

create policy request_offers_update_participants on public.request_offers
    for update to authenticated
    using (
        professional_id = (select auth.uid())
        or exists (
            select 1
            from public.service_requests r
            where r.id = request_offers.request_id
              and r.patient_id = (select auth.uid())
        )
    )
    with check (true);

-- No delete policy: the thread is evidence of the negotiation (INV-06).

-- ---------------------------------------------------------------------------
-- messages
-- ---------------------------------------------------------------------------

-- RF-09.1 and HU-21: the conversation attached to a request, available once an
-- offer has been accepted. It also holds the detail of what was done during the
-- service (RF-09.3, FA-01), which is why it is sensitive information.
create table public.messages (
    id uuid primary key default gen_random_uuid(),
    request_id uuid not null references public.service_requests (id) on delete cascade,
    sender_id uuid not null references public.profiles (id) on delete cascade,
    content text not null check (char_length(content) between 1 and 4000),
    read_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_messages_request_id_created_at on public.messages (request_id, created_at desc);
create index idx_messages_sender_id on public.messages (sender_id);

alter table public.messages enable row level security;

-- INV-12 and RN-11: there is no administrator policy on this table, here or in
-- any later migration. The conversation carries the detail of the care given.
--
-- The patient side is expressed here. The professional side depends on the
-- accepted service and is added by the migration that creates services.
create policy messages_select_patient on public.messages
    for select to authenticated
    using (
        exists (
            select 1
            from public.service_requests r
            where r.id = messages.request_id
              and r.patient_id = (select auth.uid())
        )
    );

create policy messages_insert_patient on public.messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and exists (
            select 1
            from public.service_requests r
            where r.id = messages.request_id
              and r.patient_id = (select auth.uid())
        )
    );

-- The recipient marks a message as read (RF-09.2). The sender never edits a
-- message already sent. Being the recipient is not enough on its own: the
-- caller must be a party of the conversation, or anyone could mark messages of
-- conversations they cannot even read.
create policy messages_update_patient on public.messages
    for update to authenticated
    using (
        sender_id <> (select auth.uid())
        and exists (
            select 1
            from public.service_requests r
            where r.id = messages.request_id
              and r.patient_id = (select auth.uid())
        )
    )
    with check (sender_id <> (select auth.uid()));
