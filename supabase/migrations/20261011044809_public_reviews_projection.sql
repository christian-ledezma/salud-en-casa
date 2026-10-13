-- ---------------------------------------------------------------------------
-- The comments a professional's profile shows are a projection, not the table
-- ---------------------------------------------------------------------------
-- HU-12 lists the comments a professional received. reviews_select_visible
-- already says exactly which ones are public -- visible, received on the
-- professional side of that service, and about somebody the directory carries
-- -- so a client query over public.reviews looked like enough.
--
-- It is not, because reviews_select_own is a second permissive policy on the
-- same table: whoever the review is about may also read it. Row level security
-- ORs its permissive policies, so the one caller who is both the subject of the
-- profile and its reader -- a professional opening their own public preview --
-- reads strictly more than a patient does.
--
-- Measured against this project on 2026-10-11, inside a transaction rolled
-- back: with two ratings aimed at the same dual-role person, a third party saw
-- only the one received as a professional, and the person themselves saw both,
-- the patient-side one included. That is INV-17 broken on the screen that is
-- supposed to show what a patient sees, and no client filter can repair it:
-- deciding which side of a service somebody was on means reading
-- public.services, which a third party cannot.
--
-- So the rule moves where professional_directory already keeps its own: into a
-- projection that runs with its owner's privileges, whose WHERE is the rule
-- itself and is therefore the same for every caller.
--
-- author_id is not projected. The profile shows what was said and when, never
-- who said it, and no policy would let a third party resolve that name anyway
-- (docs/decisions.md, 2026-10-11).

create view public.professional_reviews
with (security_invoker = false) as
select
    r.id,
    r.recipient_id,
    r.rating,
    r.comment,
    r.created_at
from public.reviews r
where r.visible
  and public.was_the_professional_of(r.service_id, r.recipient_id)
  and exists (
      select 1 from public.professional_directory d where d.id = r.recipient_id
  );

revoke all on public.professional_reviews from anon;
grant select on public.professional_reviews to authenticated;
