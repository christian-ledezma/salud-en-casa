-- HU-22: the record of what was done is a marked message. docs/decisions.md,
-- 2026-11-03, HU-22.
--
-- The text stays in messages.content, which already carries its author and date
-- and cannot be edited or deleted by a client: 20260912090000 grants update on
-- read_at alone, and no delete policy exists. That grant is also what keeps this
-- column out of reach once the row exists: the counterpart may update the row to
-- mark it read, and nothing else in it. conversation_page returns the table's row
-- type, so it carries the column without being rewritten.

alter table public.messages
    add column is_work_record boolean not null default false;

-- ---------------------------------------------------------------------------
-- Only the professional of the service may mark a message, and only once the
-- attention is under way. Ordinary messages keep the rule of HU-21.
-- ---------------------------------------------------------------------------

drop policy messages_insert_patient on public.messages;

create policy messages_insert_patient on public.messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and not is_work_record
        and exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.patient_id = (select auth.uid())
              and s.status <> 'CANCELLED'
        )
    );

drop policy messages_insert_professional on public.messages;

create policy messages_insert_professional on public.messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and exists (
            select 1
            from public.services s
            where s.request_id = messages.request_id
              and s.professional_id = (select auth.uid())
              and s.status <> 'CANCELLED'
              and (not messages.is_work_record or s.status in ('IN_PROGRESS', 'COMPLETED'))
        )
    );
