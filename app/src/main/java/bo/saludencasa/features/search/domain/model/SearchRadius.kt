package bo.saludencasa.features.search.domain.model

// RF-06.1 asks for a configurable radius, and the closed set of three is also
// part of a privacy control: the engine snaps the requested radius to a tenth
// of a kilometre, so an option finer than that would be snapped anyway
// (docs/decisions.md, 2026-10-09).
enum class SearchRadius(
    val km: Int,
) {
    KM_2(2),
    KM_5(5),
    KM_10(10),
    ;

    companion object {
        val DEFAULT: SearchRadius = KM_5
    }
}
