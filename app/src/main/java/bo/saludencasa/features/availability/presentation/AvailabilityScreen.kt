package bo.saludencasa.features.availability.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import bo.saludencasa.features.availability.domain.model.AvailabilitySlot
import bo.saludencasa.features.availability.domain.model.WeeklyAvailability
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.components.TimeChipRow
import bo.saludencasa.ui.components.timeChipIndex
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun AvailabilityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvailabilityViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AvailabilityContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onAddClick = viewModel::onAddClick,
        onRemoveClick = viewModel::onRemoveClick,
        onDraftStart = viewModel::onDraftStart,
        onDraftEnd = viewModel::onDraftEnd,
        onConfirmAdd = viewModel::onConfirmAdd,
        onCancelDraft = viewModel::onCancelDraft,
        onConfirmRemove = viewModel::onConfirmRemove,
        onDismissRemoval = viewModel::onDismissRemoval,
        modifier = modifier,
    )
}

@Composable
internal fun AvailabilityContent(
    uiState: AvailabilityUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAddClick: (DayOfWeek) -> Unit,
    onRemoveClick: (AvailabilitySlot) -> Unit,
    onDraftStart: (LocalTime) -> Unit,
    onDraftEnd: (LocalTime) -> Unit,
    onConfirmAdd: () -> Unit,
    onCancelDraft: () -> Unit,
    onConfirmRemove: () -> Unit,
    onDismissRemoval: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                // The screen paints its own background, so the dark preview
                // looks like the phone (docs/decisions.md, 2026-10-14).
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        ScreenHeader(
            title = stringResource(R.string.availability_title),
            onBack = onBack,
            subtitle = stringResource(R.string.availability_subtitle),
        )

        when (uiState) {
            AvailabilityUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDescription },
                    )
                }
            }

            is AvailabilityUiState.Failed -> {
                Text(
                    text = stringForError(uiState.error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
            }

            is AvailabilityUiState.Content -> {
                WeekList(
                    availability = uiState.availability,
                    draft = uiState.draft,
                    busy = uiState.busy,
                    onAddClick = onAddClick,
                    onRemoveClick = onRemoveClick,
                    onDraftStart = onDraftStart,
                    onDraftEnd = onDraftEnd,
                    onConfirmAdd = onConfirmAdd,
                    onCancelDraft = onCancelDraft,
                )

                uiState.removal?.let { slot ->
                    RemoveSlotDialog(
                        slot = slot,
                        busy = uiState.busy,
                        onConfirm = onConfirmRemove,
                        onDismiss = onDismissRemoval,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekList(
    availability: WeeklyAvailability,
    draft: SlotDraft?,
    busy: Boolean,
    onAddClick: (DayOfWeek) -> Unit,
    onRemoveClick: (AvailabilitySlot) -> Unit,
    onDraftStart: (LocalTime) -> Unit,
    onDraftEnd: (LocalTime) -> Unit,
    onConfirmAdd: () -> Unit,
    onCancelDraft: () -> Unit,
) {
    val allEmpty = availability.slotsByDay.values.all { it.isEmpty() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        contentPadding = PaddingValues(bottom = Spacing.scale48),
    ) {
        if (allEmpty) {
            item(key = "empty-hint") {
                EmptyHint()
            }
        }

        DayOfWeek.entries.forEach { day ->
            val slots = availability.slotsByDay[day].orEmpty()
            item(key = "header-${day.name}") {
                DayHeader(dayName = stringResource(day.labelRes()))
            }
            items(slots, key = AvailabilitySlot::id) { slot ->
                SlotRow(slot = slot, onRemoveClick = { onRemoveClick(slot) })
            }
            item(key = "draft-${day.name}") {
                if (draft?.dayOfWeek == day) {
                    SlotEditor(
                        draft = draft,
                        busy = busy,
                        onDraftStart = onDraftStart,
                        onDraftEnd = onDraftEnd,
                        onConfirm = onConfirmAdd,
                        onCancel = onCancelDraft,
                    )
                } else {
                    OutlinedButton(
                        onClick = { onAddClick(day) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Text(
                            modifier = Modifier.padding(start = Spacing.scale8),
                            text = stringResource(R.string.availability_add_slot),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotEditor(
    draft: SlotDraft,
    busy: Boolean,
    onDraftStart: (LocalTime) -> Unit,
    onDraftEnd: (LocalTime) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val loadingDescription = stringResource(R.string.cd_loading)
    val startState = rememberLazyListState()
    LaunchedEffect(Unit) { startState.scrollToItem(timeChipIndex(SlotTimes.starts, draft.startTime)) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.labelToField),
        ) {
            FieldLabel(text = stringResource(R.string.availability_start_label))
            TimeChipRow(
                times = SlotTimes.starts,
                selected = draft.startTime,
                onSelected = onDraftStart,
                state = startState,
            )

            FieldLabel(text = stringResource(R.string.availability_end_label))
            TimeChipRow(
                times = SlotTimes.ends(draft.startTime),
                selected = draft.endTime,
                onSelected = onDraftEnd,
            )

            draft.error?.let { error ->
                Text(
                    text = stringForError(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }

            PrimaryButton(
                text = stringResource(R.string.availability_add_confirm),
                onClick = onConfirm,
                enabled = !busy,
            )
            TextButton(
                onClick = onCancel,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyHint() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                modifier = Modifier.size(Spacing.scale40),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.availability_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.availability_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayHeader(dayName: String) {
    Text(
        text = dayName,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SlotRow(
    slot: AvailabilitySlot,
    onRemoveClick: () -> Unit,
) {
    val removeLabel = stringResource(R.string.availability_remove_slot_cd)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.cardPadding, vertical = Spacing.scale8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text =
                    stringResource(
                        R.string.availability_slot_range,
                        formattedTime(slot.startTime),
                        formattedTime(slot.endTime),
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = removeLabel,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RemoveSlotDialog(
    slot: AvailabilitySlot,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.availability_remove_dialog_title)) },
        text = {
            Text(
                text =
                    stringResource(
                        R.string.availability_remove_dialog_message,
                        formattedTime(slot.startTime),
                        formattedTime(slot.endTime),
                    ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(text = stringResource(R.string.availability_remove_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

private fun previewSlots(): List<AvailabilitySlot> =
    listOf(
        AvailabilitySlot(
            id = "1",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(12, 0),
        ),
        AvailabilitySlot(
            id = "2",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(17, 0),
        ),
        AvailabilitySlot(
            id = "3",
            dayOfWeek = DayOfWeek.WEDNESDAY,
            startTime = LocalTime.of(8, 30),
            endTime = LocalTime.of(10, 0),
        ),
    )

@Composable
private fun AvailabilityPreview(
    darkTheme: Boolean,
    uiState: AvailabilityUiState,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        AvailabilityContent(
            uiState = uiState,
            onBack = {},
            onRetry = {},
            onAddClick = {},
            onRemoveClick = {},
            onDraftStart = {},
            onDraftEnd = {},
            onConfirmAdd = {},
            onCancelDraft = {},
            onConfirmRemove = {},
            onDismissRemoval = {},
        )
    }
}

@Preview(showBackground = true, name = "Disponibilidad con franjas (claro)")
@Composable
private fun AvailabilityContentPreview() {
    AvailabilityPreview(
        darkTheme = false,
        uiState = AvailabilityUiState.Content(WeeklyAvailability.from(previewSlots())),
    )
}

@Preview(showBackground = true, name = "Nueva franja del lunes (claro)")
@Composable
private fun AvailabilityEditorPreview() {
    AvailabilityPreview(
        darkTheme = false,
        uiState =
            AvailabilityUiState.Content(
                availability = WeeklyAvailability.from(previewSlots()),
                draft =
                    SlotDraft(
                        dayOfWeek = DayOfWeek.MONDAY,
                        startTime = LocalTime.of(9, 0),
                        endTime = LocalTime.of(10, 0),
                    ),
            ),
    )
}

@Preview(showBackground = true, name = "Nueva franja al 200 % (oscuro)", fontScale = 2f)
@Composable
private fun AvailabilityEditorDarkLargeFontPreview() {
    AvailabilityPreview(
        darkTheme = true,
        uiState =
            AvailabilityUiState.Content(
                availability = WeeklyAvailability.from(previewSlots()),
                draft =
                    SlotDraft(
                        dayOfWeek = DayOfWeek.MONDAY,
                        startTime = LocalTime.of(14, 0),
                        endTime = LocalTime.of(17, 30),
                        error = AvailabilityError.OverlapsExistingSlot,
                    ),
            ),
    )
}

@Preview(showBackground = true, name = "Disponibilidad vacía (oscuro)")
@Composable
private fun AvailabilityEmptyDarkPreview() {
    AvailabilityPreview(
        darkTheme = true,
        uiState = AvailabilityUiState.Content(availability = WeeklyAvailability.Empty),
    )
}

@Preview(showBackground = true, name = "Disponibilidad con error (fuente grande)", fontScale = 2f)
@Composable
private fun AvailabilityFailedLargeFontPreview() {
    AvailabilityPreview(
        darkTheme = false,
        uiState = AvailabilityUiState.Failed(AvailabilityError.NetworkUnavailable),
    )
}

@Preview(showBackground = true, name = "Disponibilidad con franjas (fuente grande)", fontScale = 2f)
@Composable
private fun AvailabilityContentLargeFontPreview() {
    AvailabilityPreview(
        darkTheme = false,
        uiState = AvailabilityUiState.Content(WeeklyAvailability.from(previewSlots())),
    )
}
