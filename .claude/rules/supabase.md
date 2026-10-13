# Supabase, migraciones y seguridad

Se carga al trabajar con migraciones SQL o con fuentes de datos.

## Regla que gobierna todo

**Cada tabla se crea junto con sus políticas de seguridad, en la misma migración.**
Ninguna tabla llega a existir sin política.

Postergarlo lleva a que el desarrollo se acostumbre a que todo funcione sin
restricción; el día que se habilitan las políticas media aplicación deja de
funcionar y la reacción natural es desactivarlas «temporalmente». Ese estado
temporal es como se filtran los datos.

## Migraciones

- Se crean con `supabase migration new <nombre_en_ingles>`.
- Se aplican con `supabase db push`.
- **Nunca** se modifica el esquema desde el panel web. El proyecto perdería
  reproducibilidad, que es un atributo de calidad exigible en la defensa.
- Una migración aplicada no se edita. Un cambio posterior es una migración nueva.
- `supabase db reset` debe reconstruir la base completa sin errores. Verificarlo
  al cerrar cada sprint que haya tocado el esquema.

## Convenciones del esquema

| Elemento | Convención |
|---|---|
| Tablas | `snake_case`, plural, en inglés |
| Columnas | `snake_case`, en inglés |
| Tipos enumerados | `snake_case` el tipo, `SCREAMING_SNAKE_CASE` los valores |
| Claves primarias | `id uuid primary key default gen_random_uuid()` |
| Claves foráneas | `<tabla_singular>_id` |
| Marcas de tiempo | `timestamptz`, nunca `timestamp` |
| Dinero | `numeric(10,2)`, con sufijo `_bob` |
| Geografía | `geography(Point, 4326)` |
| Índices | `idx_<tabla>_<columnas>` |

## Seguridad a nivel de fila

Toda tabla comienza así:

```sql
alter table <tabla> enable row level security;
```

Y declara al menos una política. Criterios generales:

- El usuario lee y escribe únicamente sus propias filas, identificadas por
  `auth.uid()`.
- El profesional accede a solicitudes publicadas dentro de su radio, y a aquellas
  en que participa.
- El administrador accede a documentos de verificación, pagos y estados. **Nunca
  a `messages`**: la conversación contiene el detalle de la atención.
- La denegación es el comportamiento por omisión. Si una consulta no devuelve
  datos que deberían existir, lo primero a revisar es la política.

Al cerrar un sprint que agregue tablas, verificar la cobertura consultando el
catálogo del sistema: toda tabla del esquema público debe tener la seguridad
habilitada y al menos una política.

## Lo que se publica no es solo lo que se devuelve

Redondear un valor antes de enviarlo acota **ese** canal y ningún otro. Si la
función tiene un parámetro continuo que el llamante controla —una coordenada, un
monto, una fecha—, la **respuesta binaria** ya es un canal: sondeando el
parámetro se encuentra el borde donde la respuesta cambia, y ese borde habla del
dato exacto que la condición usa por dentro.

De modo que una función que publica una versión reducida de un dato tiene que
**decidir con esa misma versión reducida**, no con el original. Si eso cuesta un
índice, el camino es un prefiltro con holgura sobre la columna intacta y la
condición exacta sobre el valor publicado después.

En HU-11 se redondeó el marcador a la manzana, la distancia a la centena de metros
y el radio pedido a la décima de kilómetro, y la base seguía siendo recuperable al
metro con unas veinte llamadas, porque el origen era un parámetro libre. Ver
`docs/decisions.md`, 2026-10-11.

## Vistas que corren con los privilegios de su dueño

Una vista con `security_invoker = false` existe para mostrar filas que las
políticas de la tabla no dejarían ver, y su `where` **es** la regla. Eso vale para
leer. Para escribir hay una trampa:

