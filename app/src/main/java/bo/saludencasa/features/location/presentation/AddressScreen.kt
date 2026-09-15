package bo.saludencasa.features.location.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.MapDefaults
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddressScreen(
    modifier: Modifier = Modifier,
    viewModel: AddressViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            viewModel.onPermissionResult(granted.values.any { it })
        }

    AddressContent(
        uiState = uiState,
        onFormChange = viewModel::onFormChange,
        onQueryChange = viewModel::onQueryChange,
        onSearchClick = viewModel::onSearch,
        onAllowLocationClick = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
        onDeclineLocationClick = viewModel::onPermissionDeclined,
        onSaveClick = viewModel::save,
        onRetryClick = viewModel::load,
        modifier = modifier,
    ) { content, mapModifier ->
        AddressMap(
            point = content.point,
            camera = content.camera,
            myLocationEnabled = content.permission == LocationPermissionState.Granted,
            onPointPicked = viewModel::onPointPicked,
            modifier = mapModifier,
        )
    }
}

// The map arrives as a slot so that this function, its previews and its tests
// keep working without the Maps SDK, which needs a key and a device before it
// draws anything.
@Composable
private fun AddressContent(
    uiState: AddressUiState,
    onFormChange: (AddressForm) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onAllowLocationClick: () -> Unit,
    onDeclineLocationClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable (AddressUiState.Content, Modifier) -> Unit,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        AddressUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        is AddressUiState.Failed -> {
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
                ) {
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetryClick)
                }
            }
        }

        is AddressUiState.Content -> {
            EditableAddress(
                content = uiState,
                onFormChange = onFormChange,
                onQueryChange = onQueryChange,
                onSearchClick = onSearchClick,
                onAllowLocationClick = onAllowLocationClick,
                onDeclineLocationClick = onDeclineLocationClick,
                onSaveClick = onSaveClick,
                modifier = modifier,
                map = map,
            )
        }
    }
}

