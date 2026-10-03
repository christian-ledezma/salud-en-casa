# Modelo de datos

Diagrama entidad-relación del esquema, derivado de las migraciones aplicadas en
`supabase/migrations/`. Si el esquema cambia, este archivo cambia en la misma
migración.

Los identificadores están en inglés y los textos en español, según la regla de
idioma de `CLAUDE.md`.

## Diagrama

```mermaid
erDiagram
    profiles ||--o{ profile_roles : "tiene"
    profiles ||--o| patients : "comparte identidad"
    profiles ||--o| professionals : "comparte identidad"
    profiles ||--o{ verification_documents : "carga"
    profiles ||--o{ addresses : "registra"
    profiles ||--o{ device_tokens : "usa"
    profiles ||--o{ messages : "envia"
    profiles ||--o{ reviews : "escribe"

    professionals ||--o{ professional_services : "declara"
    professionals ||--o{ availability_slots : "publica"
    service_types ||--o{ professional_services : "clasifica"

    patients ||--o{ service_requests : "crea"
    service_types ||--o{ service_requests : "tipifica"
    addresses |o--o{ service_requests : "origina"
    professionals |o--o{ service_requests : "recibe agendada"

    service_requests ||--o{ request_offers : "recibe"
    request_offers |o--o{ request_offers : "contraoferta de"
    professionals ||--o{ request_offers : "emite"

    service_requests ||--o{ messages : "contiene"

    service_requests ||--|| services : "se acuerda en"
    request_offers ||--|| services : "fija el monto de"
    patients ||--o{ services : "recibe"
    professionals ||--o{ services : "presta"

    services ||--|| payments : "genera"
    services ||--o{ reviews : "habilita"

    profiles {
        uuid id PK "igual al identificador del proveedor de autenticacion"
        text full_name
        text email
        text phone "oculto hasta aceptar una oferta"
        text photo_url
        user_role active_role "nulo hasta elegir el primero; FK compuesta a profile_roles"
        boolean active
    }

    profile_roles {
        uuid profile_id PK, FK
        user_role role PK "PATIENT, PROFESSIONAL o ADMIN"
        timestamptz created_at "solo agregar: nunca se da de baja"
    }

    patients {
        uuid id PK, FK
        date birth_date
        text emergency_contact
        text medical_notes
    }

    professionals {
        uuid id PK, FK
        professional_type professional_type
        text specialty
        text biography
        numeric base_rate_bob
        integer years_of_experience
        numeric coverage_radius_km
        boolean available_now
        review_status verification_status
        integer total_services
    }

    verification_documents {
        uuid id PK
        uuid profile_id FK
        document_type document_type
        text storage_path "contenedor privado"
        review_status status
        uuid reviewed_by FK
        text rejection_reason
    }

    addresses {
        uuid id PK
        uuid profile_id FK
        text alias
        text address_text
        text reference "hasta 300 caracteres"
        text city
        geography location "Point 4326, indice GIST"
        boolean is_primary "una sola por persona, la primera automatica"
    }

    service_types {
        uuid id PK
        text name UK
        numeric reference_price_bob
        integer estimated_duration_min
        boolean active
    }

    professional_services {
        uuid id PK
        uuid professional_id FK
        uuid service_type_id FK
        numeric reference_price_bob
        boolean active
    }

    availability_slots {
        uuid id PK
        uuid professional_id FK
        smallint day_of_week "1 lunes a 7 domingo"
        time start_time
        time end_time
    }

    service_requests {
        uuid id PK
        uuid patient_id FK
        uuid professional_id FK "solo si es agendada"
        uuid service_type_id FK
        uuid address_id FK "solo trazabilidad"
        geography location "instantanea, nunca se recalcula"
        text address_text "instantanea"
        request_modality modality
        timestamptz scheduled_at
        numeric suggested_budget_bob
        request_status status
        timestamptz expires_at
    }

    request_offers {
        uuid id PK
        uuid request_id FK
        uuid professional_id FK
        uuid parent_offer_id FK "la oferta que responde"
        offer_issuer issuer
        numeric amount_bob "inmutable"
        text message
        offer_status status
    }

    messages {
        uuid id PK
        uuid request_id FK
        uuid sender_id FK
        text content "el administrador nunca lo lee"
        timestamptz read_at
    }

    services {
        uuid id PK
        uuid request_id FK, UK
        uuid offer_id FK, UK
        uuid patient_id FK
        uuid professional_id FK
        numeric final_amount_bob "congelado"
        service_status status
        timestamptz started_at
        timestamptz finished_at
        text cancellation_reason
        uuid cancelled_by FK
    }

    payments {
        uuid id PK
        uuid service_id FK, UK
        numeric total_amount_bob "igual a la suma de los dos siguientes"
        numeric platform_fee_bob
        numeric professional_amount_bob
        payment_method method
        text provider
        text external_reference
        payment_status status
        timestamptz patient_confirmed_at
        timestamptz professional_confirmed_at
        timestamptz settled_at
    }

    reviews {
        uuid id PK
        uuid service_id FK
        uuid author_id FK
        uuid recipient_id FK "nunca igual al autor"
        smallint rating "entre 1 y 5"
        text comment
        boolean visible
    }

    device_tokens {
        uuid id PK
        uuid profile_id FK
        text token UK
        device_platform platform
        timestamptz last_used_at
    }
```