**Si la vista es auto-actualizable, se puede escribir por ella, y esa escritura no
enfrenta ninguna política.** Es auto-actualizable cuando sale de una sola tabla,
sin `join`, sin agregados, sin `distinct` y sin `limit`; una subconsulta en el
`where` no lo impide. Supabase concede todo a `authenticated` sobre un objeto
nuevo, de modo que no quitárselo es concedérselo.

Por eso toda vista de definidor se crea así, y el `revoke` nombra a
`authenticated` y no solo a `anon`:

```sql
revoke all on public.<vista> from anon, authenticated;
grant select on public.<vista> to authenticated;
```

Y **su prueba de política incluye un intento de escritura**, no solo lecturas. En
HU-12 cuatro pruebas de lectura pasaron sobre una vista por la que un profesional
podía reescribir las calificaciones que había recibido. Ver `docs/decisions.md`,
2026-10-11.

## Consultas geoespaciales

- La búsqueda por cercanía se resuelve **siempre** con PostGIS dentro de la base
  de datos. Nunca con un servicio externo de búsqueda por proximidad, y nunca
  filtrando distancias en el cliente.
- Toda columna geográfica lleva índice GIST. Sin él la consulta degrada a
  recorrido secuencial y el sistema deja de escalar.
- Verificar el plan de ejecución al escribir una consulta espacial nueva: debe
  utilizar el índice.
- **Cuidado con el orden de las coordenadas.** La construcción del punto recibe
  longitud primero y latitud después. Invertirlo produce resultados vacíos o
  absurdos sin generar error.

## Funciones almacenadas

- Se declaran `stable` si solo leen, `volatile` si escriben.
- `security invoker` por omisión, para que respeten las políticas del usuario.
- `security definer` únicamente cuando la operación deba omitir las políticas de
  forma deliberada, y en ese caso siempre con `set search_path = ''`.
- Se invocan desde la capa de datos mediante el mecanismo de llamada a función,
  con los parámetros nombrados.

## Funciones de servidor

Se reservan para tres casos y ningún otro:

1. **Transacciones atómicas** que el cliente no puede garantizar, como la
   aceptación de una oferta, que marca la oferta, actualiza la solicitud, crea el
   servicio y crea el pago en una sola operación.
2. **Operaciones privilegiadas**, como notificar a profesionales cercanos, que
   requiere leer perfiles y dispositivos de terceros.
3. **Recepción de notificaciones externas**, cuando exista pasarela de pagos.

Si algo puede resolverse con una consulta del cliente respetando las políticas, se
resuelve así. No se crea una función de servidor por comodidad.

## Claves

- El cliente móvil usa **únicamente la clave anónima**. No es un secreto: no
  otorga acceso por sí sola, porque quien concede es la política.
- La **clave de servicio omite todas las políticas** y jamás sale del entorno de
  servidor. Nunca en el cliente, nunca en el repositorio, nunca en un registro.
- Las claves se leen de `local.properties` en desarrollo y de los secretos del
  repositorio en integración continua.

## Tiempo real

- La suscripción se expone como `Flow` desde el repositorio de la característica.
- Se filtra por la fila o el conjunto relevante, nunca por la tabla completa.
- Las políticas de seguridad aplican también a esta vía.
- El modelo de vista la consume con `stateIn` y un alcance acotado, para que la
  suscripción se cierre cuando la pantalla deja de observarse.

## Almacenamiento de archivos

- Los documentos de verificación viven en un contenedor **privado**.
- La política permite leer solo al propietario del archivo y al administrador.
- Las imágenes se comprimen en el cliente antes de subirse.
- El acceso se resuelve con URL firmada de vigencia acotada, nunca con URL pública.

## Costos

- Nunca habilitar ni consumir el servicio de búsqueda por proximidad del
  proveedor de mapas.
- La geocodificación se almacena en la base al guardar una dirección. Una misma
  dirección nunca se geocodifica dos veces.
- El mapa no se instancia con identificador de estilo en la nube: eso reclasifica
  cada carga a una categoría facturable.
