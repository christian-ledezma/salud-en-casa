package bo.saludencasa.features.search.domain.model

sealed interface SearchOriginResult {
    data class Found(
        val origin: SearchOrigin,
    ) : SearchOriginResult

    data class Failure(
        val error: SearchError,
    ) : SearchOriginResult
}

sealed interface NearbyProfessionalsResult {
    data class Found(
        val professionals: List<NearbyProfessional>,
    ) : NearbyProfessionalsResult

    data class Failure(
        val error: SearchError,
    ) : NearbyProfessionalsResult
}

sealed interface NearbyProfessionalsPageResult {
    data class Loaded(
        val professionals: List<NearbyProfessional>,
        val endReached: Boolean,
    ) : NearbyProfessionalsPageResult

    data class Failure(
        val error: SearchError,
    ) : NearbyProfessionalsPageResult
}
