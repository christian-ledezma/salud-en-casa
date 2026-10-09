# design-system.md — Salud en Casa

Especificación visual derivada de las referencias de `docs/design/references/`.

Las referencias aportan dirección de color, disposición y estilo de componentes.
**No son las pantallas del producto**: los flujos de Salud en Casa difieren
(búsqueda por cercanía, negociación de tarifas, verificación documental,
confirmación mutua de pagos) y las pantallas se diseñan a partir de los requisitos,
no copiando la referencia.

> **Nota sobre las imágenes de referencia.** Provienen de un kit de interfaz de
> terceros. Las fotografías de personas que contienen son material de ese kit y
> **no deben usarse en la aplicación**. Sirven únicamente para orientar la
> dirección visual. Si alguna parte del kit se reutilizara literalmente, verificar
> antes su licencia y dejar la atribución correspondiente en el informe.

---

## 1. Paleta

### Colores base de la referencia

| Color | Valor | Rol previsto |
|---|---|---|
| Teal profundo | `#1A6F8F` | Color primario, botones, encabezados |
| Blanco | `#FFFFFF` | Superficies y tarjetas |
| Ámbar | `#FFA600` | Calificaciones y destacados |
| Verde | `#1A8F33` | Estados positivos |
| Negro casi puro | `#0D0D0D` | Texto principal |

### Verificación de contraste y correcciones

La paleta se verificó contra los criterios de accesibilidad. Dos colores requieren
ajuste antes de usarse como se ve en la referencia. Esto importa especialmente
aquí: parte de los usuarios serán adultos mayores.

| Combinación | Contraste | Resultado |
|---|---|---|
| Blanco sobre `#1A6F8F` | 5,73:1 | Cumple. El botón primario con texto blanco es correcto |
| `#1A6F8F` sobre blanco | 5,73:1 | Cumple para texto y para iconos |
| `#0D0D0D` sobre blanco | 19,6:1 | Cumple con holgura |
| **`#FFA600` sobre blanco** | **1,96:1** | **No cumple.** El ámbar nunca lleva texto sobre blanco |
| `#0D0D0D` sobre `#FFA600` | 10,7:1 | Cumple. El ámbar es relleno con texto oscuro encima |
| **Blanco sobre `#1A8F33`** | **4,23:1** | **No cumple** para texto de tamaño normal. Cumple solo para texto grande |

**Correcciones adoptadas**

- **El ámbar `#FFA600` no se usa nunca como color de texto ni de icono pequeño
  sobre fondo claro.** Su función es de relleno: la estrella de calificación se
  dibuja como glifo relleno de tamaño suficiente, y el número de la calificación
  va en tinta oscura, no en ámbar.
- **El verde de estado se oscurece a `#15782B`** cuando lleva texto encima
  (contraste 5,59:1 con blanco). El `#1A8F33` original se conserva únicamente para
  superficies grandes sin texto, como una franja de estado.

### Esquema claro

| Rol Material 3 | Valor | Uso |
|---|---|---|
| `primary` | `#1A6F8F` | Botones principales, encabezado con degradado, elementos activos |
| `onPrimary` | `#FFFFFF` | Texto e iconos sobre el primario |
| `primaryContainer` | `#CFE8F2` | Chips seleccionados, fondos de énfasis suave |
| `onPrimaryContainer` | `#08303F` | Texto sobre el contenedor primario |
| `secondary` | `#4A7C8F` | Elementos de apoyo, iconos secundarios |
| `onSecondary` | `#FFFFFF` | Texto sobre el secundario |
| `tertiary` | `#B87400` | Ámbar ajustado, para texto que deba ser ámbar |
| `tertiaryContainer` | `#FFA600` | Relleno ámbar de la referencia |
| `onTertiaryContainer` | `#3D2800` | Texto sobre relleno ámbar |
| `error` | `#B3261E` | Errores de validación |
| `onError` | `#FFFFFF` | Texto sobre error |
| `errorContainer` | `#F9DEDC` | Fondo de mensaje de error |
| `background` | `#F1F4F6` | Fondo general de la aplicación |
| `onBackground` | `#0D0D0D` | Texto sobre el fondo |
| `surface` | `#FFFFFF` | Tarjetas y hojas |
| `onSurface` | `#0D0D0D` | Texto principal |
| `onSurfaceVariant` | `#4C5B62` | Texto secundario y etiquetas |
| `outline` | `#C2CCD1` | Bordes de campos y separadores |
| `outlineVariant` | `#DEE5E8` | Separadores tenues |

