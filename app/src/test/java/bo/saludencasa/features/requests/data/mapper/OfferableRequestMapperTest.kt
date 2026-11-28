package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.OfferableRequestDto
import bo.saludencasa.features.requests.domain.model.PatientReputation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class OfferableRequestMapperTest {
    private fun dto(
        patientRating: BigDecimal? = null,
        patientTotalReviews: Int = 0,
    ) = OfferableRequestDto(
        id = "r1",
        serviceTypeName = "Vacunación",
        city = "Cochabamba",
        status = "PUBLISHED",
        modality = "IMMEDIATE",
        patientRating = patientRating,
        patientTotalReviews = patientTotalReviews,
    )

    // HU-30: the detail the professional reads before offering says it too.
    @Test
    fun aRatedPatientReachesTheDetailWithTheirAverageAndCount() {
        val reputation =
            dto(BigDecimal("3.00"), 2).toOfferableRequest()!!.patientReputation

        assertEquals(PatientReputation(BigDecimal("3.00"), 2), reputation)
    }

    @Test
    fun aPatientNeverRatedHasNoReputationInTheDetail() {
        assertEquals(PatientReputation.None, dto().toOfferableRequest()!!.patientReputation)
    }

    @Test
    fun aCountWithoutAnAverageMakesTheDetailUnreadable() {
        assertNull(dto(patientRating = null, patientTotalReviews = 1).toOfferableRequest())
    }
}