## Las dieciséis tablas

| Tabla | Qué guarda | Migración |
|---|---|---|
| `profiles` | La identidad única de toda persona y el rol en el que está | `identity`, `multi_role_identity` |
| `profile_roles` | Qué roles tiene cada persona. Solo agregar | `multi_role_identity` |
| `patients` | Los datos propios del rol paciente y su reputación como paciente | `identity`, `close_dual_role_gaps` |
| `professionals` | Los datos propios del rol profesional, su verificación y su reputación como profesional | `identity`, `close_dual_role_gaps` |
| `verification_documents` | Respaldos de identidad y título, con el resultado de su revisión | `identity` |
| `addresses` | Domicilios georreferenciados de cada persona, y la base desde la que un profesional cubre su zona | `identity`, `professional_base_address` |
| `service_types` | Catálogo de tipos de atención de la plataforma | `catalog` |
| `professional_services` | Qué tipos presta cada profesional y a qué precio | `catalog` |
| `availability_slots` | Franjas horarias semanales declaradas | `catalog` |
| `service_requests` | Peticiones de atención, inmediatas o agendadas | `requests` |
| `request_offers` | Hilo de ofertas y contraofertas sobre una solicitud | `requests` |
| `messages` | Conversación de la solicitud y detalle de la atención | `requests` |
| `services` | La atención acordada, con su monto congelado | `delivery` |
| `payments` | Registro económico y confirmación de ambas partes | `delivery` |
| `reviews` | Calificaciones cruzadas sobre una atención completada | `delivery` |
| `device_tokens` | Destinos de notificación por persona | `support` |

## Dónde se hace cumplir cada invariante

La columna de la derecha es lo que hay que revisar si alguna vez se sospecha que
un invariante dejó de cumplirse.

