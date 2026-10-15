@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.availability.domain.usecase

import bo.saludencasa.features.availability.FakeAvailabilityRepository
import bo.saludencasa.features.availability.domain.model.AvailabilityResult
import bo.saludencasa.features.availability.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class GetMyAvailabilityUseCaseTest {
    private val repository = FakeAvailabilityRepository()
    private val useCase = GetMyAvailabilityUseCase(repository)

    // The screen draws one section per day, including the untouched ones. The
    // use case must deliver the seven keys so the UI never has to pick a
    // "missing" branch.
    @Test
    fun theWeeklyMapAlwaysHasTheSevenDays() =
        runTest {
            repository.seed(slot(id = "1", dayOfWeek = DayOfWeek.MONDAY))

            val loaded = useCase() as AvailabilityResult.Loaded

            assertEquals(7, loaded.availability.slotsByDay.size)
            assertEquals(
                setOf<DayOfWeek>(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY,
                ),
                loaded.availability.slotsByDay.keys,
            )
            assertTrue(
                loaded.availability.slotsByDay
                    .getValue(DayOfWeek.SUNDAY)
                    .isEmpty(),
            )
        }

    @Test
    fun slotsInsideADayAreSortedByStartTime() =
        runTest {
            repository.seed(
                slot(
                    id = "late",
                    dayOfWeek = DayOfWeek.FRIDAY,
                    startTime = LocalTime.of(14, 0),
                    endTime = LocalTime.of(16, 0),
                ),
                slot(
                    id = "early",
                    dayOfWeek = DayOfWeek.FRIDAY,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(9, 0),
                ),
            )

            val loaded = useCase() as AvailabilityResult.Loaded

            assertEquals(
                listOf("early", "late"),
                loaded.availability.slotsByDay
                    .getValue(DayOfWeek.FRIDAY)
                    .map { it.id },
            )
        }
}
