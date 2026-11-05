package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.CancelAttentionResult
import bo.saludencasa.features.services.domain.vo.CancellationReason
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelAttentionUseCaseTest {
    private val repository = FakeServiceRepository()
    private val useCase = CancelAttentionUseCase(repository)

    @Test
    fun aBlankReasonSaysSoAndNeverLeavesThePhone() =
        runTest {
            assertEquals(
                CancelAttentionResult.Failure(CancelAttentionError.ReasonEmpty),
                useCase("r1", "   "),
            )
            assertTrue(repository.cancels.isEmpty())
        }

    @Test
    fun aReasonTooLongSaysSoAndNeverLeavesThePhone() =
        runTest {
            assertEquals(
                CancelAttentionResult.Failure(CancelAttentionError.ReasonTooLong),
                useCase("r1", "x".repeat(CancellationReason.MAX_LENGTH + 1)),
            )
            assertTrue(repository.cancels.isEmpty())
        }

    @Test
    fun whatTravelsIsTheReasonWithoutTheSpacesAround() =
        runTest {
            assertEquals(CancelAttentionResult.Cancelled, useCase("r1", "  No puedo llegar \n"))
            assertEquals(listOf("No puedo llegar"), repository.cancels)
        }
}
