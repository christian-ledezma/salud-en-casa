package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.profile.presentation.labelRes
import bo.saludencasa.features.requests.domain.model.BookableStart
import bo.saludencasa.features.requests.domain.model.BookableTimes
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.DayCarousel
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.RadioOptionGroup
import bo.saludencasa.ui.components.RatingBadge
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.components.TimeChipRow
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun ScheduleAppointmentScreen(
    professionalId: String,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onRegisterAddress: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleAppointmentViewModel = koinViewModel { parametersOf(professionalId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is ScheduleAppointmentUiState.Submitted) onCreated()
    }

    ScheduleAppointmentContent(
        uiState = uiState,
        onBack = onBack,
        actions =
            ScheduleAppointmentActions(
                onServiceSelected = viewModel::onServiceSelected,
                onDateSelected = viewModel::onDateSelected,
                onTimeSelected = viewModel::onTimeSelected,
                onAddressSelected = viewModel::onAddressSelected,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onBudgetChanged = viewModel::onBudgetChanged,
                onRetryTimes = viewModel::onRetryTimes,
                onSubmit = viewModel::onSubmit,
                onRetry = viewModel::load,
                onRegisterAddress = onRegisterAddress,
            ),
        modifier = modifier,
    )
}

internal data class ScheduleAppointmentActions(
    val onServiceSelected: (String) -> Unit = {},
    val onDateSelected: (LocalDate) -> Unit = {},
    val onTimeSelected: (BookableStart) -> Unit = {},
    val onAddressSelected: (String) -> Unit = {},
    val onDescriptionChanged: (String) -> Unit = {},
    val onBudgetChanged: (String) -> Unit = {},
    val onRetryTimes: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRegisterAddress: () -> Unit = {},
)

@Composable
internal fun ScheduleAppointmentContent(
    uiState: ScheduleAppointmentUiState,
    onBack: () -> Unit,
    actions: ScheduleAppointmentActions,
    modifier: Modifier = Modifier,
) {
    // The screen paints its own background (docs/decisions.md, 2026-10-14, the welcome screen).
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        ScheduleAppointmentState(uiState = uiState, onBack = onBack, actions = actions)
    }
}

@Composable
private fun ScheduleAppointmentState(
    uiState: ScheduleAppointmentUiState,
    onBack: () -> Unit,
    actions: ScheduleAppointmentActions,
) {
    when (uiState) {
        ScheduleAppointmentUiState.Loading, ScheduleAppointmentUiState.Submitted -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }

        ScheduleAppointmentUiState.NotBookable -> {
            MessageColumn(onBack = onBack) {
                MessageTitle(text = stringResource(R.string.schedule_not_bookable_title))
                MessageBody(text = stringResource(R.string.schedule_not_bookable_description))
            }
        }

        ScheduleAppointmentUiState.NoAddress -> {
            MessageColumn(onBack = onBack) {
                MessageTitle(text = stringResource(R.string.request_create_no_address_title))
                MessageBody(text = stringResource(R.string.request_create_no_address_description))
                PrimaryButton(
                    text = stringResource(R.string.request_create_no_address_action),
                    onClick = actions.onRegisterAddress,
                )
            }
        }

        is ScheduleAppointmentUiState.Failed -> {
            MessageColumn(onBack = onBack) {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
            }
        }

        is ScheduleAppointmentUiState.Content -> {
            ScheduleForm(uiState = uiState, onBack = onBack, actions = actions)
        }
    }
}

