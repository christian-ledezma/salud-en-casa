package bo.saludencasa.features.availability.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import bo.saludencasa.features.availability.domain.model.AvailabilityResult
import bo.saludencasa.features.availability.domain.model.AvailabilitySlot
import bo.saludencasa.features.availability.domain.model.SlotWriteResult
import bo.saludencasa.features.availability.domain.model.WeeklyAvailability
import bo.saludencasa.features.availability.domain.usecase.AddAvailabilitySlotUseCase
import bo.saludencasa.features.availability.domain.usecase.GetMyAvailabilityUseCase
import bo.saludencasa.features.availability.domain.usecase.RemoveAvailabilitySlotUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

sealed interface AvailabilityUiState {
    data object Loading : AvailabilityUiState

    data class Content(
        val availability: WeeklyAvailability,
        val draft: SlotDraft? = null,
        val removal: AvailabilitySlot? = null,
        val busy: Boolean = false,
    ) : AvailabilityUiState

    data class Failed(
        val error: AvailabilityError,
    ) : AvailabilityUiState
}

data class SlotDraft(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val error: AvailabilityError? = null,
)

class AvailabilityViewModel(
    private val getMyAvailability: GetMyAvailabilityUseCase,
    private val addSlot: AddAvailabilitySlotUseCase,
    private val removeSlot: RemoveAvailabilitySlotUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<AvailabilityUiState>(AvailabilityUiState.Loading)
    val uiState: StateFlow<AvailabilityUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = AvailabilityUiState.Loading
        viewModelScope.launch {
            when (val result = getMyAvailability()) {
                is AvailabilityResult.Loaded -> state.value = AvailabilityUiState.Content(result.availability)
                is AvailabilityResult.Failure -> state.value = AvailabilityUiState.Failed(result.error)
            }
        }
    }

    fun onAddClick(day: DayOfWeek) {
        val content = state.value as? AvailabilityUiState.Content ?: return
        state.value =
            content.copy(
                draft =
                    SlotDraft(
                        dayOfWeek = day,
                        startTime = DEFAULT_START,
                        endTime = DEFAULT_END,
                    ),
            )
    }

    fun onCancelDraft() {
        val content = state.value as? AvailabilityUiState.Content ?: return
        state.value = content.copy(draft = null)
    }

    fun onDraftStart(time: LocalTime) =
        updateDraft { it.copy(startTime = time, endTime = SlotTimes.endAfter(time, it.endTime), error = null) }

    fun onDraftEnd(time: LocalTime) = updateDraft { it.copy(endTime = time, error = null) }

    fun onRemoveClick(slot: AvailabilitySlot) {
        val content = state.value as? AvailabilityUiState.Content ?: return
        state.value = content.copy(removal = slot)
    }

    fun onDismissRemoval() {
        val content = state.value as? AvailabilityUiState.Content ?: return
        state.value = content.copy(removal = null)
    }

    fun onConfirmAdd() {
        val content = state.value as? AvailabilityUiState.Content ?: return
        val draft = content.draft ?: return
        state.value = content.copy(busy = true)
        viewModelScope.launch {
            when (val result = addSlot(draft.dayOfWeek, draft.startTime, draft.endTime)) {
                SlotWriteResult.Success -> {
                    reload()
                }

                is SlotWriteResult.Failure -> {
                    state.value =
                        content.copy(
                            draft = draft.copy(error = result.error),
                            busy = false,
                        )
                }
            }
        }
    }

    fun onConfirmRemove() {
        val content = state.value as? AvailabilityUiState.Content ?: return
        val removal = content.removal ?: return
        state.value = content.copy(busy = true)
        viewModelScope.launch {
            when (removeSlot(removal.id)) {
                SlotWriteResult.Success -> {
                    reload()
                }

                // A failed removal keeps the slot by reloading the page, and
                // surfaces the error without replacing the whole screen.
                is SlotWriteResult.Failure -> {
                    state.value = content.copy(busy = false, removal = null)
                    reload()
                }
            }
        }
    }

    private suspend fun reload() {
        when (val result = getMyAvailability()) {
            is AvailabilityResult.Loaded -> {
                state.value = AvailabilityUiState.Content(result.availability)
            }

            is AvailabilityResult.Failure -> {
                state.value = AvailabilityUiState.Failed(result.error)
            }
        }
    }

    private inline fun updateDraft(crossinline transform: (SlotDraft) -> SlotDraft) {
        state.update { current ->
            if (current !is AvailabilityUiState.Content) return@update current
            val draft = current.draft ?: return@update current
            current.copy(draft = transform(draft))
        }
    }

    private companion object {
        val DEFAULT_START: LocalTime = LocalTime.of(9, 0)
        val DEFAULT_END: LocalTime = LocalTime.of(10, 0)
    }
}
