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

class RejectDocumentUseCaseTest {
    private val repository = FakeDocumentReviewRepository()
    private val reject = RejectDocumentUseCase(repository)

    @Test
    fun aRejectionWithoutReasonNeverReachesTheRepository() =
        runTest {
            val result = reject("profile", DocumentType.DEGREE, "   ")

            assertEquals(ReviewActionResult.Failure(VerificationError.InvalidRejectionReason), result)
            assertTrue(repository.verdicts.isEmpty())
        }

    @Test
    fun aValidReasonReachesTheRepositoryTrimmed() =
        runTest {
            val result = reject("profile", DocumentType.DEGREE, "  Foto borrosa ")

            assertEquals(ReviewActionResult.Success, result)
            assertEquals(
                listOf(
                    FakeDocumentReviewRepository.Verdict(DocumentType.DEGREE, ReviewStatus.REJECTED, "Foto borrosa"),
                ),
                repository.verdicts,
            )
        }
}
