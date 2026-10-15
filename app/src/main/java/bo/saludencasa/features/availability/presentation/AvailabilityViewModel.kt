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
        val dialog: SlotDialog? = null,
        val busy: Boolean = false,
    ) : AvailabilityUiState

    data class Failed(
        val error: AvailabilityError,
    ) : AvailabilityUiState
}

sealed interface SlotDialog {
    data class Adding(
        val dayOfWeek: DayOfWeek,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val error: AvailabilityError? = null,
    ) : SlotDialog

    data class Removing(
        val slot: AvailabilitySlot,
    ) : SlotDialog
}

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
                dialog =
                    SlotDialog.Adding(
                        dayOfWeek = day,
                        startTime = DEFAULT_START,
                        endTime = DEFAULT_END,
                    ),
            )
    }

    fun onRemoveClick(slot: AvailabilitySlot) {
        val content = state.value as? AvailabilityUiState.Content ?: return
        state.value = content.copy(dialog = SlotDialog.Removing(slot))
    }

    fun onDismissDialog() {
        val content = state.value as? AvailabilityUiState.Content ?: return
        state.value = content.copy(dialog = null)
    }

    fun onDraftStart(time: LocalTime) = updateAdding { it.copy(startTime = time, error = null) }

    fun onDraftEnd(time: LocalTime) = updateAdding { it.copy(endTime = time, error = null) }

    fun onDraftDay(day: DayOfWeek) = updateAdding { it.copy(dayOfWeek = day, error = null) }

    fun onConfirmAdd() {
        val content = state.value as? AvailabilityUiState.Content ?: return
        val adding = content.dialog as? SlotDialog.Adding ?: return
        state.value = content.copy(busy = true)
        viewModelScope.launch {
            when (val result = addSlot(adding.dayOfWeek, adding.startTime, adding.endTime)) {
                SlotWriteResult.Success -> {
                    reload()
                }

                is SlotWriteResult.Failure -> {
                    state.value =
                        content.copy(
                            dialog = adding.copy(error = result.error),
                            busy = false,
                        )
                }
            }
        }
    }

    fun onConfirmRemove() {
        val content = state.value as? AvailabilityUiState.Content ?: return
        val removing = content.dialog as? SlotDialog.Removing ?: return
        state.value = content.copy(busy = true)
        viewModelScope.launch {
            when (val result = removeSlot(removing.slot.id)) {
                SlotWriteResult.Success -> {
                    reload()
                }

                // A failed removal keeps the slot by reloading the page, and
                // surfaces the error without replacing the whole screen.
                is SlotWriteResult.Failure -> {
                    state.value = content.copy(busy = false, dialog = null)
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

    private inline fun updateAdding(crossinline transform: (SlotDialog.Adding) -> SlotDialog.Adding) {
        state.update { current ->
            if (current !is AvailabilityUiState.Content) return@update current
            val adding = current.dialog as? SlotDialog.Adding ?: return@update current
            current.copy(dialog = transform(adding))
        }
    }

    private companion object {
        val DEFAULT_START: LocalTime = LocalTime.of(9, 0)
        val DEFAULT_END: LocalTime = LocalTime.of(10, 0)
    }
}
