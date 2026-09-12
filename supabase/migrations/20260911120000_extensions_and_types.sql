-- Extensions and the twelve enumerated types of the domain.
-- Enum values are written in SCREAMING_SNAKE_CASE so they map one to one onto
-- the Kotlin enum constants. See .claude/rules/glosario.md.

create extension if not exists postgis with schema extensions;

create type public.user_role as enum ('PATIENT', 'PROFESSIONAL', 'ADMIN');

-- Outcome of a manual review. Used by verification documents and by the
-- verification status of a professional.
create type public.review_status as enum ('PENDING', 'APPROVED', 'REJECTED');

create type public.professional_type as enum ('DOCTOR', 'NURSE', 'PHYSIOTHERAPIST', 'STUDENT');

create type public.document_type as enum ('ID_FRONT', 'ID_BACK', 'SELFIE', 'DEGREE', 'LICENSE', 'STUDENT_CARD');

create type public.request_modality as enum ('IMMEDIATE', 'SCHEDULED');

create type public.request_status as enum (
    'PUBLISHED', 'NEGOTIATING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'EXPIRED'
);

create type public.offer_issuer as enum ('PROFESSIONAL', 'PATIENT');

create type public.offer_status as enum ('PROPOSED', 'ACCEPTED', 'REJECTED', 'EXPIRED');

create type public.service_status as enum ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');

create type public.payment_method as enum ('CASH', 'DIRECT_QR', 'GATEWAY');

create type public.payment_status as enum (
    'PENDING', 'PATIENT_CONFIRMED', 'BOTH_CONFIRMED', 'SETTLED', 'DISPUTED'
);

create type public.device_platform as enum ('ANDROID', 'IOS');
