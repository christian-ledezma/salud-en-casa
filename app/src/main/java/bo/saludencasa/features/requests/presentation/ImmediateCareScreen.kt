package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.AppointmentZone
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.notifications.presentation.NotificationPermissionPrompt
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import bo.saludencasa.features.search.domain.model.SearchRadius
import bo.saludencasa.features.search.presentation.NearbyProfessionalCard
import bo.saludencasa.features.search.presentation.ProfessionalSearchMap
import bo.saludencasa.features.search.presentation.SearchMapModel
import bo.saludencasa.ui.animations.RadarPulse
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.ExpandableOptionPicker
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.RadioOptionGroup
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.Instant

// docs/decisions.md, 2026-10-23, HT-23: the map, the form and the wait for
// offers on one screen.
@Composable
fun ImmediateCareScreen(
    onBack: () -> Unit,
    onOpenProfessional: (String) -> Unit,
    onOpenMyRequests: () -> Unit,
    onRegisterAddress: () -> Unit,
    onOpenThread: (requestId: String, professionalId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImmediateCareViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    ImmediateCareContent(
        uiState = uiState,
        onBack = onBack,
        actions =
            ImmediateCareActions(
                onTypeListToggle = viewModel::onTypeListToggle,
                onServiceTypeSelected = viewModel::onServiceTypeSelected,
                onAddressSelected = viewModel::onAddressSelected,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onBudgetChanged = viewModel::onBudgetChanged,
                onSubmit = viewModel::onSubmit,
                onCancel = viewModel::onCancel,
                onRetry = viewModel::refresh,
                onRegisterAddress = onRegisterAddress,
                onOpenProfessional = onOpenProfessional,
                onOpenMyRequests = onOpenMyRequests,
                onOpenThread = onOpenThread,
            ),
        modifier = modifier,
        map = { model, mapModifier ->
            ProfessionalSearchMap(
                model = model,
                onProfessionalClick = viewModel::onProfessionalSelected,
                modifier = mapModifier,
            )
        },
        // docs/decisions.md, 2026-10-16: the patient is asked after publishing.
        permissionPrompt = { NotificationPermissionPrompt(body = R.string.notification_permission_body_patient) },
    )
}

internal data class ImmediateCareActions(
    val onTypeListToggle: () -> Unit = {},
    val onServiceTypeSelected: (String) -> Unit = {},
    val onAddressSelected: (String) -> Unit = {},
    val onDescriptionChanged: (String) -> Unit = {},
    val onBudgetChanged: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRegisterAddress: () -> Unit = {},
    val onOpenProfessional: (String) -> Unit = {},
    val onOpenMyRequests: () -> Unit = {},
    val onOpenThread: (requestId: String, professionalId: String) -> Unit = { _, _ -> },
)

// The map is a slot, so the preview and the Robolectric test never reach the
// Maps SDK, which draws nothing without a key and Play Services (HU-05).
@Composable
internal fun ImmediateCareContent(
    uiState: ImmediateCareUiState,
    onBack: () -> Unit,
    actions: ImmediateCareActions,
    modifier: Modifier = Modifier,
    map: @Composable (SearchMapModel, Modifier) -> Unit = { _, _ -> },
    permissionPrompt: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            ImmediateCareUiState.Loading -> {
                CenteredProgress()
            }

            ImmediateCareUiState.NoAddress -> {
                MessageColumn(onBack = onBack) {
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

            is ImmediateCareUiState.Failed -> {
                MessageColumn(onBack = onBack) {
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                }
            }

            is ImmediateCareUiState.Form -> {
                ImmediateForm(form = uiState, onBack = onBack, actions = actions, map = map)
            }

            is ImmediateCareUiState.Waiting -> {
                WaitingPanel(
                    waiting = uiState,
                    onBack = onBack,
                    actions = actions,
                    permissionPrompt = permissionPrompt,
                )
            }
        }
    }
}

@Composable
private fun ImmediateForm(
    form: ImmediateCareUiState.Form,
    onBack: () -> Unit,
    actions: ImmediateCareActions,
    map: @Composable (SearchMapModel, Modifier) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val margin = Modifier.padding(horizontal = Spacing.screenMargin)
    val nearby = (form.nearby as? NearbyState.Found)?.professionals.orEmpty()
    val selected = nearby.firstOrNull { it.professionalId == form.selectedProfessionalId }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        ) {
            ScreenHeader(
                title = stringResource(R.string.immediate_title),
                onBack = onBack,
                subtitle = stringResource(R.string.immediate_subtitle),
                modifier = margin,
            )

            // docs/decisions.md, 2026-10-23: no radius chips here. Who receives
            // the request is decided by each professional's coverage.
            map(
                SearchMapModel(
                    origin = form.selectedAddress.coordinate,
                    radius = SearchRadius.entries.last(),
                    professionals = nearby,
                    searching = form.nearby is NearbyState.Searching,
                    selectedId = form.selectedProfessionalId,
                ),
                margin
                    .fillMaxWidth()
                    .height(Spacing.mapHeight)
                    .clip(MaterialTheme.shapes.large),
            )

            NearbyLine(nearby = form.nearby, modifier = margin)

            selected?.let { professional ->
                NearbyProfessionalCard(
                    professional = professional,
                    onClick = { actions.onOpenProfessional(professional.professionalId) },
                    modifier = margin,
                )
            }

            ServiceTypePicker(form = form, actions = actions, modifier = margin)

            // The alias alone reads like a kind of place ("Consultorio"), so the
            // street goes under it, and another address can be added right here
            // (docs/decisions.md, 2026-10-27, HT-24).
            Column(modifier = margin) {
                RadioOptionGroup(
                    label = stringResource(R.string.request_create_address_label),
                    options = form.addresses,
                    selected = form.selectedAddress,
                    onSelectedChange = { actions.onAddressSelected(it.id) },
                    optionLabel = Address::alias,
                    optionSupportingText = Address::addressText,
                )
                TextButton(onClick = actions.onRegisterAddress) {
                    Text(text = stringResource(R.string.request_address_add_another))
                }
            }

            FormField(
                label = stringResource(R.string.request_create_description_label),
                value = form.description,
                onValueChange = actions.onDescriptionChanged,
                singleLine = false,
                errorText =
                    stringResource(R.string.error_request_invalid_description)
                        .takeIf { form.submitError == RequestError.InvalidDescription },
                modifier = margin,
            )

            Column(modifier = margin, verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                FormField(
                    label = stringResource(R.string.request_create_budget_label),
                    value = form.budget,
                    onValueChange = actions.onBudgetChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    errorText =
                        stringResource(R.string.error_request_invalid_budget)
                            .takeIf { form.submitError == RequestError.InvalidBudget },
                )
                form.selectedServiceType?.let { type ->
                    Text(
                        text =
                            stringResource(
                                R.string.immediate_budget_reference,
                                formatBob(type.referencePriceBob, locale),
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        BottomActionBar {
            form.submitError
                ?.takeUnless { it == RequestError.InvalidDescription || it == RequestError.InvalidBudget }
                ?.let { error ->
                    Text(
                        text = stringResource(error.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            PrimaryButton(
                text = stringResource(R.string.immediate_submit),
                onClick = actions.onSubmit,
                enabled = form.canSubmit,
            )
        }
    }
}

@Composable
private fun ServiceTypePicker(
    form: ImmediateCareUiState.Form,
    actions: ImmediateCareActions,
    modifier: Modifier = Modifier,
) {
    ExpandableOptionPicker(
        label = stringResource(R.string.request_create_service_type_label),
        selectedLabel = form.selectedServiceType?.name ?: stringResource(R.string.immediate_service_type_placeholder),
        toggleDescription = stringResource(R.string.cd_open_type_filter),
        expanded = form.typeListExpanded,
        onToggle = actions.onTypeListToggle,
        options = form.serviceTypes,
        selected = form.selectedServiceType,
        onSelectedChange = { actions.onServiceTypeSelected(it.id) },
        optionLabel = ServiceType::name,
        modifier = modifier,
    )
}

// Nobody available does not hold the request back: availability changes in
// minutes and the request lasts longer than that (docs/decisions.md, 2026-10-23).
@Composable
private fun NearbyLine(
    nearby: NearbyState,
    modifier: Modifier = Modifier,
) {
    val text =
        when (nearby) {
            NearbyState.Searching -> {
                stringResource(R.string.immediate_nearby_searching)
            }

            NearbyState.Unavailable -> {
                stringResource(R.string.immediate_nearby_unavailable)
            }

            is NearbyState.Found -> {
                val count = nearby.professionals.size
                when {
                    count == 0 -> stringResource(R.string.immediate_nearby_none)
                    nearby.more -> pluralStringResource(R.plurals.immediate_nearby_more, count, count)
                    else -> pluralStringResource(R.plurals.immediate_nearby_count, count, count)
                }
            }
        }

    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun WaitingPanel(
    waiting: ImmediateCareUiState.Waiting,
    onBack: () -> Unit,
    actions: ImmediateCareActions,
    permissionPrompt: @Composable () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val request = waiting.request

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        ScreenHeader(title = stringResource(R.string.immediate_title), onBack = onBack)

        // The radar while nobody has offered. Once someone has, the offers take
        // its place, so the first card is on screen without scrolling.
        if (waiting.threads.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // Decorative: the title below says the same in words.
                RadarPulse(modifier = Modifier.size(Spacing.radarHeroSize))
                Box(
                    modifier =
                        Modifier
                            .size(Spacing.scale16)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
        }

        Text(
            text = stringResource(R.string.immediate_waiting_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.immediate_waiting_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The offers appear here by themselves, stacked as InDrive stacks its
        // drivers, and each one opens its negotiation (docs/decisions.md,
        // 2026-10-27, HT-24).
        if (waiting.threads.isNotEmpty()) {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.immediate_waiting_offers,
                        waiting.threads.size,
                        waiting.threads.size,
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            // Keyed by professional, so a card keeps its place and state when
            // another one arrives above it.
            waiting.threads.forEach { thread ->
                key(thread.professionalId) {
                    OfferCard(
                        thread = thread,
                        onClick = { actions.onOpenThread(request.id, thread.professionalId) },
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                Text(
                    text = request.serviceTypeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = request.addressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                request.expiresAt?.let { expiresAt ->
                    Text(
                        text =
                            stringResource(
                                R.string.immediate_waiting_expires,
                                formatTime(expiresAt.atZone(AppointmentZone).toLocalTime(), locale),
                            ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        permissionPrompt()

        waiting.cancelError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlinedButton(
            onClick = actions.onCancel,
            enabled = !waiting.cancelling,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.request_list_cancel_action))
        }
        TextButton(onClick = actions.onOpenMyRequests, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.immediate_waiting_my_requests))
        }
    }
}

@Composable
private fun MessageColumn(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        ScreenHeader(title = stringResource(R.string.immediate_title), onBack = onBack)
        content()
    }
}

@Composable
private fun CenteredProgress() {
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
    }
}

@Composable
private fun PreviewMap(
    model: SearchMapModel,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

private val previewVenousLine =
    ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60)

private fun previewForm(nearby: NearbyState = previewNearby()) =
    ImmediateCareUiState.Form(
        serviceTypes = listOf(previewVenousLine, ServiceType("t2", "Vacunación", null, BigDecimal("90.00"), 20)),
        addresses =
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
                Address(
                    id = "a2",
                    alias = "Casa de mi mamá",
                    addressText = "Calle Jordán 456, Cochabamba",
                    reference = null,
                    city = "Cochabamba",
                    coordinate = Coordinate.create(-17.394, -66.157).getOrThrow(),
                    isPrimary = false,
                    isProfessionalBase = false,
                ),
            ),
        selectedServiceTypeId = previewVenousLine.id,
        selectedAddressId = "a1",
        typeListExpanded = false,
        description = "",
        budget = "",
        nearby = nearby,
        selectedProfessionalId = "p1",
        submitting = false,
        submitError = null,
    )

private fun previewNearby() =
    NearbyState.Found(
        professionals =
            listOf(
                NearbyProfessional(
                    professionalId = "p1",
                    fullName = "Ana Pérez Quiroga",
                    photoUrl = null,
                    professionalType = ProfessionalType.NURSE,
                    specialty = "Enfermería geriátrica",
                    baseRateBob = BigDecimal("180.00"),
                    averageRating = 4.8,
                    totalReviews = 12,
                    availableNow = true,
                    distanceM = 1200,
                    basePoint = Coordinate.create(-17.368, -66.174).getOrThrow(),
                ),
            ),
        more = false,
    )

private fun previewWaiting(threads: List<OfferThread> = emptyList()) =
    ImmediateCareUiState.Waiting(
        threads = threads,
        request =
            ServiceRequest(
                id = "r1",
                serviceTypeName = previewVenousLine.name,
                addressText = "Avenida América 1234, Cochabamba",
                city = "Cochabamba",
                status = RequestStatus.PUBLISHED,
                modality = RequestModality.IMMEDIATE,
                description = null,
                suggestedBudgetBob = null,
                expiresAt = Instant.parse("2026-10-23T18:30:00Z"),
                createdAt = Instant.parse("2026-10-23T18:00:00Z"),
                scheduledAt = null,
                professionalName = null,
            ),
        cancelling = false,
        cancelError = null,
    )

@Composable
private fun PreviewScreen(
    uiState: ImmediateCareUiState,
    darkTheme: Boolean = false,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        ImmediateCareContent(
            uiState = uiState,
            onBack = {},
            actions = ImmediateCareActions(),
            map = { model, modifier -> PreviewMap(model, modifier) },
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1300, name = "Atención ahora, claro")
@Composable
private fun ImmediateFormLightPreview() {
    PreviewScreen(previewForm())
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1300, name = "Atención ahora, oscuro")
@Composable
private fun ImmediateFormDarkPreview() {
    PreviewScreen(previewForm(), darkTheme = true)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 2400, fontScale = 2f, name = "Atención ahora, 200 %")
@Composable
private fun ImmediateFormLargeFontPreview() {
    PreviewScreen(previewForm())
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1100, name = "Atención ahora, nadie cerca")
@Composable
private fun ImmediateFormNobodyPreview() {
    PreviewScreen(previewForm(nearby = NearbyState.Found(emptyList(), more = false)))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900, name = "Buscando profesional, claro")
@Composable
private fun ImmediateWaitingLightPreview() {
    PreviewScreen(previewWaiting())
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900, name = "Buscando profesional, oscuro")
@Composable
private fun ImmediateWaitingDarkPreview() {
    PreviewScreen(previewWaiting(), darkTheme = true)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1600, fontScale = 2f, name = "Buscando profesional, 200 %")
@Composable
private fun ImmediateWaitingLargeFontPreview() {
    PreviewScreen(previewWaiting())
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1000, name = "Buscando profesional con ofertas")
@Composable
private fun ImmediateWaitingWithOffersPreview() {
    PreviewScreen(previewWaiting(threads = previewThreads()))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1000, name = "Buscando profesional con ofertas, oscuro")
@Composable
private fun ImmediateWaitingWithOffersDarkPreview() {
    PreviewScreen(previewWaiting(threads = previewThreads()), darkTheme = true)
}

@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 2000,
    fontScale = 2f,
    name = "Buscando profesional con ofertas, 200 %",
)
@Composable
private fun ImmediateWaitingWithOffersLargeFontPreview() {
    PreviewScreen(previewWaiting(threads = previewThreads()))
}

@Preview(showBackground = true, widthDp = 320, name = "Atención ahora sin dirección")
@Composable
private fun ImmediateNoAddressPreview() {
    PreviewScreen(ImmediateCareUiState.NoAddress)
}

@Preview(showBackground = true, widthDp = 320, name = "Atención ahora con error")
@Composable
private fun ImmediateFailedPreview() {
    PreviewScreen(ImmediateCareUiState.Failed(RequestError.NetworkUnavailable))
}
