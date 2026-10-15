@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.availability.domain.usecase

import bo.saludencasa.features.availability.FakeAvailabilityRepository
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import bo.saludencasa.features.availability.domain.model.SlotWriteResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class AddAvailabilitySlotUseCaseTest {
    private val repository = FakeAvailabilityRepository()
    private val useCase = AddAvailabilitySlotUseCase(repository)

    // The schema already checks end > start; doing it here first avoids a
    // round trip for a value the client can tell is wrong, and lets the dialog
    // show the error instantly.
    @Test
    fun rejectsSlotWhenEndIsNotAfterStart() =
        runTest {
            val result = useCase(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 0))

            assertEquals(SlotWriteResult.Failure(AvailabilityError.EndNotAfterStart), result)
            assertTrue(repository.addedSlots.isEmpty())
        }

    @Test
    fun rejectsSlotWhenEndIsBeforeStart() =
        runTest {
            val result = useCase(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(9, 0))

            assertEquals(SlotWriteResult.Failure(AvailabilityError.EndNotAfterStart), result)
            assertTrue(repository.addedSlots.isEmpty())
        }

    @Test
    fun carriesOverlapErrorFromRepository() =
        runTest {
            repository.addResult = SlotWriteResult.Failure(AvailabilityError.OverlapsExistingSlot)

            val result = useCase(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))

            assertEquals(SlotWriteResult.Failure(AvailabilityError.OverlapsExistingSlot), result)
            assertEquals(1, repository.addedSlots.size)
        }

    @Test
    fun happyPathReachesTheRepository() =
        runTest {
            val result = useCase(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(9, 0))

            assertEquals(SlotWriteResult.Success, result)
            assertEquals(
                Triple(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(9, 0)),
                repository.addedSlots.single(),
            )
        }
}
