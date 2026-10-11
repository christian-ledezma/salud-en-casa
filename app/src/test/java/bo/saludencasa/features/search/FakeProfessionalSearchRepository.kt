package bo.saludencasa.features.search

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsResult
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.repository.IProfessionalSearchRepository
import kotlinx.coroutines.CompletableDeferred
import java.math.BigDecimal

// The fake answers out of a stored list and slices it the way the engine does,
// so a paging test exercises offsets instead of trusting the repository to
// repeat whatever it was handed.
class FakeProfessionalSearchRepository(
    var professionals: List<NearbyProfessional> = emptyList(),
) : IProfessionalSearchRepository {
    data class Request(
        val origin: Coordinate,
        val criteria: SearchCriteria,
        val offset: Int,
        val limit: Int,
    )

    var failure: SearchError? = null

    // A test that needs two requests in flight at once completes this by hand.
    var gate: CompletableDeferred<Unit>? = null

    val requests: MutableList<Request> = mutableListOf()

    // Answers per request, so a test can tell which criteria a reply came from.
    var responder: ((Request) -> List<NearbyProfessional>)? = null

    override suspend fun findNearby(
        origin: Coordinate,
        criteria: SearchCriteria,
        offset: Int,
        limit: Int,
    ): NearbyProfessionalsResult {
        requests += Request(origin, criteria, offset, limit)
        gate?.await()
        failure?.let { return NearbyProfessionalsResult.Failure(it) }

        val source = responder?.invoke(Request(origin, criteria, offset, limit)) ?: professionals
        return NearbyProfessionalsResult.Found(source.drop(offset).take(limit))
    }
}

fun nearbyProfessional(
    id: String = "f1b0c0de-0000-4000-8000-000000000000",
    fullName: String = "Ana Pérez",
    photoUrl: String? = null,
    professionalType: ProfessionalType? = ProfessionalType.NURSE,
    specialty: String? = "Enfermería geriátrica",
    rate: String = "180.00",
    averageRating: Double = 4.8,
    totalReviews: Int = 12,
    availableNow: Boolean = true,
    distanceM: Int = 1200,
    basePoint: Coordinate? = Coordinate.create(-17.368, -66.174).getOrThrow(),
): NearbyProfessional =
    NearbyProfessional(
        professionalId = id,
        fullName = fullName,
        photoUrl = photoUrl,
        professionalType = professionalType,
        specialty = specialty,
        baseRateBob = BigDecimal(rate),
        averageRating = averageRating,
        totalReviews = totalReviews,
        availableNow = availableNow,
        distanceM = distanceM,
        basePoint = basePoint,
    )
