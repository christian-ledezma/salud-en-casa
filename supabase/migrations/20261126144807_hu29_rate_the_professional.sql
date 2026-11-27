-- HU-29: the patient rates the professional of a completed attention. The table, its
-- policies, the uniqueness and the reputation trigger already exist; what was missing
-- is the engine's hold on two columns and the patient's read of their own rating.
-- docs/decisions.md, 2026-11-26, HU-29.

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. The clock and the visibility are the engine's.
-- ---------------------------------------------------------------------------

-- The insert policy never looked at created_at or visible, so a client could have
-- dated a rating in the past, which orders the profile's comments, or published it
-- already hidden. A definer or the service key, which have no caller, keep theirs:
-- hiding a rating is a moderation act and belongs to them.
create function public.stamp_review_on_insert()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if (select auth.uid()) is not null then
        new.created_at := now();
        new.visible := true;
    end if;
    return new;
end;
$$;

create trigger reviews_stamp_on_insert
    before insert on public.reviews
    for each row execute function public.stamp_review_on_insert();

-- ---------------------------------------------------------------------------
-- 2. my_attention carries my own rating of the attention.
-- ---------------------------------------------------------------------------

-- The return type changes, so the function is dropped and created again; the body
-- is the one of 20261124150053 with the three columns appended. Only the rating the
-- reader wrote comes back: the one the other party wrote never does (RF-12.2).
drop function public.my_attention(uuid);

create function public.my_attention(p_request_id uuid)
returns table (
    service_id uuid,
    request_id uuid,
    status public.service_status,
    final_amount_bob numeric,
    service_type_name text,
    created_at timestamptz,
    counterpart_id uuid,
    counterpart_name text,
    counterpart_photo_url text,
    i_am_patient boolean,
    scheduled_at timestamptz,
    started_at timestamptz,
    finished_at timestamptz,
    description text,
    city text,
    address_text text,
    reference text,
    latitude double precision,
    longitude double precision,
    cancellation_reason text,
    cancelled_at timestamptz,
    cancelled_by_me boolean,
    conversation_open boolean,
    conversation_writable_until timestamptz,
    payment_status public.payment_status,
    payment_method public.payment_method,
    patient_confirmed_at timestamptz,
    professional_confirmed_at timestamptz,
    disputed_at timestamptz,
    resolved_at timestamptz,
    my_rating smallint,
    my_review_comment text,
    my_reviewed_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        s.id,
        s.request_id,
        s.status,
        s.final_amount_bob,
        t.name as service_type_name,
        s.created_at,
        cp.id as counterpart_id,
        cp.full_name as counterpart_name,
        cp.photo_url as counterpart_photo_url,
        (s.patient_id = (select auth.uid())) as i_am_patient,
        r.scheduled_at,
        s.started_at,
        s.finished_at,
        r.description,
        r.city,
        case when shows_address then r.address_text end,
        case when shows_address then r.reference end,
        case when shows_address then extensions.st_y(r.location::extensions.geometry) end,
        case when shows_address then extensions.st_x(r.location::extensions.geometry) end,
        s.cancellation_reason,
        s.cancelled_at,
        s.cancelled_by = (select auth.uid()),
        public.conversation_is_open(s.status, s.finished_at),
        case when s.status = 'COMPLETED' then public.conversation_writable_until(s.finished_at) end,
        pay.status,
        pay.method,
        pay.patient_confirmed_at,
        pay.professional_confirmed_at,
        pay.disputed_at,
        pay.resolved_at,
        rv.rating,
        rv.comment,
        rv.created_at
    from public.services s
    join public.service_requests r on r.id = s.request_id
    join public.service_types t on t.id = r.service_type_id
    join public.payments pay on pay.service_id = s.id
    join public.profiles cp
        on cp.id = case
            when s.patient_id = (select auth.uid()) then s.professional_id
            else s.patient_id
        end
    left join public.reviews rv
        on rv.service_id = s.id
       and rv.author_id = (select auth.uid())
    cross join lateral (
        select (
            s.patient_id = (select auth.uid())
            or s.status in ('ASSIGNED', 'IN_PROGRESS')
        ) as shows_address
    ) visibility
    where s.request_id = p_request_id
      and (s.patient_id = (select auth.uid()) or s.professional_id = (select auth.uid()));
$$;

revoke all on function public.my_attention(uuid) from public, anon;
grant execute on function public.my_attention(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 3. Nobody reads a rating's author off the table but the author.
-- ---------------------------------------------------------------------------

-- professional_reviews already leaves author_id out of what the profile shows, but
-- the table still answered any signed in user with every public rating, author
-- included, and answered the rated professional with the author of each: the
-- patient of that attention, by its service_id. The profile reads through the
-- projection, a definer, and my_attention is one too, so no screen reads this table
-- as the caller. What stays is what a person wrote and the administrator's read
-- (INV-13, docs/decisions.md, 2026-11-26, HU-29).
drop policy reviews_select_visible on public.reviews;
drop policy reviews_select_own on public.reviews;

create policy reviews_select_own on public.reviews
    for select to authenticated
    using (author_id = (select auth.uid()));
