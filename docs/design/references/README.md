# Referencias visuales

Material de referencia para la dirección visual del producto. **No son las
pantallas de Salud en Casa.**

## Qué son y qué no son

Estas imágenes provienen de un kit de interfaz de terceros para aplicaciones de
salud. Aportan tres cosas:

- **Color.** Paleta base y forma de aplicarla.
- **Disposición.** Uso de tarjetas, jerarquía, encabezado con degradado, barra de
  navegación flotante.
- **Estilo de componentes.** Chips, selectores de fecha, controles segmentados,
  distintivos de calificación.

No aportan los flujos. Salud en Casa tiene pantallas que la referencia no
contempla —búsqueda por cercanía sobre mapa, negociación de tarifas con hilo de
ofertas, verificación documental, confirmación mutua de pagos— y no tiene algunas
que la referencia muestra, como videollamada o pasarela de pagos con tarjeta.

**Las pantallas se diseñan a partir de los requisitos, no copiando la referencia.**

## Restricciones de uso

- **Las fotografías de personas no se usan en la aplicación.** Son material del
  kit. En el producto, la fotografía de perfil la carga cada usuario o proviene de
  su cuenta de Google; cuando no existe, se muestra un marcador con las iniciales.
- **Los iconos de especialidad médica del kit no se reutilizan.** Se sustituyen
  por Material Symbols o por ilustraciones propias.
- Si alguna parte del kit se reutilizara literalmente, verificar su licencia antes
  y dejar la atribución correspondiente en el informe de tesis.

## Archivos

| Archivo | Contenido |
|---|---|
| `01-home-screen.png` | Pantalla principal. Encabezado con degradado, tarjeta de cita próxima, chips de especialidad, tarjeta de profesional destacado, navegación flotante |
| `02-palette-typography.png` | Paleta base y tipografía Inter |
| `03-screen-flow-a.png` | Conjunto de pantallas: bienvenida, principal, listado, detalle, agenda, formulario, pago |
| `04-screen-flow-b.png` | Segundo conjunto, con variantes de bienvenida y detalle de agenda |

## Dónde está la especificación

La traducción de estas referencias a valores concretos —paleta con ambos esquemas,
escala tipográfica, radios, espaciado y componentes— está en
[`docs/design-system.md`](../../design-system.md).

Ese documento incluye además la **verificación de contraste** de la paleta, que
detectó dos ajustes necesarios respecto de los valores de la referencia:

- El ámbar `#FFA600` no alcanza contraste suficiente como color de texto sobre
  fondo claro. Se usa solo como relleno.
- El verde `#1A8F33` no alcanza contraste suficiente para texto de tamaño normal.
  Se oscurece a `#15782B` cuando lleva texto encima.

## Por qué viven aquí y no en los recursos de la aplicación

Son documentación, no material del producto. Colocarlas en los recursos de la
aplicación aumentaría el tamaño del paquete sin que ninguna pantalla las use.
