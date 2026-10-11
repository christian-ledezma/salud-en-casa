-- ---------------------------------------------------------------------------
-- The counterpart reads the public projection, not the professional's row
-- ---------------------------------------------------------------------------
-- Debt recorded on 2026-10-05 and handed to HU-11 by the Sprint 3
-- retrospective. This policy granted a read of the WHOLE professionals row to
-- anyone sharing a service, verification_status included, so a counterpart
-- could see a PENDING or a REJECTED. Row level security resolves rows and not
-- columns, so there is no way to allow the row and hide that column: the row
-- is read whole or it is not read.
--
-- What a counterpart needs is in professional_directory, which by construction
-- holds only APPROVED and active professionals and carries no phone and no
-- email. Belonging to that view IS the verification, which is the structural
-- form of RF-04.6 (docs/decisions.md, 2026-10-09).
--
-- profiles_select_counterpart stays: it is the one that reveals name, photo
-- and phone once an offer is accepted (RN-06, RF-08.6).

drop policy professionals_select_counterpart on public.professionals;
