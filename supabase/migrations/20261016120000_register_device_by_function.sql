-- HU-20: a phone is registered by its Firebase installation id, and only
-- through two definer functions. docs/decisions.md, 2026-10-16, "el teléfono
-- se identifica por su instalación".

set search_path = public, extensions;

comment on column public.device_tokens.token is
    'Firebase installation id (FID) of one app install. docs/decisions.md, 2026-10-16.';

create or replace function public.register_my_device(p_token text)
returns void
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_caller uuid := (select auth.uid());
begin
    if v_caller is null then
        raise exception 'not_signed_in';
    end if;

    if p_token is null or char_length(p_token) not between 8 and 256 then
        raise exception 'invalid_device_token';
    end if;

    insert into public.device_tokens (profile_id, token, platform)
    values (v_caller, p_token, 'ANDROID')
    on conflict (token) do update
        set profile_id = excluded.profile_id,
            last_used_at = now();
end;
$$;

create or replace function public.unregister_my_device(p_token text)
returns void
language sql
volatile
security definer
set search_path = ''
as $$
    delete from public.device_tokens
    where token = p_token
      and profile_id = (select auth.uid());
$$;

revoke all on function public.register_my_device(text) from public, anon;
revoke all on function public.unregister_my_device(text) from public, anon;
grant execute on function public.register_my_device(text) to authenticated;
grant execute on function public.unregister_my_device(text) to authenticated;

-- device_tokens_select_own stays, so the table keeps a policy (INV-02).
drop policy device_tokens_insert_own on public.device_tokens;
drop policy device_tokens_update_own on public.device_tokens;
drop policy device_tokens_delete_own on public.device_tokens;

revoke all on public.device_tokens from anon;
revoke insert, update, delete on public.device_tokens from authenticated;
