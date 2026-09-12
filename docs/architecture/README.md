# Diagramas de arquitectura

Escritos en **Mermaid dentro de archivos Markdown**, no como imágenes exportadas.
Así se versionan, se comparan entre revisiones y se renderizan en el repositorio.

| Archivo | Contenido | Producido por |
|---|---|---|
| `data-model.md` | Diagrama entidad-relación del esquema | HT-04 |
| `components.md` | Aplicación, servicios externos y sus relaciones | HT-09 |
| `packages.md` | Estructura de paquetes y la regla de dependencia | HT-09 |
| `auth-sequence.md` | Secuencia completa de autenticación con Google | HT-09 |
| `deployment.md` | Dónde se ejecuta cada componente | HT-09 |

El diagrama entidad-relación se deriva del esquema efectivamente aplicado por las
migraciones, por eso lo produce HT-04 y no HT-09.

`docs/design/` contiene material de diseño visual, no diagramas de arquitectura.

## Regla

Una imagen exportada queda desactualizada en silencio y no permite ver qué cambió
entre dos revisiones. Si un diagrama necesita una notación que Mermaid no cubre,
se documenta el motivo en `docs/decisions.md` antes de introducir otro formato.
