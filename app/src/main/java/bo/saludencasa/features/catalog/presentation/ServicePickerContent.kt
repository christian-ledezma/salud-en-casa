package bo.saludencasa.features.catalog.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.GroupedRow
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.SearchField
import bo.saludencasa.ui.theme.Spacing
import java.util.Locale

// The catalog opened over the list: what is still to declare, searchable, with
// several chosen at once (docs/decisions.md, 2026-11-25, HT-32). The search
// field stays at the top while the rows scroll under it.
@Composable
internal fun ServicePickerContent(
    content: MyServicesUiState.Content,
    picker: ServicePicker,
    actions: MyServicesActions,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val types = content.pickerTypes
    val loadingDescription = stringResource(R.string.cd_loading)

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.scale8),
        ) {
            item(key = "header") {
                PickerHeader(onClose = actions.onPickerDismiss)
            }

            stickyHeader(key = "search") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = Spacing.scale8),
                ) {
                    SearchField(
                        value = picker.query,
                        onValueChange = actions.onPickerQueryChanged,
                        placeholder = stringResource(R.string.catalog_search_catalog_placeholder),
                        clearDescription = stringResource(R.string.cd_catalog_clear_search),
                        enabled = !content.busy,
                    )
                }
            }

            item(key = "summary") {
                Column(
                    modifier = Modifier.padding(vertical = Spacing.scale8),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    Text(
                        text =
                            if (picker.query.isBlank()) {
                                pluralStringResource(R.plurals.catalog_picker_count, types.size, types.size)
                            } else {
                                pluralStringResource(
                                    R.plurals.catalog_picker_results,
                                    types.size,
                                    types.size,
                                    picker.query.trim(),
                                )
                            },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.catalog_picker_price_note),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (types.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text =
                            if (picker.query.isBlank()) {
                                stringResource(R.string.catalog_available_empty)
                            } else {
                                stringResource(R.string.catalog_picker_no_results, picker.query.trim())
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.scale16),
                    )
                }
            }

            itemsIndexed(types, key = { _, type -> type.id }) { index, type ->
                PickerRow(
                    type = type,
                    index = index,
                    count = types.size,
                    selected = type.id in picker.selected,
                    enabled = !content.busy,
                    locale = locale,
                    onToggle = { actions.onPickerToggle(type.id) },
                )
            }
        }

        BottomActionBar {
            picker.error?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (content.busy) {
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = loadingDescription },
                )
            }
            PrimaryButton(
                text =
                    if (picker.selected.isEmpty()) {
                        stringResource(R.string.catalog_picker_confirm_none)
                    } else {
                        pluralStringResource(
                            R.plurals.catalog_picker_confirm,
                            picker.selected.size,
                            picker.selected.size,
                        )
                    },
                onClick = actions.onConfirmDeclaration,
                enabled = picker.selected.isNotEmpty() && !content.busy,
            )
        }
    }
}

@Composable
private fun PickerHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_catalog_picker_close),
            )
        }
        Text(
            text = stringResource(R.string.catalog_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PickerRow(
    type: ServiceType,
    index: Int,
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    locale: Locale,
    onToggle: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    GroupedRow(
        index = index,
        count = count,
        containerColor = if (selected) colors.secondaryContainer else colors.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = selected,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { onToggle() },
                    ).padding(horizontal = Spacing.cardPadding, vertical = Spacing.scale12),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The row is the control: the checkbox only draws its state.
            Checkbox(checked = selected, onCheckedChange = null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                Text(
                    text = type.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) colors.onSecondaryContainer else colors.onSurface,
                )
                Text(
                    text =
                        stringResource(
                            R.string.catalog_picker_row_meta,
                            formatBob(type.referencePriceBob, locale),
                            durationLabel(type.estimatedDurationMin),
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                )
            }
        }
    }
}
