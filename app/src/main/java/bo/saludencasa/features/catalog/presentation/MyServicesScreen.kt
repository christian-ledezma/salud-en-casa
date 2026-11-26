package bo.saludencasa.features.catalog.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.DeclaredServices
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.GroupedRow
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.components.SearchField
import bo.saludencasa.ui.components.StatusBadge
import bo.saludencasa.ui.components.StatusTone
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

internal class MyServicesActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onQueryChanged: (String) -> Unit = {},
    val onAddServices: () -> Unit = {},
    val onPickerQueryChanged: (String) -> Unit = {},
    val onPickerToggle: (String) -> Unit = {},
    val onPickerDismiss: () -> Unit = {},
    val onConfirmDeclaration: () -> Unit = {},
    val onDeclarationNoticeShown: () -> Unit = {},
    val onReprice: (ProfessionalService) -> Unit = {},
    val onRemove: (ProfessionalService) -> Unit = {},
    val onPriceChanged: (String) -> Unit = {},
    val onConfirmPrice: () -> Unit = {},
    val onConfirmRemoval: () -> Unit = {},
    val onDismissEditor: () -> Unit = {},
)

@Composable
fun MyServicesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyServicesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onBack) {
            MyServicesActions(
                onBack = onBack,
                onRetry = viewModel::load,
                onQueryChanged = viewModel::onQueryChanged,
                onAddServices = viewModel::onAddServicesClick,
                onPickerQueryChanged = viewModel::onPickerQueryChanged,
                onPickerToggle = viewModel::onPickerToggle,
                onPickerDismiss = viewModel::onPickerDismiss,
                onConfirmDeclaration = viewModel::onConfirmDeclaration,
                onDeclarationNoticeShown = viewModel::onDeclarationNoticeShown,
                onReprice = viewModel::onRepriceClick,
                onRemove = viewModel::onRemoveClick,
                onPriceChanged = viewModel::onPriceChanged,
                onConfirmPrice = viewModel::onConfirmPrice,
                onConfirmRemoval = viewModel::onConfirmRemoval,
                onDismissEditor = viewModel::onDismissEditor,
            )
        }

    MyServicesContent(uiState = uiState, actions = actions, modifier = modifier)
}

// initiallyExpandedId exists so a preview can show a row open.
@Composable
internal fun MyServicesContent(
    uiState: MyServicesUiState,
    actions: MyServicesActions,
    modifier: Modifier = Modifier,
    initiallyExpandedId: String? = null,
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
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                OutlinedButton(onClick = actions.onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        }

        is MyServicesUiState.Content -> {
            LoadedServices(uiState, actions, modifier, initiallyExpandedId)
        }
    }
}