@Composable
private fun ScheduleForm(
    uiState: ScheduleAppointmentUiState.Content,
    onBack: () -> Unit,
    actions: ScheduleAppointmentActions,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val margin = Modifier.padding(horizontal = Spacing.screenMargin)
    // Resolved here because optionLabel is not composable.
    val serviceLabels =
        uiState.services.associate { service ->
            service.type.id to
                stringResource(
                    R.string.schedule_service_option,
                    service.type.name,
                    formatBob(service.referencePriceBob, locale),
                )
        }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap),
        ) {
            ScreenHeader(
                title = stringResource(R.string.schedule_title),
                onBack = onBack,
                subtitle = stringResource(R.string.schedule_subtitle),
                modifier = margin,
            )

            ProfessionalSummary(professional = uiState.professional, modifier = margin)

            RadioOptionGroup(
                label = stringResource(R.string.schedule_service_label),
                options = uiState.services,
                selected = uiState.selectedService,
                onSelectedChange = { actions.onServiceSelected(it.type.id) },
                optionLabel = { service -> serviceLabels.getValue(service.type.id) },
                modifier = margin,
            )

            TimesSection(uiState = uiState, actions = actions)

            RadioOptionGroup(
                label = stringResource(R.string.schedule_address_label),
                options = uiState.addresses,
                selected = uiState.addresses.firstOrNull { it.id == uiState.selectedAddressId },
                onSelectedChange = { actions.onAddressSelected(it.id) },
                optionLabel = Address::alias,
                modifier = margin,
            )

            FormField(
                label = stringResource(R.string.request_create_description_label),
                value = uiState.description,
                onValueChange = actions.onDescriptionChanged,
                singleLine = false,
                errorText =
                    stringResource(R.string.error_request_invalid_description)
                        .takeIf { uiState.submitError == RequestError.InvalidDescription },
                modifier = margin,
            )

            Column(modifier = margin, verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                FormField(
                    label = stringResource(R.string.request_create_budget_label),
                    value = uiState.budget,
                    onValueChange = actions.onBudgetChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    errorText =
                        stringResource(R.string.error_request_invalid_budget)
                            .takeIf { uiState.submitError == RequestError.InvalidBudget },
                )
                uiState.selectedService?.let { service ->
                    Text(
                        text =
                            stringResource(
                                R.string.schedule_budget_reference,
                                formatBob(service.referencePriceBob, locale),
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        BottomActionBar {
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
                text =
                    uiState.selectedStart
                        ?.let {
                            stringResource(
                                R.string.schedule_submit_at,
                                formatAppointment(it.date, it.time, locale),
                            )
                        }
                        ?: stringResource(R.string.schedule_submit_pick_time),
                onClick = actions.onSubmit,
                enabled = uiState.canSubmit,
            )
        }
    }
}

@Composable
private fun ProfessionalSummary(
    professional: PublicProfile,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardAvatar(name = professional.fullName, photoUrl = professional.photoUrl)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                Text(
                    text = professional.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                professional.professionalType?.let { type ->
                    Text(
                        text = stringResource(type.labelRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                // docs/decisions.md, 2026-10-09, no badge without ratings.
                if (professional.totalReviews > 0) RatingBadge(rating = professional.averageRating)
            }
        }
    }
}

@Composable
private fun TimesSection(
    uiState: ScheduleAppointmentUiState.Content,
    actions: ScheduleAppointmentActions,
) {
    val margin = Modifier.padding(horizontal = Spacing.screenMargin)

    when (val times = uiState.times) {
        TimesState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        }

        is TimesState.Failed -> {
            Column(modifier = margin, verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                Text(
                    text = stringResource(times.error.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = actions.onRetryTimes) {
                    Text(text = stringResource(R.string.common_retry))
                }
            }
        }

        is TimesState.Loaded -> {
            if (times.times.isEmpty) {
                Column(modifier = margin, verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                    MessageTitle(text = stringResource(R.string.schedule_no_times_title))
                    MessageBody(text = stringResource(R.string.schedule_no_times_description))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.labelToField)) {
                    FieldLabel(text = stringResource(R.string.schedule_day_label), modifier = margin)
                    DayCarousel(
                        days = times.times.days,
                        selected = uiState.selectedDate,
                        onSelectedChange = actions.onDateSelected,
                        isAvailable = times.times::hasTimesOn,
                        contentPadding = PaddingValues(horizontal = Spacing.screenMargin),
                    )
                }
                uiState.selectedDate?.let { date ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.labelToField)) {
                        FieldLabel(text = stringResource(R.string.schedule_time_label), modifier = margin)
                        TimeCarousel(
                            date = date,
                            times = times.times,
                            selected = uiState.selectedStart,
                            onSelected = actions.onTimeSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeCarousel(
    date: LocalDate,
    times: BookableTimes,
    selected: BookableStart?,
    onSelected: (BookableStart) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(date) { state.scrollToItem(0) }
    val starts = times.on(date).values.flatten()

    TimeChipRow(
        times = starts.map(BookableStart::time),
        selected = selected?.time?.takeIf { selected.date == date },
        onSelected = { time -> starts.first { it.time == time }.let(onSelected) },
        state = state,
        contentPadding = PaddingValues(horizontal = Spacing.screenMargin),
    )
}

@Composable
private fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun MessageTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun MessageBody(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LoadingIndicator() {
    val loadingDescription = stringResource(R.string.cd_loading)
    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
}

@Composable
private fun MessageColumn(
    onBack: () -> Unit,
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
        ScreenHeader(title = stringResource(R.string.schedule_title), onBack = onBack)
        content()
    }
}

private fun previewProfessional(): PublicProfile =
    PublicProfile(
        professionalId = "fcab94c0-0000-4000-8000-000000000000",
        fullName = "Ana Quispe",
        photoUrl = null,
        averageRating = 4.8,
        totalReviews = 12,
        totalServices = 34,
        professionalType = ProfessionalType.NURSE,
        specialty = "Enfermería geriátrica",
        biography = null,
        yearsOfExperience = 10,
        baseRateBob = BigDecimal("120.00"),
        availableNow = true,
    )

private fun previewServices(): List<ProfessionalService> =
    listOf(
        ProfessionalService(
            id = "1",
            type = ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60),
            referencePriceBob = BigDecimal("120.00"),
        ),
        ProfessionalService(
            id = "2",
            type = ServiceType("t2", "Curación de heridas", null, BigDecimal("100.00"), 40),
            referencePriceBob = BigDecimal("100.00"),
        ),
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

// La Paz is UTC-4 all year, with no daylight saving.
private fun previewStart(
    date: LocalDate,
    hour: Int,
    minute: Int = 0,
): BookableStart {
    val time = LocalTime.of(hour, minute)
    return BookableStart(
        startAt = date.atTime(time).toInstant(ZoneOffset.ofHours(-4)),
        date = date,
        time = time,
    )
}

private fun previewTimes(): BookableTimes {
    val thursday = LocalDate.of(2026, 10, 15)
    val friday = thursday.plusDays(1)
    val monday = thursday.plusDays(4)
    return BookableTimes(
        listOf(
            previewStart(thursday, 15),
            previewStart(friday, 9),
            previewStart(friday, 9, 30),
            previewStart(friday, 10),
            previewStart(friday, 14),
            previewStart(friday, 14, 30),
            previewStart(friday, 19),
            previewStart(monday, 9),
        ),
    )
}

private fun previewContent(
    times: TimesState = TimesState.Loaded(previewTimes()),
    submitError: RequestError? = null,
): ScheduleAppointmentUiState.Content {
    val friday = LocalDate.of(2026, 10, 16)
    return ScheduleAppointmentUiState.Content(
        professional = previewProfessional(),
        services = previewServices(),
        addresses = previewAddresses(),
        selectedServiceTypeId = "t1",
        times = times,
        selectedDate = friday.takeIf { (times as? TimesState.Loaded)?.times?.isEmpty == false },
        selectedStart = previewStart(friday, 9, 30).takeIf { (times as? TimesState.Loaded)?.times?.isEmpty == false },
        selectedAddressId = "a1",
        description = "",
        budget = "",
        submitting = false,
        submitError = submitError,
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1400, name = "Agendar una cita, claro")
@Composable
private fun ScheduleAppointmentLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ScheduleAppointmentContent(previewContent(), onBack = {}, actions = ScheduleAppointmentActions())
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1400, name = "Agendar una cita, oscuro")
@Composable
private fun ScheduleAppointmentDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ScheduleAppointmentContent(previewContent(), onBack = {}, actions = ScheduleAppointmentActions())
    }
}

@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 2600,
    fontScale = 2f,
    name = "Agendar una cita al 200 por ciento, LargeFont",
)
@Composable
private fun ScheduleAppointmentLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ScheduleAppointmentContent(previewContent(), onBack = {}, actions = ScheduleAppointmentActions())
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1200, name = "Agendar una cita sin horarios")
@Composable
private fun ScheduleAppointmentNoTimesPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ScheduleAppointmentContent(
            previewContent(times = TimesState.Loaded(BookableTimes(emptyList()))),
            onBack = {},
            actions = ScheduleAppointmentActions(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1400, name = "Agendar una cita con la hora vencida")
@Composable
private fun ScheduleAppointmentTimeNotBookablePreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ScheduleAppointmentContent(
            previewContent(submitError = RequestError.TimeNotBookable),
            onBack = {},
            actions = ScheduleAppointmentActions(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Agendar una cita, profesional sin servicios")
@Composable
private fun ScheduleAppointmentNotBookablePreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ScheduleAppointmentContent(
            ScheduleAppointmentUiState.NotBookable,
            onBack = {},
            actions = ScheduleAppointmentActions(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Agendar una cita sin dirección")
@Composable
private fun ScheduleAppointmentNoAddressPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ScheduleAppointmentContent(
            ScheduleAppointmentUiState.NoAddress,
            onBack = {},
            actions = ScheduleAppointmentActions(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Agendar una cita con error")
@Composable
private fun ScheduleAppointmentFailedPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ScheduleAppointmentContent(
            ScheduleAppointmentUiState.Failed(RequestError.NetworkUnavailable),
            onBack = {},
            actions = ScheduleAppointmentActions(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Agendar una cita, cargando")
@Composable
private fun ScheduleAppointmentLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ScheduleAppointmentContent(
            previewContent(times = TimesState.Loading),
            onBack = {},
            actions = ScheduleAppointmentActions(),
        )
    }
}
