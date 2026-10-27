@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.FakeOfferRepository
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SubmitOfferUseCaseTest {
    private val repository = FakeOfferRepository()
    private val useCase = SubmitOfferUseCase(repository)

    // RF-08.1: a message is part of the offer, so a blank one never reaches the
    // network and never records a submission.
    @Test
    fun rejectsABlankMessageBeforeHittingTheNetwork() =
        runTest {
            val result = useCase(requestId = "r1", amountRaw = "120", message = "   ")

            assertEquals(OfferSubmitResult.Failure(OfferError.MessageRequired), result)
            assertTrue(repository.submitted.isEmpty())
        }

    // The amount is a value object: zero, negatives and junk never leave.
    @Test
    fun rejectsAnAmountThatIsNotPositive() =
        runTest {
            val result = useCase(requestId = "r1", amountRaw = "0", message = "Puedo ir ahora")

            assertEquals(OfferSubmitResult.Failure(OfferError.InvalidAmount), result)
            assertTrue(repository.submitted.isEmpty())
        }

    @Test
    fun rejectsAnAmountThatDoesNotParse() =
        runTest {
            val result = useCase(requestId = "r1", amountRaw = "ciento veinte", message = "Puedo ir ahora")

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

    // The policy's refusal comes back as it is, not swallowed.
    @Test
    fun surfacesTheRepositoryRefusal() =
        runTest {
            repository.submitResult = OfferSubmitResult.Failure(OfferError.NotAllowed)

            val result = useCase(requestId = "r1", amountRaw = "120", message = "Puedo ir ahora")

            assertEquals(OfferSubmitResult.Failure(OfferError.NotAllowed), result)
        }
}
