-- HU-03: saving a profile writes profiles and, for a patient, patients, in one
-- transaction.
--
-- The review of pull request 3 found the same defect assign_my_role exists to
-- prevent, one story later: the client sent two separate requests, so a
-- connection that dropped between them left the name and the phone stored and
-- the patient columns not, while the screen reported the save as failed. That
-- one is recoverable —saving again rewrites both rows— but only for somebody
-- who tries again, and nothing tells them they should.

set search_path = public, extensions;

-- Security invoker, like assign_my_role: profiles_update_own and
-- patients_update_own already allow both writes, so the function adds atomicity
-- and nothing else.
--
-- The role is read here instead of being trusted from the client. A
-- professional has no row in patients, and the patient columns are not theirs
-- to fill: deciding by the stored role means an argument sent by mistake is
-- ignored rather than written.
create or replace function public.save_my_profile(
    p_full_name text,
    p_phone text default null,
    p_birth_date date default null,
    p_emergency_contact text default null,
    p_medical_notes text default null
)
returns void
language plpgsql
volatile
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
    v_role public.user_role;
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    select role into v_role
    from public.profiles
    where id = v_caller;

    if not found then
        raise exception 'profile_not_found';
    end if;

    update public.profiles
    set full_name = p_full_name,
        phone = p_phone
    where id = v_caller;

    if v_role = 'PATIENT' then
        update public.patients
        set birth_date = p_birth_date,
            emergency_contact = p_emergency_contact,
            medical_notes = p_medical_notes
        where id = v_caller;
    end if;
end;
$$;

revoke all on function public.save_my_profile(text, text, date, text, text) from public, anon;

grant execute on function public.save_my_profile(text, text, date, text, text) to authenticated;
