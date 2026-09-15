-- Revisión del pull request #4: la comprobación que marca como principal la
-- primera dirección de una persona no estaba serializada.
--
-- Dos inserciones simultáneas del mismo perfil podían ver las dos que no había
-- ninguna dirección, reclamar las dos el distintivo de principal, y perder una
-- de ellas contra idx_addresses_profile_primary. El índice protege la
-- invariante, así que nunca habría habido dos direcciones principales; lo que
-- fallaba era el guardado, de forma intermitente y sin que nada explicara por
-- qué.

set search_path = public, extensions;

create or replace function public.first_address_is_primary()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    -- El bloqueo es sobre la fila del perfil y no sobre addresses, porque no se
    -- puede bloquear una fila que todavía no existe. Dos inserciones de la
    -- primera dirección de la misma persona se ponen en fila aquí, de modo que
    -- la segunda ya ve la primera y no reclama el distintivo. Dura lo que la
    -- transacción y no alcanza a los perfiles de otras personas.
    perform 1 from public.profiles where id = new.profile_id for update;

    if not exists (
        select 1 from public.addresses where profile_id = new.profile_id
    ) then
        new.is_primary := true;
    end if;
    return new;
end;
$$;