@Composable
private fun EditableAddress(
    content: AddressUiState.Content,
    onFormChange: (AddressForm) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onAllowLocationClick: () -> Unit,
    onDeclineLocationClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable (AddressUiState.Content, Modifier) -> Unit,
) {
    val form = content.form
    val fieldError = (content.status as? SaveStatus.Failed)?.error

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        Text(
            text = stringResource(R.string.address_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (content.point == null && content.permission == LocationPermissionState.NotRequested) {
            LocationRationale(
                onAllowClick = onAllowLocationClick,
                onDeclineClick = onDeclineLocationClick,
            )
        }

        SearchSection(
            query = content.query,
            isSearching = content.isSearching,
            onQueryChange = onQueryChange,
            onSearchClick = onSearchClick,
        )

        map(content, Modifier.fillMaxWidth().height(Spacing.mapHeight))

        Text(
            text =
                stringResource(
                    if (content.point == null) R.string.address_point_hint else R.string.address_drag_hint,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (content.isLocating) {
            Text(
                text = stringResource(R.string.address_locating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        content.notice?.let { notice ->
            Text(
                text = stringResource(notice.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FormField(
            label = stringResource(R.string.address_alias_label),
            value = form.alias,
            onValueChange = { onFormChange(form.copy(alias = it)) },
            errorText = fieldError.messageFor(AddressError.InvalidAlias),
        )

        FormField(
            label = stringResource(R.string.address_text_label),
            value = form.addressText,
            onValueChange = { onFormChange(form.copy(addressText = it)) },
            errorText = fieldError.messageFor(AddressError.InvalidAddressText),
            singleLine = false,
        )

        FormField(
            label = stringResource(R.string.address_city_label),
            value = form.city,
            onValueChange = { onFormChange(form.copy(city = it)) },
            errorText = fieldError.messageFor(AddressError.InvalidCity),
        )

        FormField(
            label = stringResource(R.string.address_reference_label),
            value = form.reference,
            onValueChange = { onFormChange(form.copy(reference = it)) },
            errorText = fieldError.messageFor(AddressError.InvalidReference),
            singleLine = false,
        )

        if (content.isPrimary) {
            Text(
                text = stringResource(R.string.address_primary_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SaveSection(status = content.status, onSaveClick = onSaveClick)
    }
}

// RF-03.6: the reason comes before the system dialog, and declining is one of
// the two ways out of this card rather than a dead end.
@Composable
private fun LocationRationale(
    onAllowClick: () -> Unit,
    onDeclineClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            Text(
                text = stringResource(R.string.address_permission_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.address_permission_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryButton(text = stringResource(R.string.address_permission_allow), onClick = onAllowClick)
            OutlinedButton(onClick = onDeclineClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.address_permission_decline))
            }
        }
    }
}

@Composable
private fun SearchSection(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        FormField(
            label = stringResource(R.string.address_search_label),
            value = query,
            onValueChange = onQueryChange,
        )
        OutlinedButton(
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSearching && query.isNotBlank(),
        ) {
            Text(
                text =
                    stringResource(
                        if (isSearching) R.string.address_searching else R.string.address_search_action,
                    ),
            )
        }
    }
}

@Composable
private fun SaveSection(
    status: SaveStatus,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val savingDescription = stringResource(R.string.cd_loading)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (status is SaveStatus.Saved) {
            Text(
                text = stringResource(R.string.address_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val generalError = (status as? SaveStatus.Failed)?.error?.takeIf { !it.isAboutAField() }
        if (generalError != null) {
            Text(
                text = stringResource(generalError.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (status is SaveStatus.Saving) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = savingDescription },
            )
        } else {
            PrimaryButton(text = stringResource(R.string.address_save), onClick = onSaveClick)
        }
    }
}

private val LOCATION_PERMISSIONS =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

private fun previewContent(
    point: Coordinate? = MapDefaults.initialPosition,
    permission: LocationPermissionState = LocationPermissionState.Granted,
    status: SaveStatus = SaveStatus.Idle,
    notice: AddressNotice? = null,
    isPrimary: Boolean = true,
): AddressUiState.Content =
    AddressUiState.Content(
        addressId = "6b1f1f2e-0000-4000-8000-000000000000",
        form =
            AddressForm(
                alias = "Casa",
                addressText = "Avenida Arce 2081, La Paz",
                reference = "Portón verde, al lado de la farmacia",
                city = "La Paz",
            ),
        point = point,
        camera = MapCamera(MapDefaults.initialPosition, 1),
        permission = permission,
        status = status,
        notice = notice,
        isLocating = false,
        isSearching = false,
        isPrimary = isPrimary,
        query = "",
    )

@Composable
private fun PreviewMap(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {}
}

@Preview(showBackground = true, heightDp = 1100, name = "Direccion guardada, claro")
@Composable
private fun AddressContentLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressContent(previewContent(), {}, {}, {}, {}, {}, {}, {}) { _, modifier -> PreviewMap(modifier) }
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "Direccion guardada, oscuro")
@Composable
private fun AddressContentDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        AddressContent(previewContent(), {}, {}, {}, {}, {}, {}, {}) { _, modifier -> PreviewMap(modifier) }
    }
}

@Preview(showBackground = true, heightDp = 1300, name = "Primera direccion, pidiendo el permiso")
@Composable
private fun AddressContentFirstTimePreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressContent(
            previewContent(
                point = null,
                permission = LocationPermissionState.NotRequested,
                isPrimary = false,
            ),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        ) { _, modifier -> PreviewMap(modifier) }
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "Permiso denegado y busqueda sin resultado")
@Composable
private fun AddressContentDeniedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressContent(
            previewContent(
                permission = LocationPermissionState.Denied,
                notice = AddressNotice.PlaceNotFound,
            ),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        ) { _, modifier -> PreviewMap(modifier) }
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "Guardado sin punto marcado")
@Composable
private fun AddressContentWithoutPointPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressContent(
            previewContent(
                point = null,
                permission = LocationPermissionState.Denied,
                status = SaveStatus.Failed(AddressError.PointNotChosen),
                isPrimary = false,
            ),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        ) { _, modifier -> PreviewMap(modifier) }
    }
}

@Preview(showBackground = true, name = "Direccion cargando")
@Composable
private fun AddressLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AddressContent(AddressUiState.Loading, {}, {}, {}, {}, {}, {}, {}) { _, modifier -> PreviewMap(modifier) }
    }
}

@Preview(showBackground = true, name = "Direccion que no se pudo leer")
@Composable
private fun AddressFailedPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        AddressContent(
            AddressUiState.Failed(AddressError.NetworkUnavailable),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        ) { _, modifier -> PreviewMap(modifier) }
    }
}
