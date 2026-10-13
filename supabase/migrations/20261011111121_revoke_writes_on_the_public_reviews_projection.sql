-- ---------------------------------------------------------------------------
-- The public reviews projection is read-only, and saying so is not optional
-- ---------------------------------------------------------------------------
-- 20261011044809 created professional_reviews with security_invoker = false so
-- its WHERE would be the rule for every caller. It granted select to
-- authenticated and revoked everything from anon, which looked like enough and
-- was the shape professional_directory already had.
--
-- It is not enough, because this view is the first one in the project that is
-- BOTH a definer view AND auto-updatable: one table, no join, no aggregate, no
-- distinct, with the subquery confined to the WHERE. PostgreSQL will happily
-- route an update or a delete through it, and because the view runs with its
-- owner's privileges that write never meets the row level security of
-- public.reviews. Supabase grants a new object to authenticated by default, and
-- the first migration never took that back.
--
-- Measured on 2026-10-11, inside a transaction rolled back: an authenticated
-- professional ran
--   update public.professional_reviews set rating = 5, comment = '...'
--    where recipient_id = <their own id>
-- and the rating they had received was rewritten, text and all. The
-- reviews_recalculate_reputation trigger then recomputed their public average
-- to 5.00 from the rewritten row. A delete with the same filter removes an
-- unflattering comment. public.reviews carries no update and no delete policy
-- on purpose -- a rating is part of the record -- and the view walked around it.
--
-- The audit of the other five views came out clean: my_addresses is
-- auto-updatable but security_invoker = true, so a write through it still faces
-- the policies; professional_directory is a definer view but joins two tables
-- and cannot be written at all.

revoke all on public.professional_reviews from anon, authenticated;

grant select on public.professional_reviews to authenticated;
