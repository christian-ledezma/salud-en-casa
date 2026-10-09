package bo.saludencasa.features.catalog.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.DeclaredServices
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

@Composable
fun MyServicesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyServicesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyServicesContent(
        uiState = uiState,
        onBack = onBack,
        onRetryClick = viewModel::load,
        onDeclareClick = viewModel::onDeclareClick,
        onRepriceClick = viewModel::onRepriceClick,
        onRemoveClick = viewModel::onRemoveClick,
        onPriceChanged = viewModel::onPriceChanged,
        onConfirmPrice = viewModel::onConfirmPrice,
        onConfirmRemoval = viewModel::onConfirmRemoval,
        onDismissEditor = viewModel::onDismissEditor,
        modifier = modifier,
    )
}

@Composable
internal fun MyServicesContent(
    uiState: MyServicesUiState,
    onBack: () -> Unit,
    onRetryClick: () -> Unit,
    onDeclareClick: (ServiceType) -> Unit,
    onRepriceClick: (ProfessionalService) -> Unit,
    onRemoveClick: (ProfessionalService) -> Unit,
    onPriceChanged: (String) -> Unit,
    onConfirmPrice: () -> Unit,
    onConfirmRemoval: () -> Unit,
    onDismissEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        MyServicesUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        is MyServicesUiState.Failed -> {
            CenteredMessage(modifier = modifier) {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetryClick)
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        }

        is MyServicesUiState.Content -> {
            ServiceSections(
                content = uiState,
                onBack = onBack,
                onDeclareClick = onDeclareClick,
                onRepriceClick = onRepriceClick,
                onRemoveClick = onRemoveClick,
                modifier = modifier,
            )

            uiState.editor?.let { editor ->
                when (editor) {
                    is ServiceEditor.Declaring -> {
                        PriceDialog(
                            serviceTypeName = editor.type.name,
                            price = editor.price,
                            error = editor.error,
                            busy = uiState.busy,
                            onPriceChanged = onPriceChanged,
                            onConfirm = onConfirmPrice,
                            onDismiss = onDismissEditor,
                        )
                    }

                    is ServiceEditor.Repricing -> {
                        PriceDialog(
                            serviceTypeName = editor.service.type.name,
                            price = editor.price,
                            error = editor.error,
                            busy = uiState.busy,
                            onPriceChanged = onPriceChanged,
                            onConfirm = onConfirmPrice,
                            onDismiss = onDismissEditor,
                        )
                    }

                    is ServiceEditor.Removing -> {
                        RemovalDialog(
                            serviceTypeName = editor.service.type.name,
                            busy = uiState.busy,
                            onConfirm = onConfirmRemoval,
                            onDismiss = onDismissEditor,
                        )
                    }
                }
            }
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

// Both sections live in the same list: the catalog is what the professional
// declares from, so putting it behind a dialog would hide twelve rows behind a
// container that clips instead of scrolling at a 200 % font scale.
@Composable
private fun ServiceSections(
    content: MyServicesUiState.Content,
    onBack: () -> Unit,
    onDeclareClick: (ServiceType) -> Unit,
    onRepriceClick: (ProfessionalService) -> Unit,
    onRemoveClick: (ProfessionalService) -> Unit,
    modifier: Modifier = Modifier,
) {
    val declared = content.declaredServices

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                ScreenHeader(
                    title = stringResource(R.string.catalog_my_services_title),
                    onBack = onBack,
                    subtitle = stringResource(R.string.catalog_my_services_subtitle),
                )
                content.notice?.let { error ->
                    Text(
                        text = stringResource(error.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item(key = "declared-title") {
            SectionTitle(text = stringResource(R.string.catalog_declared_section_title))
        }

        if (declared.services.isEmpty()) {
            item(key = "declared-empty") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                    Text(
                        text = stringResource(R.string.catalog_declared_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.catalog_declared_empty_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(declared.services, key = { it.id }) { service ->
            DeclaredServiceCard(
                service = service,
                busy = content.busy,
                onRepriceClick = { onRepriceClick(service) },
                onRemoveClick = { onRemoveClick(service) },
            )
        }

        item(key = "available-title") {
            SectionTitle(text = stringResource(R.string.catalog_available_section_title))
        }

        if (declared.undeclaredTypes.isEmpty()) {
            item(key = "available-empty") {
                Text(
                    text = stringResource(R.string.catalog_available_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(declared.undeclaredTypes, key = { "type:${it.id}" }) { type ->
            ServiceTypeCard(
                type = type,
                busy = content.busy,
                onDeclareClick = { onDeclareClick(type) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun DeclaredServiceCard(
    service: ProfessionalService,
    busy: Boolean,
    onRepriceClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]

    ServiceCard(
        name = service.type.name,
        supporting = stringResource(R.string.catalog_my_price, formatBob(service.referencePriceBob, locale)),
        estimatedDurationMin = service.type.estimatedDurationMin,
    ) {
        TextButton(onClick = onRepriceClick, enabled = !busy) {
            Text(text = stringResource(R.string.catalog_reprice_action))
        }
        TextButton(onClick = onRemoveClick, enabled = !busy) {
            Text(text = stringResource(R.string.catalog_remove_action))
        }
    }
}

@Composable
private fun ServiceTypeCard(
    type: ServiceType,
    busy: Boolean,
    onDeclareClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]

    ServiceCard(
        name = type.name,
        supporting = stringResource(R.string.catalog_reference_price, formatBob(type.referencePriceBob, locale)),
        estimatedDurationMin = type.estimatedDurationMin,
        description = type.description,
    ) {
        TextButton(onClick = onDeclareClick, enabled = !busy) {
            Text(text = stringResource(R.string.catalog_declare_action))
        }
    }
}

@Composable
private fun ServiceCard(
    name: String,
    supporting: String,
    estimatedDurationMin: Int,
    description: String? = null,
    actions: @Composable FlowRowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)

            description?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            Text(text = supporting, style = MaterialTheme.typography.bodyLarge)

            Text(
                text =
                    pluralStringResource(
                        R.plurals.catalog_estimated_duration,
                        estimatedDurationMin,
                        estimatedDurationMin,
                    ),
                style = MaterialTheme.typography.bodySmall,
            )

            // FlowRow and not Row, so a third action wraps instead of pushing
            // the others out. No test holds this: swapping it for a Row was
            // tried and passed, because the label clips inside its own button
            // (docs/decisions.md, 2026-10-08, what the screen tests miss).
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                content = actions,
            )
        }
    }
}

@Composable
private fun PriceDialog(
    serviceTypeName: String,
    price: String,
    error: CatalogError?,
    busy: Boolean,
    onPriceChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.catalog_price_dialog_title, serviceTypeName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                Text(
                    text = stringResource(R.string.catalog_price_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FormField(
                    label = stringResource(R.string.catalog_price_label),
                    value = price,
                    onValueChange = onPriceChanged,
                    errorText = error?.let { stringResource(it.messageRes()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDescription },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(text = stringResource(R.string.catalog_price_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun RemovalDialog(
    serviceTypeName: String,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.catalog_remove_dialog_title)) },
        text = { Text(text = stringResource(R.string.catalog_remove_dialog_message, serviceTypeName)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(text = stringResource(R.string.catalog_remove_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

private fun previewType(
    id: String,
    name: String,
    price: String,
    durationMin: Int,
    description: String?,
): ServiceType =
    ServiceType(
        id = id,
        name = name,
        description = description,
        referencePriceBob = BigDecimal(price),
        estimatedDurationMin = durationMin,
    )

private val consultation =
    previewType(
        id = "1",
        name = "Consulta médica general",
        price = "150.00",
        durationMin = 45,
        description = "Valoración médica en domicilio, anamnesis y examen físico.",
    )

private val physiotherapy =
    previewType(
        id = "2",
        name = "Terapia física y rehabilitación",
        price = "130.00",
        durationMin = 60,
        description = "Sesión de fisioterapia en domicilio según plan de tratamiento.",
    )

private val elderCare =
    previewType(
        id = "3",
        name = "Cuidado de adulto mayor",
        price = "200.00",
        durationMin = 240,
        description = "Acompañamiento, higiene, movilización y control de medicación.",
    )

private fun previewContent(
    declaredTypes: List<ServiceType> = listOf(consultation),
    editor: ServiceEditor? = null,
    busy: Boolean = false,
    notice: CatalogError? = null,
): MyServicesUiState.Content =
    MyServicesUiState.Content(
        declaredServices =
            DeclaredServices(
                services =
                    declaredTypes.mapIndexed { index, type ->
                        ProfessionalService(
                            id = "declared-$index",
                            type = type,
                            referencePriceBob = type.referencePriceBob,
                        )
                    },
                catalog = listOf(consultation, physiotherapy, elderCare),
            ),
        editor = editor,
        busy = busy,
        notice = notice,
    )

@Composable
private fun PreviewScreen(uiState: MyServicesUiState) {
    MyServicesContent(
        uiState = uiState,
        onBack = {},
        onRetryClick = {},
        onDeclareClick = {},
        onRepriceClick = {},
        onRemoveClick = {},
        onPriceChanged = {},
        onConfirmPrice = {},
        onConfirmRemoval = {},
        onDismissEditor = {},
    )
}

@Preview(showBackground = true, heightDp = 900, name = "Mis servicios, claro")
@Composable
private fun MyServicesLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent())
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Mis servicios, oscuro")
@Composable
private fun MyServicesDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PreviewScreen(previewContent(declaredTypes = listOf(consultation, physiotherapy)))
    }
}

@Preview(showBackground = true, heightDp = 1800, fontScale = 2f, name = "Mis servicios al 200 %")
@Composable
private fun MyServicesLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent())
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Sin servicios declarados")
@Composable
private fun MyServicesEmptyPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent(declaredTypes = emptyList()))
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Catalogo completo declarado")
@Composable
private fun MyServicesAllDeclaredPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent(declaredTypes = listOf(consultation, physiotherapy, elderCare)))
    }
}

@Preview(showBackground = true, heightDp = 600, name = "Fijando el precio")
@Composable
private fun MyServicesPricingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(
            previewContent(
                editor = ServiceEditor.Declaring(physiotherapy, price = "130", error = null),
            ),
        )
    }
}

@Preview(showBackground = true, heightDp = 800, fontScale = 2f, name = "Precio rechazado al 200 %")
@Composable
private fun MyServicesPricingErrorLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(
            previewContent(
                editor =
                    ServiceEditor.Declaring(
                        physiotherapy,
                        price = "0",
                        error = CatalogError.InvalidPrice,
                    ),
            ),
        )
    }
}

@Preview(showBackground = true, heightDp = 500, name = "Confirmando quitar un servicio")
@Composable
private fun MyServicesRemovingPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        val content = previewContent()
        PreviewScreen(
            content.copy(editor = ServiceEditor.Removing(content.declaredServices.services.first())),
        )
    }
}

@Preview(showBackground = true, name = "Mis servicios cargando")
@Composable
private fun MyServicesLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(MyServicesUiState.Loading)
    }
}

@Preview(showBackground = true, name = "Mis servicios no se pudieron leer")
@Composable
private fun MyServicesFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(MyServicesUiState.Failed(CatalogError.NetworkUnavailable))
    }
}