@Composable
private fun LoadedServices(
    content: MyServicesUiState.Content,
    actions: MyServicesActions,
    modifier: Modifier,
    initiallyExpandedId: String?,
) {
    // Both live above the switch between the list and the selector, so the
    // list comes back where it was and with the same row open.
    val listState = rememberLazyListState()
    var expandedId by rememberSaveable { mutableStateOf(initiallyExpandedId) }
    val picker = content.picker

    BackHandler(enabled = picker != null, onBack = actions.onPickerDismiss)

    if (picker != null) {
        ServicePickerContent(content = content, picker = picker, actions = actions, modifier = modifier)
    } else {
        ServicesListContent(
            content = content,
            actions = actions,
            listState = listState,
            expandedId = expandedId,
            onExpandedChange = { expandedId = it },
            modifier = modifier,
        )
    }

    content.editor?.let { editor ->
        when (editor) {
            is ServiceEditor.Repricing -> {
                PriceDialog(
                    serviceTypeName = editor.service.type.name,
                    price = editor.price,
                    error = editor.error,
                    busy = content.busy,
                    onPriceChanged = actions.onPriceChanged,
                    onConfirm = actions.onConfirmPrice,
                    onDismiss = actions.onDismissEditor,
                )
            }

            is ServiceEditor.Removing -> {
                RemovalDialog(
                    serviceTypeName = editor.service.type.name,
                    busy = content.busy,
                    onConfirm = actions.onConfirmRemoval,
                    onDismiss = actions.onDismissEditor,
                )
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

// What the professional offers, in compact rows, with the catalog one button
// away (docs/decisions.md, 2026-11-25, HT-32). The button is pinned: with the
// list long, it must not be below the last row.
@Composable
private fun ServicesListContent(
    content: MyServicesUiState.Content,
    actions: MyServicesActions,
    listState: LazyListState,
    expandedId: String?,
    onExpandedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val declared = content.declaredServices
    val snackbarHostState = remember { SnackbarHostState() }
    val noticeCount = content.declarationNotice
    val noticeMessage = noticeCount?.let { pluralStringResource(R.plurals.catalog_declared_notice, it, it) }

    LaunchedEffect(noticeCount) {
        if (noticeMessage != null) {
            snackbarHostState.showSnackbar(noticeMessage)
            actions.onDeclarationNoticeShown()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.scale8),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                    ScreenHeader(
                        title = stringResource(R.string.catalog_my_services_title),
                        onBack = actions.onBack,
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

            item(key = "summary") {
                Text(
                    text =
                        stringResource(
                            R.string.catalog_summary,
                            declared.services.size,
                            declared.undeclaredTypes.size,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.scale16, bottom = Spacing.scale8),
                )
            }

            if (content.showsSearch) {
                item(key = "search") {
                    SearchField(
                        value = content.query,
                        onValueChange = actions.onQueryChanged,
                        placeholder = stringResource(R.string.catalog_search_declared_placeholder),
                        clearDescription = stringResource(R.string.cd_catalog_clear_search),
                        modifier = Modifier.padding(bottom = Spacing.scale12),
                    )
                }
            }

            if (declared.services.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier.padding(vertical = Spacing.scale16),
                        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                    ) {
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
            } else if (content.visibleServices.isEmpty()) {
                item(key = "no-match") {
                    Text(
                        text = stringResource(R.string.catalog_no_declared_match, content.query.trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.scale16),
                    )
                }
            }

            itemsIndexed(content.visibleServices, key = { _, service -> service.id }) { index, service ->
                DeclaredServiceRow(
                    service = service,
                    index = index,
                    count = content.visibleServices.size,
                    expanded = expandedId == service.id,
                    isNew = service.type.id in content.newTypeIds,
                    busy = content.busy,
                    onToggle = { onExpandedChange(if (expandedId == service.id) null else service.id) },
                    onPriceClick = { actions.onReprice(service) },
                    onRemoveClick = { actions.onRemove(service) },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale8),
        )

        BottomActionBar {
            PrimaryButton(
                text = stringResource(R.string.catalog_add_services_action),
                onClick = actions.onAddServices,
                enabled = !content.busy,
                leadingIcon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun DeclaredServiceRow(
    service: ProfessionalService,
    index: Int,
    count: Int,
    expanded: Boolean,
    isNew: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onPriceClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val price = formatBob(service.referencePriceBob, locale)
    val stateDescription =
        stringResource(if (expanded) R.string.cd_catalog_service_expanded else R.string.cd_catalog_service_collapsed)
    val changePriceDescription = stringResource(R.string.cd_catalog_change_price, service.type.name, price)

    GroupedRow(index = index, count = count) {
        // The name takes what the price leaves; when the font is large enough
        // that both do not fit, the price goes under the name and the name is
        // not squeezed. No test holds this: the label clips inside its own
        // container (docs/decisions.md, 2026-10-08, what the screen tests miss).
        FlowRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.cardPadding, vertical = Spacing.scale12),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.minTouchTarget)
                        .clickable(role = Role.Button, onClick = onToggle)
                        .semantics { this.stateDescription = stateDescription },
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = service.type.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = durationLabel(service.type.estimatedDurationMin),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isNew) {
                        StatusBadge(label = stringResource(R.string.catalog_new_badge), tone = StatusTone.OPEN)
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                }
            }

            FilledTonalButton(
                onClick = onPriceClick,
                enabled = !busy,
                modifier =
                    Modifier
                        .heightIn(min = Spacing.minTouchTarget)
                        .semantics { contentDescription = changePriceDescription },
                shape = ExtraShapes.pill,
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            ) {
                Text(text = price, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.width(Spacing.scale8))
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }
        }

        if (expanded) {
            Column(
                modifier =
                    Modifier.padding(
                        start = Spacing.cardPadding,
                        end = Spacing.cardPadding,
                        bottom = Spacing.scale8,
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                service.type.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // The button's own padding would push its text in from the
                // description above it.
                TextButton(onClick = onRemoveClick, enabled = !busy, modifier = Modifier.offset(x = -Spacing.scale12)) {
                    Text(
                        text = stringResource(R.string.catalog_remove_service_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
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
    val scroll = rememberScrollState()
    // The refusal sits under the field, below the fold at a large font.
    LaunchedEffect(error) {
        if (error != null) scroll.animateScrollTo(scroll.maxValue)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.catalog_price_dialog_title, serviceTypeName)) },
        text = {
            // Scrolls: at 200 % font size the text and the field no longer fit a small phone.
            Column(
                modifier = Modifier.verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
            ) {
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

private val injections =
    previewType(
        "1",
        "Aplicación de inyectables",
        "50.00",
        15,
        "Administración de medicación intramuscular o subcutánea indicada por receta.",
    )
private val venousLine =
    previewType(
        "2",
        "Colocación y control de vía venosa",
        "120.00",
        60,
        "Canalización de vía periférica y administración de suero o medicación.",
    )
private val vitalSigns =
    previewType(
        "3",
        "Control de signos vitales",
        "60.00",
        20,
        "Medición de presión arterial, frecuencia cardíaca, temperatura y saturación.",
    )
private val woundCare =
    previewType(
        "4",
        "Curación de heridas",
        "100.00",
        40,
        "Limpieza, desinfección y cobertura de heridas o úlceras por presión.",
    )
private val consultation =
    previewType(
        "5",
        "Consulta médica general",
        "150.00",
        45,
        "Valoración médica en domicilio, anamnesis y examen físico.",
    )
private val prenatal =
    previewType(
        "6",
        "Control prenatal y posparto",
        "160.00",
        45,
        "Control de embarazo o puerperio y orientación a la madre.",
    )
private val elderCare =
    previewType(
        "7",
        "Cuidado de adulto mayor",
        "200.00",
        240,
        "Acompañamiento, higiene, movilización y control de medicación.",
    )
private val postoperative =
    previewType(
        "8",
        "Cuidado postoperatorio",
        "180.00",
        60,
        "Control de evolución, curación y manejo del dolor tras una cirugía.",
    )
private val catheter =
    previewType("9", "Sondaje vesical", "140.00", 30, "Colocación, cambio o retiro de sonda vesical.")
private val physiotherapy =
    previewType(
        "10",
        "Terapia física y rehabilitación",
        "130.00",
        60,
        "Sesión de fisioterapia en domicilio según plan de tratamiento.",
    )
private val respiratory =
    previewType(
        "11",
        "Terapia respiratoria",
        "110.00",
        45,
        "Nebulizaciones, ejercicios respiratorios y manejo de secreciones.",
    )
private val samples =
    previewType(
        "12",
        "Toma de muestras de laboratorio",
        "80.00",
        20,
        "Extracción de sangre y recolección de muestras para análisis.",
    )

private val previewCatalog =
    listOf(
        injections,
        venousLine,
        vitalSigns,
        woundCare,
        consultation,
        prenatal,
        elderCare,
        postoperative,
        catheter,
        physiotherapy,
        respiratory,
        samples,
    )

private val previewDeclared = listOf(injections, venousLine, vitalSigns, woundCare)

private fun previewContent(
    declaredTypes: List<ServiceType> = previewDeclared,
    editor: ServiceEditor? = null,
    picker: ServicePicker? = null,
    query: String = "",
    newTypeIds: Set<String> = emptySet(),
    busy: Boolean = false,
    notice: CatalogError? = null,
): MyServicesUiState.Content =
    MyServicesUiState.Content(
        declaredServices =
            DeclaredServices(
                services =
                    declaredTypes
                        .mapIndexed { index, type ->
                            ProfessionalService(
                                id = "declared-$index",
                                type = type,
                                referencePriceBob = type.referencePriceBob,
                            )
                        }.sortedBy { it.type.name },
                catalog = previewCatalog,
            ),
        editor = editor,
        picker = picker,
        query = query,
        newTypeIds = newTypeIds,
        busy = busy,
        notice = notice,
    )

@Composable
private fun PreviewScreen(
    uiState: MyServicesUiState,
    initiallyExpandedId: String? = null,
) {
    // The app paints the theme background with its Scaffold; a preview has
    // none, and in the dark scheme the light text would sit on white.
    Surface(color = MaterialTheme.colorScheme.background) {
        MyServicesContent(uiState = uiState, actions = MyServicesActions(), initiallyExpandedId = initiallyExpandedId)
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Mis servicios, claro")
@Composable
private fun MyServicesLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent(newTypeIds = setOf(vitalSigns.id)), initiallyExpandedId = "declared-2")
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Mis servicios, oscuro")
@Composable
private fun MyServicesDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PreviewScreen(previewContent(newTypeIds = setOf(vitalSigns.id)), initiallyExpandedId = "declared-2")
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1700, fontScale = 2f, name = "Mis servicios al 200 %")
@Composable
private fun MyServicesLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent(newTypeIds = setOf(vitalSigns.id)), initiallyExpandedId = "declared-2")
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Sin servicios declarados")
@Composable
private fun MyServicesEmptyPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent(declaredTypes = emptyList()))
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Con búsqueda, más de ocho declarados")
@Composable
private fun MyServicesSearchPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(
            previewContent(
                declaredTypes = previewCatalog.take(9),
                query = "cuid",
            ),
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Selector, claro")
@Composable
private fun ServicePickerLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(
            previewContent(
                declaredTypes = listOf(injections, venousLine, vitalSigns, woundCare),
                picker = ServicePicker(selected = setOf(consultation.id, elderCare.id)),
            ),
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Selector, oscuro, con búsqueda")
@Composable
private fun ServicePickerDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PreviewScreen(previewContent(picker = ServicePicker(query = "cui")))
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1700, fontScale = 2f, name = "Selector al 200 %")
@Composable
private fun ServicePickerLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent(picker = ServicePicker(selected = setOf(consultation.id, elderCare.id))))
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Selector sin resultados")
@Composable
private fun ServicePickerNoResultsPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(previewContent(picker = ServicePicker(query = "dentista")))
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Selector con un tipo ya declarado")
@Composable
private fun ServicePickerRefusedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PreviewScreen(
            previewContent(
                picker = ServicePicker(selected = setOf(consultation.id), error = CatalogError.ServiceAlreadyDeclared),
            ),
        )
    }
}

@Preview(showBackground = true, heightDp = 600, name = "Cambiando un precio")
@Composable
private fun MyServicesPricingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val content = previewContent()
        PreviewScreen(
            content.copy(
                editor = ServiceEditor.Repricing(content.declaredServices.services.first(), price = "50", error = null),
            ),
        )
    }
}

@Preview(showBackground = true, heightDp = 800, widthDp = 320, fontScale = 2f, name = "Precio rechazado al 200 %")
@Composable
private fun MyServicesPricingErrorLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val content = previewContent()
        PreviewScreen(
            content.copy(
                editor =
                    ServiceEditor.Repricing(
                        content.declaredServices.services.first(),
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
        PreviewScreen(content.copy(editor = ServiceEditor.Removing(content.declaredServices.services.first())))
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