### Esquema oscuro

No se obtiene invirtiendo el claro. El teal se aclara para conservar contraste
sobre fondo oscuro.

| Rol | Valor |
|---|---|
| `primary` | `#7FC5DE` |
| `onPrimary` | `#00344A` |
| `primaryContainer` | `#0F5570` |
| `onPrimaryContainer` | `#CFE8F2` |
| `tertiaryContainer` | `#E0A83C` |
| `onTertiaryContainer` | `#2B1C00` |
| `background` | `#0E1518` |
| `onBackground` | `#E6EDF0` |
| `surface` | `#162025` |
| `onSurface` | `#E6EDF0` |
| `onSurfaceVariant` | `#A9BAC1` |
| `outline` | `#3B4A51` |

### Colores semánticos de estado

Material 3 no define un rol de éxito. Se agregan como extensión del tema, no como
valores sueltos en las pantallas.

| Estado | Claro | Oscuro | Uso |
|---|---|---|---|
| Verificado, completado, pago confirmado | `#15782B` | `#6FD588` | Distintivo de verificación, estado de servicio completado |
| En curso, pendiente | `#B87400` | `#E0A83C` | Solicitud en negociación, pago pendiente |
| Cancelado, rechazado, disputa | `#B3261E` | `#F2B8B5` | Estados negativos |
| Disponible ahora | `#15782B` | `#6FD588` | Indicador de disponibilidad del profesional |

### Degradado del encabezado

La referencia usa un degradado del teal hacia el fondo claro en la parte superior
de la pantalla principal. Se reproduce como degradado vertical de `primary` a
`background`, en una altura no mayor a un tercio de la pantalla. **El texto sobre
el degradado va siempre en la zona superior**, donde el contraste con el teal está
verificado; nunca en la zona de transición.

---

## 2. Tipografía

La referencia usa **Inter**, que se conserva. Está disponible como fuente
descargable, de modo que no aumenta el tamaño del paquete.

| Estilo Material 3 | Tamaño | Peso | Uso |
|---|---|---|---|
| `displaySmall` | 32 sp | 700 | Título de la pantalla de bienvenida |
| `headlineMedium` | 26 sp | 700 | Nombre del profesional en su ficha |
| `headlineSmall` | 22 sp | 600 | Encabezado de pantalla |
| `titleLarge` | 20 sp | 600 | Nombre en tarjetas |
| `titleMedium` | 17 sp | 600 | Encabezado de sección |
| `bodyLarge` | 16 sp | 400 | Texto principal |
| `bodyMedium` | 15 sp | 400 | Texto secundario |
| `labelLarge` | 15 sp | 600 | Texto de botón |
| `labelMedium` | 13 sp | 500 | Chips y etiquetas |
| `labelSmall` | 12 sp | 500 | Metadatos, distancias, fechas |

**Tamaño mínimo: 12 sp.** Nada por debajo. La referencia usa textos más pequeños
en algunas etiquetas; no se replican.

Toda medida en `sp`, nunca en `dp`, para que la interfaz responda al ajuste de
tamaño de fuente del sistema. Verificar cada pantalla al 130 % y al 200 %: el
texto debe fluir, nunca recortarse.

---

## 3. Forma y elevación

| Elemento | Radio | Elevación |
|---|---|---|
| Tarjeta de contenido | 20 dp | 1 dp |
| Tarjeta destacada, como la del profesional popular | 24 dp | 2 dp |
| Botón principal y secundario | 16 dp | 0 dp |
| Chip y píldora | Completo | 0 dp |
| Campo de formulario | 12 dp | 0 dp |
| Hoja inferior | 28 dp arriba | 3 dp |
| Barra de navegación flotante | Completo | 3 dp |
| Avatar | Circular | 0 dp |

El borde, el relleno, el radio y la sombra indican «objeto separado». Se gastan
por función. **No toda superficie es una tarjeta**: agrupar con espaciado y
jerarquía tipográfica antes que envolver todo en un contenedor con sombra.

---

## 4. Espaciado

Escala de 4 dp. Valores permitidos: 4, 8, 12, 16, 20, 24, 32, 40, 48.

| Uso | Valor |
|---|---|
| Margen lateral de pantalla | 20 dp |
| Separación entre secciones | 24 dp |
| Relleno interno de tarjeta | 16 dp |
| Separación entre elementos de lista | 12 dp |
| Separación entre etiqueta y campo | 8 dp |
| Altura mínima de elemento tocable | 48 dp |
| Altura del mapa de dirección | 280 dp |

