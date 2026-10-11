package bo.saludencasa.features.search.domain.model

data class SearchCriteria(
    val radius: SearchRadius = SearchRadius.DEFAULT,
    val serviceTypeId: String? = null,
    val availableNowOnly: Boolean = false,
) {
    // What an empty result can offer to undo. The radius is not part of it:
    // widening is the other way out, and it is offered on its own.
    val isNarrowed: Boolean
        get() = serviceTypeId != null || availableNowOnly
}
