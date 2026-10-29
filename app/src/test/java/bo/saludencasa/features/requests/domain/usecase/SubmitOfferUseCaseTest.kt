@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.FakeOfferRepository
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SubmitOfferUseCaseTest {
    private val repository = FakeOfferRepository()
    private val useCase = SubmitOfferUseCase(repository)

    // HT-24: the message is optional. A blank one leaves as no message at all,
    // not as an empty string the patient would see as a blank bubble.
    @Test
    fun anOfferWithoutAMessageIsSent() =
        runTest {
            val result = useCase(requestId = "r1", amountRaw = "120", message = "   ")

            assertEquals(OfferSubmitResult.Success, result)
            assertNull(repository.submitted.single().message)
        }

    // The amount is a value object: zero, negatives and junk never leave.
    @Test
    fun rejectsAnAmountThatIsNotPositive() =
        runTest {
            val result = useCase(requestId = "r1", amountRaw = "0", message = "")

            assertEquals(OfferSubmitResult.Failure(OfferError.InvalidAmount), result)
            assertTrue(repository.submitted.isEmpty())
        }

    @Test
    fun rejectsAnAmountThatDoesNotParse() =
        runTest {
            val result = useCase(requestId = "r1", amountRaw = "ciento veinte", message = "")

            assertEquals(OfferSubmitResult.Failure(OfferError.InvalidAmount), result)
            assertTrue(repository.submitted.isEmpty())
        }

    // A valid offer reaches the repository with the amount as a value object and
    // the message trimmed; the comma separator is normalised to a dot.
    @Test
    fun passesAValidOfferToTheRepository() =
        runTest {
            val result = useCase(requestId = "r1", amountRaw = "120,50", message = "  Llevo el material  ")

            assertEquals(OfferSubmitResult.Success, result)
            val submission = repository.submitted.single()
            assertEquals("r1", submission.requestId)
            assertEquals(BigDecimal("120.50"), submission.amount.value)
            assertEquals("Llevo el material", submission.message)
        }
}