El espaciado entre elementos hermanos se resuelve con `Arrangement.spacedBy`,
nunca con márgenes individuales que se dupliquen o se colapsen.

---

## 5. Componentes derivados de la referencia

### Barra de navegación flotante

Píldora flotante sobre el contenido, con separación de 16 dp respecto del borde
inferior. El destino activo se marca con un círculo relleno en `surface` sobre el
fondo primario, con el icono en `primary`. Los inactivos van en `onPrimary` con
opacidad reducida.

Adaptada al producto: **tres destinos**, no cuatro. Inicio, Solicitudes, Perfil.
La referencia incluye un destino de llamadas que este producto no tiene.

### Chip de especialidad

Fila desplazable horizontal. El chip inactivo es un círculo con el icono; el
activo se expande a píldora y muestra la etiqueta. La transición se anima.
Elemento tocable de 48 dp incluso cuando el círculo visible mide menos.

### Tarjeta de profesional

Fotografía a sangre por el borde derecho, información a la izquierda. Contiene, en
este orden: nombre, especialidad, calificación con estrella, distancia y tarifa
base. El botón de acción va abajo a la derecha.

Adaptación al producto: **la distancia es información obligatoria**, porque la
cercanía es el criterio principal de selección. En la referencia no aparece.

### Distintivo de calificación

Píldora en `tertiaryContainer` con la estrella rellena y el número en
`onTertiaryContainer`. **El número nunca va en ámbar sobre blanco.**

### Selector de fecha

Fila horizontal de días. El día seleccionado se marca con un círculo relleno en
`primary` y el número en `onPrimary`. Los días no disponibles aparecen atenuados y
no responden al toque.

### Control segmentado

Dos o tres opciones excluyentes en una píldora. La activa se rellena en `primary`.
Se usa para modalidad de atención y para filtros de historial.

### Grupo de opciones exclusivas

Lista vertical de opciones con un círculo de selección a la izquierda y la
etiqueta al lado. La fila entera es el elemento tocable, no el círculo, de modo
que mide al menos 48 dp y sigue siendo alcanzable cuando el tamaño de fuente del
sistema crece.

**No figura en la referencia.** Se agrega porque el tipo de profesional tiene
cuatro opciones excluyentes y el control segmentado admite dos o tres: con cuatro
etiquetas como «Estudiante del área de salud» el segmento recorta el texto al
ancho de un teléfono. El control segmentado sigue siendo la forma de dos o tres
opciones cortas.

### Mapa de dirección

Recuadro de ancho completo y 280 dp de alto, con el radio de tarjeta del tema, un
marcador arrastrable en el centro del punto elegido y los controles de acercamiento
del propio mapa. Debajo va siempre una línea que dice qué hacer con él: colocar el
marcador si todavía no hay punto, arrastrarlo si ya lo hay.

**No figura en la referencia.** Los 280 dp salen de una restricción concreta: el
mapa tiene que dejar ver una manzana completa y, al mismo tiempo, dejar la
dirección escrita en pantalla junto a él, para que la persona compare lo que marcó
con lo que dice el texto. Más alto obliga a desplazar para leer el texto; más bajo
no distingue una calle de la siguiente.

El mapa se instancia **sin identificador de estilo en la nube**, porque eso
reclasifica cada carga a una categoría facturable, y adopta el esquema de color del
sistema para que el esquema oscuro no quede con un mapa claro encima.

### Visor de documento

Muestra la imagen de un documento de verificación dentro de una caja de proporción
4:3, con `ContentScale.Fit` para que no se recorte ningún dato del documento, sobre
`surfaceVariant`. Tiene estado de carga, de error con reintento y de documento sin
cargar. La proporción vive como constante de la pantalla porque no es una medida de
la escala de 4 dp. La imagen nunca se guarda en disco (`docs/decisions.md`,
2026-10-06).

### Campo de formulario

Etiqueta encima del campo, en `labelMedium` y `onSurfaceVariant`. Campo delineado
con radio de 12 dp. El error se muestra debajo, en `error`, y explica qué corregir.

### Botón principal

Ancho completo, alto de 56 dp, radio de 16 dp, relleno en `primary` con texto en
`onPrimary`. Fijo al pie de la pantalla en los flujos de varios pasos. Admite un
icono inicial opcional (`leadingIcon`), como el logotipo de Google en el botón de
ingreso: sin ese parámetro se comporta exactamente igual que antes.

