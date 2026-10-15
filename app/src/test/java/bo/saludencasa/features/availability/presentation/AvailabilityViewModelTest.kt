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

    @Test
    fun theInitialLoadReachesContentWithTheRepositoryData() =
        runTest {
            repository.seed(slot(id = "a", dayOfWeek = DayOfWeek.MONDAY))

            val viewModel = viewModel()
            advanceUntilIdle()

            val content = viewModel.uiState.value as AvailabilityUiState.Content
            assertEquals(
                "a",
                content.availability.slotsByDay
                    .getValue(DayOfWeek.MONDAY)
                    .single()
                    .id,
            )
            assertNull(content.dialog)
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

    @Test
    fun theAddDialogOpensWithTheChosenDay() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onAddClick(DayOfWeek.FRIDAY)

            val dialog =
                (viewModel.uiState.value as AvailabilityUiState.Content).dialog
                    as SlotDialog.Adding
            assertEquals(DayOfWeek.FRIDAY, dialog.dayOfWeek)
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

            val content = viewModel.uiState.value as AvailabilityUiState.Content
            assertNull(content.dialog)
            assertEquals(
                listOf(DayOfWeek.MONDAY to (LocalTime.of(9, 0) to LocalTime.of(10, 0))),
                content.availability.slotsByDay.getValue(DayOfWeek.MONDAY).map {
                    it.dayOfWeek to (it.startTime to it.endTime)
                },
            )
        }

    // The failed add keeps the dialog open with its error, so the user can
    // correct the hours without re-opening and re-typing.
    @Test
    fun anOverlapKeepsTheDialogWithTheError() =
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

            val content = viewModel.uiState.value as AvailabilityUiState.Content
            val dialog = content.dialog as SlotDialog.Adding
            assertEquals(AvailabilityError.OverlapsExistingSlot, dialog.error)
            assertEquals(false, content.busy)
        }

    @Test
    fun removalRemovesTheSlotAndClosesTheDialog() =
        runTest {
            repository.seed(slot(id = "to-go", dayOfWeek = DayOfWeek.THURSDAY))
            val viewModel = viewModel()
            advanceUntilIdle()
            val content = viewModel.uiState.value as AvailabilityUiState.Content
            val target =
                content.availability.slotsByDay
                    .getValue(DayOfWeek.THURSDAY)
                    .single()

            viewModel.onRemoveClick(target)
            viewModel.onConfirmRemove()
            advanceUntilIdle()

            val after = viewModel.uiState.value as AvailabilityUiState.Content
            assertNull(after.dialog)
            assertTrue(
                after.availability.slotsByDay
                    .getValue(DayOfWeek.THURSDAY)
                    .isEmpty(),
            )
            assertEquals("to-go", repository.removedIds.single())
        }
}
