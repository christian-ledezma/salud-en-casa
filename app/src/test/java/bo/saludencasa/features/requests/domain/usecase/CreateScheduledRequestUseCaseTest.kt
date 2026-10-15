package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.bookableStart
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CreateScheduledRequestUseCaseTest {
    private val repository = FakeRequestRepository()
    private val useCase = CreateScheduledRequestUseCase(repository)

    private val start = bookableStart(hour = 9, minute = 30)

    // The server accepts only an instant bookable_start_times listed. Any
    // conversion on the way back, through a local time or a zone, is how a
    // listed 9:30 would arrive as some other time and be refused.
    @Test
    fun sendsTheInstantTheServerListedUntouched() =
        runTest {
            useCase("pro-1", "t1", "a1", start, description = "", budget = "")

            val draft = repository.scheduledDrafts.single()
            assertEquals(Instant.parse("2026-10-19T13:30:00Z"), draft.scheduledAt)
            assertEquals("pro-1", draft.professionalId)
            assertEquals("t1", draft.serviceTypeId)
            assertEquals("a1", draft.addressId)
        }

    @Test
    fun treatsBlankDescriptionAndBudgetAsAbsent() =
        runTest {
            useCase("pro-1", "t1", "a1", start, description = "  ", budget = " ")

            val draft = repository.scheduledDrafts.single()
            assertNull(draft.description)
            assertNull(draft.suggestedBudgetBob)
        }

    @Test
    fun rejectsAnInvalidBudgetWithoutReachingTheRepository() =
        runTest {
            val result = useCase("pro-1", "t1", "a1", start, description = "", budget = "-5")

            assertEquals(RequestResult.Failure(RequestError.InvalidBudget), result)
            assertTrue(repository.scheduledDrafts.isEmpty())
        }

    @Test
    fun rejectsADescriptionOverTheBoundWithoutReachingTheRepository() =
        runTest {
            val result = useCase("pro-1", "t1", "a1", start, description = "a".repeat(1001), budget = "")

            assertEquals(RequestResult.Failure(RequestError.InvalidDescription), result)
            assertTrue(repository.scheduledDrafts.isEmpty())
        }
}
