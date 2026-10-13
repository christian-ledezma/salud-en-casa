package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.RadioOptionGroup
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

@Composable
fun CreateRequestScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onRegisterAddress: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateRequestViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is CreateRequestUiState.Submitted) onCreated()
    }

    CreateRequestContent(
        uiState = uiState,
        onBack = onBack,
        actions =
            CreateRequestActions(
                onServiceTypeSelected = viewModel::onServiceTypeSelected,
                onAddressSelected = viewModel::onAddressSelected,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onBudgetChanged = viewModel::onBudgetChanged,
                onSubmit = viewModel::onSubmit,
                onRetry = viewModel::load,
                onRegisterAddress = onRegisterAddress,
            ),
        modifier = modifier,
    )
}

internal data class CreateRequestActions(
    val onServiceTypeSelected: (String) -> Unit = {},
    val onAddressSelected: (String) -> Unit = {},
    val onDescriptionChanged: (String) -> Unit = {},
    val onBudgetChanged: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRegisterAddress: () -> Unit = {},
)

@Composable
internal fun CreateRequestContent(
    uiState: CreateRequestUiState,
    onBack: () -> Unit,
    actions: CreateRequestActions,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        CreateRequestUiState.Loading, CreateRequestUiState.Submitted -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        CreateRequestUiState.NoAddress -> {
            RequestColumn(modifier = modifier) {
                ScreenHeader(title = stringResource(R.string.request_create_title), onBack = onBack)
                Text(
                    text = stringResource(R.string.request_create_no_address_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.request_create_no_address_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text = stringResource(R.string.request_create_no_address_action),
                    onClick = actions.onRegisterAddress,
                )
            }
        }

        is CreateRequestUiState.Failed -> {
            RequestColumn(modifier = modifier) {
                ScreenHeader(title = stringResource(R.string.request_create_title), onBack = onBack)
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
            }
        }

        is CreateRequestUiState.Content -> {
            RequestColumn(modifier = modifier) {
                ScreenHeader(
                    title = stringResource(R.string.request_create_title),
                    onBack = onBack,
                    subtitle = stringResource(R.string.request_create_subtitle),
                )
                RadioOptionGroup(
                    label = stringResource(R.string.request_create_service_type_label),
                    options = uiState.serviceTypes,
                    selected = uiState.serviceTypes.firstOrNull { it.id == uiState.selectedServiceTypeId },
                    onSelectedChange = { actions.onServiceTypeSelected(it.id) },
                    optionLabel = ServiceType::name,
                )
                RadioOptionGroup(
                    label = stringResource(R.string.request_create_address_label),
                    options = uiState.addresses,
                    selected = uiState.addresses.firstOrNull { it.id == uiState.selectedAddressId },
                    onSelectedChange = { actions.onAddressSelected(it.id) },
                    optionLabel = Address::alias,
                )
                FormField(
                    label = stringResource(R.string.request_create_description_label),
                    value = uiState.description,
                    onValueChange = actions.onDescriptionChanged,
                    singleLine = false,
                    errorText =
                        stringResource(R.string.error_request_invalid_description)
                            .takeIf { uiState.submitError == RequestError.InvalidDescription },
                )
                FormField(
                    label = stringResource(R.string.request_create_budget_label),
                    value = uiState.budget,
                    onValueChange = actions.onBudgetChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    errorText =
                        stringResource(R.string.error_request_invalid_budget)
                            .takeIf { uiState.submitError == RequestError.InvalidBudget },
                )
                uiState.submitError
                    ?.takeUnless { it == RequestError.InvalidDescription || it == RequestError.InvalidBudget }
                    ?.let { error ->
                        Text(
                            text = stringResource(error.messageRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                PrimaryButton(
                    text = stringResource(R.string.request_create_submit),
                    onClick = actions.onSubmit,
                    enabled = uiState.canSubmit,
                )
            }
        }
    }
}

@Composable
private fun RequestColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        content()
    }
}

private fun previewServiceTypes(): List<ServiceType> =
    listOf(
        ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60),
        ServiceType("t2", "Vacunación", null, BigDecimal("90.00"), 20),
    )

private fun previewAddresses(): List<Address> =
    listOf(
        Address(
            id = "a1",
            alias = "Mi casa",
            addressText = "Avenida América 1234, Cochabamba",
            reference = null,
            city = "Cochabamba",
            coordinate = Coordinate.create(-17.3835, -66.1568).getOrThrow(),
            isPrimary = true,
            isProfessionalBase = false,
        ),
    )

private fun previewContent(): CreateRequestUiState.Content =
    CreateRequestUiState.Content(
        serviceTypes = previewServiceTypes(),
        addresses = previewAddresses(),
        selectedServiceTypeId = "t1",
        selectedAddressId = "a1",
        description = "",
        budget = "",
        submitting = false,
        submitError = null,
    )

@Preview(showBackground = true, widthDp = 320, name = "Crear solicitud, claro")
@Composable
private fun CreateRequestLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        CreateRequestContent(previewContent(), onBack = {}, actions = CreateRequestActions())
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Crear solicitud, oscuro")
@Composable
private fun CreateRequestDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        CreateRequestContent(previewContent(), onBack = {}, actions = CreateRequestActions())
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f, name = "Crear solicitud, fuente 200%")
@Composable
private fun CreateRequestLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        CreateRequestContent(previewContent(), onBack = {}, actions = CreateRequestActions())
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Crear solicitud sin dirección")
@Composable
private fun CreateRequestNoAddressPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        CreateRequestContent(CreateRequestUiState.NoAddress, onBack = {}, actions = CreateRequestActions())
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Crear solicitud con error")
@Composable
private fun CreateRequestFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        CreateRequestContent(
            CreateRequestUiState.Failed(RequestError.NetworkUnavailable),
            onBack = {},
            actions = CreateRequestActions(),
        )
    }
}
