package bo.saludencasa.features.location.domain.model

import bo.saludencasa.core.vo.Coordinate

// Donde y con cuanto detalle abre el mapa cuando todavia no hay nada que
// mostrar. Los tres valores viven juntos porque responden a la misma pregunta,
// y son numeros sin nada de plataforma detras, de modo que siguen siendo
// codigo de dominio.
object MapDefaults {
    // RF-03.6: con el permiso denegado y sin dirección registrada no hay
    // posición sobre la que abrir, y una centrada en el origen del sistema de
    // coordenadas dejaría a la persona en el Golfo de Guinea. Abre sobre el
    // estadio Félix Capriles de Cochabamba, un punto que cualquiera reconoce y
    // desde el que puede desplazarse. Las coordenadas salieron del propio
    // geocodificador, buscando el estadio por su nombre.
    val initialPosition: Coordinate = Coordinate.create(-17.379268, -66.161794).getOrThrow()

    // Escala de Google Maps: 12 abarca una ciudad entera, 15 un barrio con sus
    // avenidas, 17 distingue una puerta de la siguiente. El mapa abre alejado,
    // porque todavía no sabe dónde vive la persona y lo que necesita es
    // reconocer la zona.
    const val INITIAL_ZOOM = 15f

    // Y se acerca en cuanto hay un punto concreto que señalar, que es cuando la
    // precisión empieza a importar.
    const val POINT_ZOOM = 17f
}
