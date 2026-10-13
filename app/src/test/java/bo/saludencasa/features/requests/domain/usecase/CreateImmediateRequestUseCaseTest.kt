package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CreateImmediateRequestUseCaseTest {
    private val repository = FakeRequestRepository()
    private val useCase = CreateImmediateRequestUseCase(repository)

    @Test
    fun rejectsABudgetThatIsNotAPositiveAmountWithoutReachingTheRepository() =
        runTest {
            val result = useCase(serviceTypeId = "t1", addressId = "a1", description = "", budget = "0")

            assertEquals(RequestResult.Failure(RequestError.InvalidBudget), result)
            assertTrue(repository.createdDrafts.isEmpty())
        }

    @Test
    fun rejectsADescriptionOverTheBoundWithoutReachingTheRepository() =
        runTest {
            val result =
                useCase(serviceTypeId = "t1", addressId = "a1", description = "a".repeat(1001), budget = "")

            assertEquals(RequestResult.Failure(RequestError.InvalidDescription), result)
            assertTrue(repository.createdDrafts.isEmpty())
        }

    // Both optional fields are absent when blank, not rejected and not sent as
    // an empty string the server would store.
    @Test
    fun treatsBlankDescriptionAndBudgetAsAbsent() =
        runTest {
            useCase(serviceTypeId = "t1", addressId = "a1", description = "   ", budget = "   ")

            val draft = repository.createdDrafts.single()
            assertNull(draft.description)
            assertNull(draft.suggestedBudgetBob)
        }

    @Test
    fun forwardsTheValidatedDraftWithTheParsedAmount() =
        runTest {
            useCase(serviceTypeId = "t1", addressId = "a1", description = "  Fiebre alta  ", budget = "80,50")

            val draft = repository.createdDrafts.single()
            assertEquals("t1", draft.serviceTypeId)
            assertEquals("a1", draft.addressId)
            assertEquals("Fiebre alta", draft.description)
            assertEquals(0, BigDecimal("80.50").compareTo(draft.suggestedBudgetBob))
        }
}
