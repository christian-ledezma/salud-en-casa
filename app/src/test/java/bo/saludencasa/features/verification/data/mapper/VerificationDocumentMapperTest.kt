package bo.saludencasa.features.verification.data.mapper

import bo.saludencasa.features.verification.data.model.VerificationDocumentDto
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class VerificationDocumentMapperTest {
    @Test
    fun rejectedDocumentExposesReasonToItsOwner() {
        val document =
            VerificationDocumentDto(
                documentType = "SELFIE",
                status = "REJECTED",
                storagePath = "profile/SELFIE.jpg",
                rejectionReason = "El rostro no se ve con claridad.",
            ).toDocument()

        assertEquals(ReviewStatus.REJECTED, document?.status)
        assertEquals("El rostro no se ve con claridad.", document?.rejectionReason)
    }
}
