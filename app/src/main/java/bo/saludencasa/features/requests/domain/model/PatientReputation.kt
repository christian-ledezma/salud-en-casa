package bo.saludencasa.features.requests.domain.model

import java.math.BigDecimal

// How the patient has been rated by the professionals who attended them: an average
// and a count, never who rated or what was said (docs/decisions.md, 2026-11-27, HU-30).
data class PatientReputation(
    val averageRating: BigDecimal,
    val totalReviews: Int,
) {
    val isRated: Boolean get() = totalReviews > 0

    companion object {
        val None = PatientReputation(BigDecimal.ZERO, 0)
    }
}
