-- HU-24: a cancelled attention leaves no payment pending. docs/decisions.md,
-- 2026-11-04, HU-24.
--
-- On its own because PostgreSQL does not let a transaction use the enum value it
-- added, and 20261104190100 uses it in a check constraint.

alter type public.payment_status add value 'CANCELLED';
