-- HU-07: upload and store verification documents. See docs/decisions.md,
-- 2026-10-05 (OTHER + caption, insert_own gap, path contract, storage).

set search_path = public, extensions;

-- ---------------------------------------------------------------------------
-- 1. OTHER joins document_type
-- ---------------------------------------------------------------------------

alter type public.document_type rename to document_type_old;

create type public.document_type as enum (
    'ID_FRONT',
    'ID_BACK',
    'SELFIE',
    'DEGREE',
    'LICENSE',
    'STUDENT_CARD',
    'OTHER'
);

alter table public.verification_documents
    alter column document_type type public.document_type
    using (document_type::text::public.document_type);

drop type public.document_type_old;

-- ---------------------------------------------------------------------------
-- 2. caption on verification_documents, required for OTHER, forbidden otherwise
-- ---------------------------------------------------------------------------

alter table public.verification_documents
    add column caption text;

-- The equality check reads as a biconditional: both sides are true for OTHER,
-- both false for every fixed type. A one-sided check would let a caption slip
-- into an ID_FRONT row or an OTHER row arrive without one.
alter table public.verification_documents
    add constraint verification_documents_caption_matches_type
    check ((document_type = 'OTHER') = (caption is not null));

-- A short caption is a sentence of context, not a paragraph. 120 characters
-- matches the length of a form field label, which is where it is written.
alter table public.verification_documents
    add constraint verification_documents_caption_length
    check (caption is null or char_length(caption) between 1 and 120);

-- storage_path must sit under the owner's own folder (RF-04.3, INV-13).
--
-- Without this, the client could insert a row that points at another user's
-- object: the storage.objects policies would still deny the owner from reading
-- that file, but verification_documents_storage_select_admin lets the admin
-- read any file in the bucket. The admin reviewing a crafted row would open
-- the signed URL of a legitimate document belonging to someone else and
-- approve the attacker on the strength of it -- a confused deputy that lets a
-- person be verified by impersonating another one's documents. Pinning the
-- first path segment to the owner's profile closes it in the schema, where
-- the row is written, instead of relying on the client to construct the path
-- correctly.
alter table public.verification_documents
    add constraint verification_documents_storage_path_matches_owner
    check (split_part(storage_path, '/', 1) = profile_id::text);

-- One storage_path belongs to at most one row (RF-04.4). The
-- (profile_id, document_type) uniqueness alone would let one owner insert two
-- rows of different types that point at the same object, by passing identical
-- storage_path values. Approving one of them and leaving the other PENDING
-- would then let the owner overwrite the approved document, because the
-- update policy on storage.objects only requires that SOME PENDING row
-- reference the object. Making storage_path unique collapses the attack: the
-- exists subquery can only ever match the single row that owns the file, and
-- a reviewed row keeps its file frozen.
alter table public.verification_documents
    add constraint verification_documents_storage_path_unique
    unique (storage_path);

-- ---------------------------------------------------------------------------
-- 3. Close the insert_own gap on verification_documents
-- ---------------------------------------------------------------------------

-- The pre-existing policy only asked that profile_id be the caller. Nothing
-- stopped the caller from inserting their own row already APPROVED, reviewed by
-- themselves, with a stamp of now(): the guard_document_review trigger is a
-- `before update` and never saw the row.
drop policy verification_documents_insert_own on public.verification_documents;

create policy verification_documents_insert_own on public.verification_documents
    for insert to authenticated
    with check (
        profile_id = (select auth.uid())
        and status = 'PENDING'
        and reviewed_by is null
        and reviewed_at is null
        and rejection_reason is null
    );

-- ---------------------------------------------------------------------------
-- 4. Private Storage bucket for verification documents
-- ---------------------------------------------------------------------------

-- 2 MiB after client-side compression (RF-04.3 and compose.md). Larger means
-- the compressor did not run; smaller is fine. image/jpeg is the shape the
-- client produces, image/png is accepted for a document that was already in
-- that format on the device.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'verification-documents',
    'verification-documents',
    false,
    2097152,
    array['image/jpeg', 'image/png']
)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- 5. Policies on storage.objects (RF-04.3, INV-13)
-- ---------------------------------------------------------------------------

-- Path contract: '<profile_id>/<document_type_or_other_id>.jpg'. The first
-- segment of the object name is the owner's identifier, so the policy asks for
-- (storage.foldername(name))[1] = auth.uid()::text. The verification_documents
-- table carries the full path in storage_path; the two must agree, which the
-- application enforces by building the path from the signed-in user's id.

create policy verification_documents_storage_select_own on storage.objects
    for select to authenticated
    using (
        bucket_id = 'verification-documents'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

create policy verification_documents_storage_insert_own on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'verification-documents'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

-- Same restriction as the table's update_own: an admin review freezes the file
-- too, so the owner cannot overwrite it after that point. HU-08 is the story
-- that may want to lift this for REJECTED rows; it is not this one.
create policy verification_documents_storage_update_own on storage.objects
    for update to authenticated
    using (
        bucket_id = 'verification-documents'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and exists (
            select 1
            from public.verification_documents d
            where d.profile_id = (select auth.uid())
              and d.storage_path = storage.objects.name
              and d.status = 'PENDING'
        )
    )
    with check (
        bucket_id = 'verification-documents'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );

create policy verification_documents_storage_delete_own on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'verification-documents'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and exists (
            select 1
            from public.verification_documents d
            where d.profile_id = (select auth.uid())
              and d.storage_path = storage.objects.name
              and d.status = 'PENDING'
        )
    );

-- is_admin is already a definer function, so this subquery does not drag the
-- admin into the policies of profile_roles.
create policy verification_documents_storage_select_admin on storage.objects
    for select to authenticated
    using (
        bucket_id = 'verification-documents'
        and public.is_admin()
    );
