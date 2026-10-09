package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.FakeDocumentReviewRepository
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectDocumentsUseCaseTest {
    private val repository = FakeDocumentReviewRepository()
    private val reject = RejectDocumentsUseCase(repository)

    @Test
    fun aRejectionWithoutReasonNeverReachesTheRepository() =
        runTest {
            val result = reject("profile", setOf(DocumentType.DEGREE), "   ")

            assertEquals(ReviewActionResult.Failure(VerificationError.InvalidRejectionReason), result)
            assertTrue(repository.verdicts.isEmpty())
        }

    @Test
    fun aValidReasonReachesTheRepositoryTrimmed() =
        runTest {
            val result = reject("profile", setOf(DocumentType.DEGREE), "  Foto borrosa ")

            assertEquals(ReviewActionResult.Success, result)
            assertEquals(
                listOf(
                    FakeDocumentReviewRepository.Verdict(
                        setOf(DocumentType.DEGREE),
                        ReviewStatus.REJECTED,
                        "Foto borrosa",
                    ),
                ),
                repository.verdicts,
            )
        }

    // The reason is written once for the whole selection, so a blank one has to
    // be refused before any of the documents is touched.
    @Test
    fun aWholeSelectionTravelsInASingleVerdict() =
        runTest {
            val types = setOf(DocumentType.DEGREE, DocumentType.LICENSE, DocumentType.SELFIE)

            reject("profile", types, "Las fotos estan borrosas.")

            assertEquals(1, repository.verdicts.size)
            assertEquals(types, repository.verdicts.single().types)
        }
}
