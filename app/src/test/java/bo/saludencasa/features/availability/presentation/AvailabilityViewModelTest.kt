@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.availability.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.availability.FakeAvailabilityRepository
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import bo.saludencasa.features.availability.domain.model.AvailabilityResult
import bo.saludencasa.features.availability.domain.model.SlotWriteResult
import bo.saludencasa.features.availability.domain.usecase.AddAvailabilitySlotUseCase
import bo.saludencasa.features.availability.domain.usecase.GetMyAvailabilityUseCase
import bo.saludencasa.features.availability.domain.usecase.RemoveAvailabilitySlotUseCase
import bo.saludencasa.features.availability.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class AvailabilityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAvailabilityRepository()

    private fun viewModel(): AvailabilityViewModel =
        AvailabilityViewModel(
            GetMyAvailabilityUseCase(repository),
            AddAvailabilitySlotUseCase(repository),
            RemoveAvailabilitySlotUseCase(repository),
        )

    private fun content(viewModel: AvailabilityViewModel): AvailabilityUiState.Content =
        viewModel.uiState.value as AvailabilityUiState.Content

    @Test
    fun theInitialLoadReachesContentWithTheRepositoryData() =
        runTest {
            repository.seed(slot(id = "a", dayOfWeek = DayOfWeek.MONDAY))

            val viewModel = viewModel()
            advanceUntilIdle()

            val content = content(viewModel)
            assertEquals(
                "a",
                content.availability.slotsByDay
                    .getValue(DayOfWeek.MONDAY)
                    .single()
                    .id,
            )
            assertNull(content.draft)
        }

    @Test
    fun aFailedLoadIsReported() =
        runTest {
            repository.getResult = AvailabilityResult.Failure(AvailabilityError.NetworkUnavailable)

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                AvailabilityUiState.Failed(AvailabilityError.NetworkUnavailable),
                viewModel.uiState.value,
            )
        }

    // The editor belongs to the day whose button was pressed: there is no day
    // picker left to correct a wrong one.
    @Test
    fun theEditorOpensOnTheChosenDay() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onAddClick(DayOfWeek.FRIDAY)

            assertEquals(DayOfWeek.FRIDAY, content(viewModel).draft?.dayOfWeek)
        }

    // Without this the professional can leave the editor on an impossible
    // range —ends before it starts— and only find out when the engine says no.
    @Test
    fun aStartMovedPastTheEndCarriesTheEndWithIt() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddClick(DayOfWeek.MONDAY)
            viewModel.onDraftEnd(LocalTime.of(10, 0))

            viewModel.onDraftStart(LocalTime.of(14, 0))

            val draft = content(viewModel).draft
            assertEquals(LocalTime.of(14, 0), draft?.startTime)
            assertEquals(LocalTime.of(14, 30), draft?.endTime)
        }

    @Test
    fun aStartMovedBeforeTheEndLeavesTheEndWhereItWas() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddClick(DayOfWeek.MONDAY)
            viewModel.onDraftEnd(LocalTime.of(18, 0))

            viewModel.onDraftStart(LocalTime.of(14, 0))

            assertEquals(LocalTime.of(18, 0), content(viewModel).draft?.endTime)
        }

    // The point of this one: after a successful add the view model re-asks the
    // repository instead of patching its own copy. That is what guarantees the
    // list reflects whatever the server actually stored.
    @Test
    fun aSuccessfulAddReloadsFromTheRepository() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddClick(DayOfWeek.MONDAY)
            viewModel.onDraftStart(LocalTime.of(9, 0))
            viewModel.onDraftEnd(LocalTime.of(10, 0))

            viewModel.onConfirmAdd()
            advanceUntilIdle()

            val content = content(viewModel)
            assertNull(content.draft)
            assertEquals(
                listOf(DayOfWeek.MONDAY to (LocalTime.of(9, 0) to LocalTime.of(10, 0))),
                content.availability.slotsByDay.getValue(DayOfWeek.MONDAY).map {
                    it.dayOfWeek to (it.startTime to it.endTime)
                },
            )
        }

    // The failed add keeps the editor open with its error, so the professional
    // can correct the hours without opening it again.
    @Test
    fun anOverlapKeepsTheEditorWithTheError() =
        runTest {
            repository.addResult =
                SlotWriteResult.Failure(AvailabilityError.OverlapsExistingSlot)

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddClick(DayOfWeek.MONDAY)
            viewModel.onDraftStart(LocalTime.of(9, 0))
            viewModel.onDraftEnd(LocalTime.of(10, 0))

            viewModel.onConfirmAdd()
            advanceUntilIdle()

            val content = content(viewModel)
            assertEquals(AvailabilityError.OverlapsExistingSlot, content.draft?.error)
            assertEquals(false, content.busy)
        }

    @Test
    fun cancellingTheEditorDiscardsTheDraftAndNothingIsWritten() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddClick(DayOfWeek.MONDAY)
            viewModel.onDraftStart(LocalTime.of(9, 0))

            viewModel.onCancelDraft()
            advanceUntilIdle()

            assertNull(content(viewModel).draft)
            assertTrue(repository.addedSlots.isEmpty())
        }

    @Test
    fun removalRemovesTheSlotAndClosesTheDialog() =
        runTest {
            repository.seed(slot(id = "to-go", dayOfWeek = DayOfWeek.THURSDAY))
            val viewModel = viewModel()
            advanceUntilIdle()
            val target =
                content(viewModel)
                    .availability.slotsByDay
                    .getValue(DayOfWeek.THURSDAY)
                    .single()

            viewModel.onRemoveClick(target)
            viewModel.onConfirmRemove()
            advanceUntilIdle()

            val after = content(viewModel)
            assertNull(after.removal)
            assertTrue(
                after.availability.slotsByDay
                    .getValue(DayOfWeek.THURSDAY)
                    .isEmpty(),
            )
            assertEquals("to-go", repository.removedIds.single())
        }
}