| Invariante | Mecanismo |
|---|---|
| INV-01 · `profiles` es la identidad única | Clave primaria de `patients` y `professionals` referida a `profiles.id`, que a su vez referencia `auth.users`. Una persona puede tener fila en ambas, porque puede tener ambos roles |
| INV-02 · Toda tabla con RLS y política | `enable row level security` y al menos una política en la migración que crea cada tabla |
| INV-03 · `service_requests.location` es instantánea | Disparador `service_requests_guard_location` |
| INV-04 · `services.final_amount_bob` congelado | Disparador `services_guard_frozen` |
| INV-05 · No se corrige el histórico | Sin política de borrado en `services`, `payments`, `request_offers` ni `reviews`; disparadores `services_guard_transition` y `payments_guard_frozen` |
| INV-06 · Ofertas de solo agregar | Disparador `request_offers_guard_append_only` y ausencia de política de borrado |
| INV-07 · Solo profesionales aprobados y activos | Vista `professional_directory`, filtro de `search_nearby_professionals` y restricción `professionals_approved_profile_is_complete`, que impide aprobar un profesional sin tipo ni tarifa |
| INV-08 · Cercanía con PostGIS | `search_nearby_professionals` con `st_dwithin` sobre el índice GIST `idx_addresses_location` |
| INV-09 · Pago confirmado solo por ambas partes | Restricción `confirmations_match_status` y disparador `payments_guard_confirmation` |
| INV-10 · Total igual a comisión más monto del profesional | Restricción `total_equals_fee_plus_professional_amount` |
| INV-11 · Calificación única, de 1 a 5, autor distinto del destinatario | Restricciones `unique (service_id, author_id)`, `rating between 1 and 5` y `author_is_not_recipient` |
| INV-12 · El administrador no accede a `messages` | Ninguna política de `messages` invoca `is_admin()`, en esta ni en ninguna migración posterior |
| INV-13 · Nadie lee filas ajenas | Políticas por `auth.uid()` en las dieciséis tablas |
| INV-14 · La clave de servicio no sale del servidor | Fuera del esquema: `local.properties` y secretos del repositorio |
| INV-15 · El rol activo es uno de los que la persona tiene | Clave foránea compuesta `profiles_active_role_is_held`, de `(id, active_role)` a la clave primaria de `profile_roles`. Ninguna política lee el rol activo: la seguridad decide por posesión |
| INV-16 · Nadie es paciente y profesional de la misma atención | `patient_id <> auth.uid()` en `service_requests_select_inbox` y en la rama profesional de `request_offers_insert_participants`; `pro.id <> auth.uid()` en `search_nearby_professionals`; restricción `parties_are_different` en `services` |
| INV-17 · La reputación es por rol | `patients.average_rating` y `professionals.average_rating` por separado, calculadas por `recalculate_reputation()` derivando el lado desde `services`; `reviews_select_visible` solo hace pública la recibida como profesional |

## Tipos enumerados

Los doce tipos viven en la migración `extensions_and_types`. Sus valores se
escriben en `SCREAMING_SNAKE_CASE` para que correspondan uno a uno con las
constantes de Kotlin.

`user_role` · `review_status` · `professional_type` · `document_type` ·
`request_modality` · `request_status` · `offer_issuer` · `offer_status` ·
`service_status` · `payment_method` · `payment_status` · `device_platform`

## Vistas y funciones

| Objeto | Para qué |
|---|---|
| `professional_directory` | Proyección pública del profesional. La seguridad a nivel de fila no puede ocultar una sola columna, así que la ficha pública es una vista que simplemente no contiene el teléfono |
| `my_roles` | Proyección de `profiles` y `profile_roles`: el rol activo y el arreglo de roles que la persona tiene, en una sola fila. `security_invoker`, así que las políticas propias siguen decidiendo (INV-13). Existe para que el cliente lea el estado de rol en una petición y no en dos; incrustar `profile_roles` desde `profiles` sería ambiguo ahora que dos claves foráneas unen el mismo par de tablas |
| `my_addresses` | Proyección de `addresses` que devuelve el punto como `latitude` y `longitude`. PostgREST entrega una columna `geography` como su codificación hexadecimal, que el cliente tendría que decodificar para dibujar un marcador. Es `security_invoker`, al revés que `professional_directory`: muestra filas propias, así que `addresses_select_own` sigue decidiendo cuáles (INV-13) |
| `search_nearby_professionals` | Búsqueda por cercanía. Devuelve distancia y ordena de forma ascendente. Es la única vía de la búsqueda geográfica |
| `handle_new_user` | Crea el perfil al primer ingreso, sin ningún rol y con el rol activo nulo |
| `save_my_profile` | Escribe `profiles` y, según el rol activo, `patients` o `professionals`, en una sola transacción. Lee `active_role` del servidor en vez de recibirlo del cliente, de modo que un argumento que no corresponde al rol se ignora en vez de escribirse. No toca `available_now`: esa columna la escribe el interruptor de disponibilidad por su cuenta |
| `add_my_role` | Agrega un rol a `profile_roles`, crea la fila de `patients` o de `professionals`, y deja el rol nuevo como activo, en una sola transacción. `security invoker`: cada escritura ya la permite la política del propio usuario, de modo que la función aporta atomicidad y nada más. Reemplazó a `assign_my_role` en el Sprint 2.5 |
| `recalculate_reputation` | Recalcula `average_rating` y `total_reviews` en cada calificación, por separado en `patients` y en `professionals`. El lado de cada calificación se deriva del servicio que la originó, nunca de los roles que la persona tiene hoy |
| `first_address_is_primary` | Marca como principal la primera dirección de cada persona. La escribe la base y no el formulario, de modo que vale para toda fila que llegue a la tabla |
| `unmark_previous_primary_address` | Desmarca la anterior cuando otra pasa a ser principal, en vez de fallar contra el índice único |
| `unmark_previous_professional_base` | Lo mismo para la base profesional, que es la dirección desde la que se centra el radio de cobertura. No hay disparador equivalente a `first_address_is_primary`: declarar desde dónde se trabaja es una afirmación que solo el profesional puede hacer |
| `is_admin`, `shares_service_with`, `professional_covers` | Auxiliares que usan las políticas |
| `offer_continues_thread`, `was_the_professional_of` | Auxiliares `security definer` que contestan un booleano a una política. Existen porque una subconsulta dentro de una política obedece a las políticas de la tabla que consulta: la primera recursaba sobre `request_offers`, la segunda no veía `services` desde un tercero. Ver `docs/decisions.md`, 2026-10-02 |

