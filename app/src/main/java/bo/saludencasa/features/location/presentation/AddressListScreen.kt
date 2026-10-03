package bo.saludencasa.features.location.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddressListScreen(
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddressListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AddressListContent(
        uiState = uiState,
        onSetPrimaryClick = viewModel::onSetPrimaryClick,
        onSetProfessionalBaseClick = viewModel::onSetProfessionalBaseClick,
        onDeleteClick = viewModel::onDeleteClick,
        onConfirmDeleteClick = viewModel::onConfirmDelete,
        onDismissDeleteClick = viewModel::onDismissDeleteConfirmation,
        onRetryClick = viewModel::load,
        onAddClick = onAddClick,
        onEditClick = onEditClick,
        modifier = modifier,
    )
}

@Composable
private fun AddressListContent(
    uiState: AddressListUiState,
    onSetPrimaryClick: (String) -> Unit,
    onSetProfessionalBaseClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onConfirmDeleteClick: () -> Unit,
    onDismissDeleteClick: () -> Unit,
    onRetryClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        AddressListUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        AddressListUiState.Empty -> {
            CenteredMessage(modifier = modifier) {
                Text(
                    text = stringResource(R.string.address_list_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.address_list_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(text = stringResource(R.string.address_list_add), onClick = onAddClick)
            }
        }

        is AddressListUiState.Failed -> {
            CenteredMessage(modifier = modifier) {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetryClick)
            }
        }

        is AddressListUiState.Content -> {
            AddressListItems(
                content = uiState,
                onSetPrimaryClick = onSetPrimaryClick,
                onSetProfessionalBaseClick = onSetProfessionalBaseClick,
                onDeleteClick = onDeleteClick,
                onConfirmDeleteClick = onConfirmDeleteClick,
                onDismissDeleteClick = onDismissDeleteClick,
                onAddClick = onAddClick,
                onEditClick = onEditClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CenteredMessage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun AddressListItems(
    content: AddressListUiState.Content,
    onSetPrimaryClick: (String) -> Unit,
    onSetProfessionalBaseClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onConfirmDeleteClick: () -> Unit,
    onDismissDeleteClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            Text(
                text = stringResource(R.string.address_list_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            content.notice?.let { error ->
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            PrimaryButton(text = stringResource(R.string.address_list_add), onClick = onAddClick)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding =
                PaddingValues(
                    horizontal = Spacing.screenMargin,
                    vertical = Spacing.scale8,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
        ) {
            items(content.addresses, key = { it.id }) { address ->
                AddressListRow(
                    address = address,
                    isPending = content.pendingId == address.id,
                    canDeclareProfessionalBase = content.canDeclareProfessionalBase,
                    onSetPrimaryClick = { onSetPrimaryClick(address.id) },
                    onSetProfessionalBaseClick = { onSetProfessionalBaseClick(address.id) },
                    onEditClick = { onEditClick(address.id) },
                    onDeleteClick = { onDeleteClick(address.id) },
                )
            }
        }
    }

    if (content.confirmingDeleteId != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteClick,
            title = { Text(text = stringResource(R.string.address_list_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.address_list_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteClick) {
                    Text(text = stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteClick) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun AddressListRow(
    address: Address,
    isPending: Boolean,
    canDeclareProfessionalBase: Boolean,
    onSetPrimaryClick: () -> Unit,
    onSetProfessionalBaseClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = address.alias, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                    if (address.isPrimary) {
                        Text(
                            text = stringResource(R.string.address_list_primary_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = SaludEnCasaTheme.statusColors.positive,
                        )
                    }
                    if (address.isProfessionalBase && canDeclareProfessionalBase) {
                        Text(
                            text = stringResource(R.string.address_list_professional_base_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Text(text = address.addressText, style = MaterialTheme.typography.bodyMedium)

            address.reference?.let { reference ->
                Text(
                    text = reference,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isPending) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            } else {
                // FlowRow and not Row: a plain Row does not wrap, and the fourth
                // action pushed "Editar" and "Eliminar" clean out of the layout
                // -- they were not clipped, they stopped being composed at all,
                // which left an address impossible to edit or delete. It is also
                // what keeps the actions reachable at 200 % font size.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    if (!address.isPrimary) {
                        TextButton(onClick = onSetPrimaryClick) {
                            Text(text = stringResource(R.string.address_list_set_primary_action))
                        }
                    }
                    if (canDeclareProfessionalBase && !address.isProfessionalBase) {
                        TextButton(onClick = onSetProfessionalBaseClick) {
                            Text(text = stringResource(R.string.address_list_set_professional_base_action))
                        }
                    }
                    TextButton(onClick = onEditClick) {
                        Text(text = stringResource(R.string.common_edit))
                    }
                    TextButton(onClick = onDeleteClick) {
                        Text(text = stringResource(R.string.common_delete))
                    }
                }
            }
        }
    }
}

private fun previewAddress(
    id: String,
    alias: String,
    isPrimary: Boolean,
    isProfessionalBase: Boolean = false,
    reference: String? = "Portón verde, al lado de la farmacia",
): Address =
    Address(
        id = id,
        alias = alias,
        addressText = "Avenida Arce 2081, La Paz",
        reference = reference,
        city = "La Paz",
        coordinate = Coordinate.create(-16.4957, -68.1335).getOrThrow(),
        isPrimary = isPrimary,
        isProfessionalBase = isProfessionalBase,
    )

private fun previewContent(
    pendingId: String? = null,
    confirmingDeleteId: String? = null,
    notice: AddressError? = null,
    canDeclareProfessionalBase: Boolean = false,
): AddressListUiState.Content =
    AddressListUiState.Content(
        addresses =
            listOf(
                previewAddress(id = "1", alias = "Casa", isPrimary = true),
                previewAddress(id = "2", alias = "Consultorio de mi madre", isPrimary = false, reference = null),
            ),
        pendingId = pendingId,
        confirmingDeleteId = confirmingDeleteId,
        notice = notice,
        canDeclareProfessionalBase = canDeclareProfessionalBase,
    )

@Preview(showBackground = true, heightDp = 700, name = "Lista de direcciones, claro")
@Composable
private fun AddressListContentLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressListContent(previewContent(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Lista de direcciones, oscuro")
@Composable
private fun AddressListContentDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        AddressListContent(previewContent(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Confirmando eliminacion")
@Composable
private fun AddressListContentConfirmingDeletePreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressListContent(previewContent(confirmingDeleteId = "2"), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Sin direcciones registradas")
@Composable
private fun AddressListEmptyPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressListContent(AddressListUiState.Empty, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cargando la lista")
@Composable
private fun AddressListLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressListContent(AddressListUiState.Loading, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "La lista no se pudo leer")
@Composable
private fun AddressListFailedPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        AddressListContent(
            AddressListUiState.Failed(AddressError.NetworkUnavailable),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }
}