El logotipo de Google, cuando aparece como `leadingIcon`, va sobre un círculo
blanco fijo de 24 dp, nunca sobre `MaterialTheme.colorScheme.surface`: la marca
multicolor de Google está diseñada para fondo claro sin importar el tema de
quien la aloja, y sin ese círculo se pierde contra el relleno `primary` del
botón, tanto en claro como en oscuro.

### Cabecera de bienvenida con degradado

Introducida en la pantalla de bienvenida (2026-09-15) y pensada para
reutilizarse en otras pantallas de entrada al producto. Tres bloques en
secuencia vertical dentro de la cabecera con degradado de la sección 1:

1. Encabezado: icono de marca en una caja de 36 dp sobre `onPrimary` al 20 % de
   opacidad, y el nombre de la aplicación en `titleLarge`.
2. Una o más insignias flotantes (`ExtraShapes.featuredCard`, 24 dp de radio,
   fondo `surface`) con un icono o punto a la izquierda y dos líneas de texto a
   la derecha: la cifra en `labelLarge` sobre `onSurface`, la aclaración en
   `labelSmall` sobre `onSurfaceVariant` o sobre el color de estado que
   corresponda.
3. Un círculo decorativo centrado, con un icono de Material Symbols sobre
   `onPrimary` al 15 % de opacidad —**nunca una fotografía**, por la razón de la
   sección 7— que sugiere el tema de la pantalla sin representar a nadie en
   particular. Puede llevar una animación de pulso suave (`PulsingHeartIcon`,
   `ui/components/`) en vez de quedar estático, para reforzar el tema de salud
   sin caer en el parpadeo genérico de una carga.

**Nunca se fija la altura de la cabecera con un valor en `dp` calculado a partir
del alto de pantalla.** La primera versión de esta cabecera lo hacía y, con el
tamaño de fuente del sistema al 200 % o en una pantalla angosta, el texto de la
insignia crecía más de lo previsto y terminaba encajado contra el círculo
decorativo. La cabecera deja que `Column` mida su propio contenido con
espaciado (`Arrangement.spacedBy`), nunca con posiciones absolutas
superpuestas calculadas para el tamaño de reposo: así crece con el contenido en
vez de recortarlo o de solaparlo. Verificado en el emulador a 200 % de fuente y
en un ancho de 360 dp.

Una insignia puede llevar un punto de disponibilidad parpadeante en vez de un
icono fijo: es el componente `AvailabilityDot` (`ui/components/`), que se
apaga (queda fijo en vez de parpadear) cuando el sistema tiene activada la
opción de accesibilidad «Quitar animaciones». Pensado para la insignia de
mercadeo de esta pantalla y también para el indicador real de «disponible
ahora» de un profesional, el día que esa historia lo necesite.

---

## 6. Iconografía

Material Symbols, estilo redondeado, para coincidir con el trazo suave de la
referencia. Grosor 400 en tamaño 24 dp.

Los iconos de especialidad médica de la referencia son ilustraciones propias del
kit y **no se reutilizan**. Se sustituyen por Material Symbols equivalentes o por
ilustraciones propias.

Todo icono con significado lleva descripción de contenido desde recursos de
cadenas. Los decorativos la llevan nula de forma explícita.

---

## 7. Fotografías de personas

Las fotografías de la referencia son material del kit de terceros y no se usan.

En el producto, la fotografía de perfil es la que carga cada usuario, o la que
entrega su cuenta de Google. Cuando no existe, se muestra un marcador de posición
con las iniciales sobre `primaryContainer`, nunca una fotografía genérica.

---

## 8. Verificación obligatoria antes de cerrar una pantalla

- [ ] Ningún color escrito directamente. Todos desde el tema.
- [ ] Ningún texto escrito en el código. Todos desde recursos de cadenas.
- [ ] Los esquemas claro y oscuro se ven correctos.
- [ ] El contraste de texto cumple los criterios de accesibilidad.
- [ ] El ámbar no lleva texto sobre fondo claro.
- [ ] Todo elemento tocable mide al menos 48 dp.
- [ ] Todo icono con significado tiene descripción de contenido.
- [ ] La pantalla se ve correcta con el tamaño de fuente del sistema al 200 %.
- [ ] Los cuatro estados están resueltos: cargando, vacío, con contenido y error.
- [ ] Las previsualizaciones cubren esos cuatro estados en ambos esquemas.