`search_nearby_professionals`, `handle_new_user`, `recalculate_reputation`,
`is_admin` y `shares_service_with` son `security definer` porque deben leer filas
que el usuario no puede leer por sí mismo. Todas declaran `set search_path = ''`
y califican cada objeto con su esquema.

`add_my_role` y `save_my_profile` son la excepción: son `security invoker`,
porque no necesitan saltarse ninguna política. Existe por la atomicidad. Dos escrituras separadas
desde el cliente no pueden garantizarla, y si la segunda fallara la persona
quedaría con un rol sin la fila que lo sostiene, sin que nada lo reintentara
(`docs/decisions.md`, 2026-09-13).

**`professionals.professional_type` y `professionals.base_rate_bob` nacen
nulos.** RF-01.4 pide el rol antes que cualquier otra funcionalidad y RF-02.2
pone el tipo y la tarifa en el perfil profesional, que HU-04 recoge después. Un
campo en blanco en esa pantalla sigue guardándose como nulo, porque es alguien
que todavía no declaró el dato. La restricción
`professionals_approved_profile_is_complete` impide que un profesional
incompleto llegue a `APPROVED`, que es la condición que
`professional_directory` y `search_nearby_professionals` exigen para mostrarlo
(INV-07).

**`addresses.is_primary` la decide la base en la primera dirección.** La columna
nace en falso y `search_nearby_professionals` une con `addresses` filtrando por
ella, así que una persona con una sola dirección que no fuera la principal sería
invisible para la búsqueda. El disparador `addresses_first_is_primary` la marca al
insertar; elegir entre varias es RF-03.4 y lo recoge HU-06. Los dos disparadores de
la tabla se ejecutan en orden alfabético, y ese nombre coloca a este antes de
`addresses_unmark_previous_primary`, que es el orden que corresponde.

**`addresses.reference` admite hasta 300 caracteres.** `alias` y `address_text`
traen su límite desde la migración que creó la tabla; `reference` quedó abierta,
lo que dejaba al único campo de texto libre del formulario como la única columna
donde un cliente podía guardar una cantidad arbitraria de datos. Una referencia
vacía se guarda como nulo, así que la restricción empieza en un carácter.

**`professionals.coverage_radius_km` admite de 1 a 50 kilómetros.** La columna
se creó aceptando cualquier valor mayor que cero; HU-04 fija el rango que la
historia enuncia y lo cierra en el motor con
`professionals_coverage_radius_km_range`, de modo que la regla no dependa de
que la petición venga de la aplicación.

## Lo que este esquema no incluye, de forma deliberada

- **Registro clínico estructurado.** Fuera de alcance (FA-01). El detalle de la
  atención vive en `messages`. No existe una columna de notas en `services`, y no
  debe crearse: el registro clínico se incorporará como tabla propia con autoría,
  fecha y semántica de solo agregar.
- **Base de datos local en el cliente.** Ningún requisito exige operación sin
  conexión (FA-08).
- **Pasarela de pagos.** `payments` ya contempla `method`, `provider` y
  `external_reference` para incorporarla sin cambio de esquema (FA-02).
