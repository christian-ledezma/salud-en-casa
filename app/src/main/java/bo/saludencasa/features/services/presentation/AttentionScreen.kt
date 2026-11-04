package bo.saludencasa.features.services.presentation

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionPlace
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.components.StatusBadge
import bo.saludencasa.ui.components.StatusTone
import bo.saludencasa.ui.components.badgeFitsBesideTitle
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

internal data class AttentionActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenProfessionalProfile: (String) -> Unit = {},
    val onOpenConversation: () -> Unit = {},
    val onOpenDirections: (String) -> Unit = {},
    val onMarkRequested: () -> Unit = {},
    val onMarkDismissed: () -> Unit = {},
    val onMarkConfirmed: () -> Unit = {},
)

// docs/decisions.md, 2026-10-30, and 2026-11-04, HU-23.
@Composable
fun AttentionScreen(
    requestId: String,
    onBack: () -> Unit,
    onOpenProfessionalProfile: (String) -> Unit,
    onOpenConversation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttentionViewModel = koinViewModel { parametersOf(requestId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { }
    }

    AttentionContent(
        uiState = uiState,
        actions =
            AttentionActions(
                onBack = onBack,
                onRetry = viewModel::refresh,
                onOpenProfessionalProfile = onOpenProfessionalProfile,
                onOpenConversation = onOpenConversation,
                // A phone with no maps application refuses it, and the address is
                // already on screen.
                onOpenDirections = { uri -> runCatching { uriHandler.openUri(uri) } },
                onMarkRequested = viewModel::onMarkRequested,
                onMarkDismissed = viewModel::onMarkDismissed,
                onMarkConfirmed = viewModel::onMarkConfirmed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun AttentionContent(
    uiState: AttentionUiState,
    actions: AttentionActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            AttentionUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            AttentionUiState.NotFound -> {
                MessageColumn(onBack = actions.onBack) {
                    Text(
                        text = stringResource(R.string.attention_not_found_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.attention_not_found_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is AttentionUiState.Failed -> {
                MessageColumn(onBack = actions.onBack) {
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                }
            }

            is AttentionUiState.Content -> {
                AttentionBody(content = uiState, actions = actions)
            }
        }
    }
}

@Composable
private fun AttentionBody(
    content: AttentionUiState.Content,
    actions: AttentionActions,
) {
    val attention = content.attention
    val fontScale = LocalDensity.current.fontScale
    val large = fontScale >= LARGE_FONT_SCALE
    // On a 320 dp phone, any enlarged font breaks the title in the middle of a word.
    val pillFitsBesideTitle = badgeFitsBesideTitle()
    val margin = Modifier.padding(horizontal = Spacing.screenMargin)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.scale16),
            verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap),
        ) {
            Column(modifier = margin, verticalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScreenHeader(
                        title = stringResource(R.string.attention_title),
                        onBack = actions.onBack,
                        modifier = Modifier.weight(1f),
                    )
                    if (pillFitsBesideTitle) StatusPill(status = attention.status)
                }
                if (!pillFitsBesideTitle) StatusPill(status = attention.status)
                Hero(attention = attention)
            }

            SummaryCard(attention = attention, large = large, actions = actions, modifier = margin)

            Progress(attention = attention, modifier = margin)

            attention.description?.let { description ->
                Column(modifier = margin, verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                    SectionTitle(stringResource(R.string.attention_description_title))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        attention.nextStepForMe?.let { step ->
            NextStepBar(step = step, content = content, actions = actions)
        }
    }

    content.confirming?.let { step ->
        ConfirmStepDialog(step = step, patientName = attention.counterpart.name, actions = actions)
    }
}

@Composable
private fun StatusPill(status: ServiceStatus) {
    StatusBadge(label = stringResource(status.labelRes()), tone = status.tone())
}

@Composable
private fun Hero(attention: Attention) {
    val name = attention.counterpart.name
    val title =
        when (attention.status) {
            ServiceStatus.ASSIGNED -> {
                stringResource(
                    if (attention.iAmThePatient) {
                        R.string.attention_hero_assigned_patient_title
                    } else {
                        R.string.attention_hero_assigned_professional_title
                    },
                )
            }

            ServiceStatus.IN_PROGRESS -> {
                if (attention.iAmThePatient) {
                    stringResource(R.string.attention_hero_in_progress_patient_title, name)
                } else {
                    stringResource(R.string.attention_hero_in_progress_professional_title)
                }
            }

            ServiceStatus.COMPLETED -> {
                stringResource(R.string.attention_hero_completed_title)
            }

            ServiceStatus.CANCELLED -> {
                stringResource(R.string.attention_hero_cancelled_title)
            }
        }
    val body =
        when (attention.status) {
            ServiceStatus.ASSIGNED -> {
                if (attention.iAmThePatient) {
                    stringResource(R.string.attention_hero_assigned_patient_body, name)
                } else {
                    stringResource(R.string.attention_hero_assigned_professional_body)
                }
            }

            ServiceStatus.IN_PROGRESS -> {
                stringResource(
                    if (attention.iAmThePatient) {
                        R.string.attention_hero_in_progress_patient_body
                    } else {
                        R.string.attention_hero_in_progress_professional_body
                    },
                )
            }

            ServiceStatus.COMPLETED -> {
                stringResource(
                    if (attention.iAmThePatient) {
                        R.string.attention_hero_completed_patient_body
                    } else {
                        R.string.attention_hero_completed_professional_body
                    },
                )
            }

            ServiceStatus.CANCELLED -> {
                stringResource(R.string.attention_hero_cancelled_body)
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryCard(
    attention: Attention,
    large: Boolean,
    actions: AttentionActions,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ExtraShapes.featuredCard,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Spacing.scale4,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        ) {
            PersonRow(attention = attention, large = large, onOpen = actions.onOpenProfessionalProfile)

            FilledTonalButton(
                onClick = actions.onOpenConversation,
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat),
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.scale20),
                )
                Text(
                    text = stringResource(R.string.attention_open_conversation),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = Spacing.scale8),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            InfoRow(
                icon = rememberVectorPainter(Icons.Filled.DateRange),
                label = stringResource(R.string.attention_when_label),
            ) {
                Text(
                    text = attention.whenText(locale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            InfoRow(
                icon = rememberVectorPainter(Icons.Filled.LocationOn),
                label = stringResource(R.string.attention_where_label),
            ) {
                PlaceDetails(
                    place = attention.place,
                    showsDirections = !attention.iAmThePatient,
                    onOpenDirections = actions.onOpenDirections,
                )
            }

            InfoRow(
                icon = painterResource(R.drawable.ic_payments),
                label = stringResource(R.string.attention_amount_label),
            ) {
                Text(
                    text = formatBob(attention.finalAmountBob, locale),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Attention.whenText(locale: Locale): String =
    scheduledAt?.let { stringResource(R.string.attention_when_scheduled, formatAppointment(it, locale)) }
        ?: stringResource(R.string.attention_when_immediate, formatAppointment(createdAt, locale))

@Composable
private fun PersonRow(
    attention: Attention,
    large: Boolean,
    onOpen: (String) -> Unit,
) {
    val counterpart = attention.counterpart
    val viewProfile = stringResource(R.string.attention_view_professional)
    val role =
        stringResource(
            if (attention.iAmThePatient) {
                R.string.attention_counterpart_professional
            } else {
                R.string.attention_counterpart_patient
            },
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouchTarget)
                .then(
                    if (attention.iAmThePatient) {
                        Modifier.clickable(onClickLabel = viewProfile) { onOpen(counterpart.id) }
                    } else {
                        Modifier
                    },
                ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // At a large font the name needs the width the avatar would take.
        if (!large) CardAvatar(name = counterpart.name, photoUrl = counterpart.photoUrl)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = counterpart.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.attention_person_subtitle, role, attention.serviceTypeName),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (attention.iAmThePatient) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: Painter,
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.scale20),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun PlaceDetails(
    place: AttentionPlace,
    showsDirections: Boolean,
    onOpenDirections: (String) -> Unit,
) {
    Text(
        text = place.addressText ?: place.city,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    place.reference?.let { reference ->
        Text(
            text = reference,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val latitude = place.latitude
    val longitude = place.longitude
    if (showsDirections && latitude != null && longitude != null) {
        OutlinedButton(
            onClick = { onOpenDirections(directionsUri(latitude, longitude)) },
            modifier = Modifier.padding(top = Spacing.scale4).heightIn(min = Spacing.minTouchTarget),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(Spacing.borderWidth, MaterialTheme.colorScheme.outline),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_near_me),
                contentDescription = null,
                modifier = Modifier.size(Spacing.scale20),
            )
            Text(
                text = stringResource(R.string.attention_directions),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = Spacing.scale8),
            )
        }
    }
}

@Composable
private fun Progress(
    attention: Attention,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val steps = attention.timeline()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
        SectionTitle(stringResource(R.string.attention_progress_title))
        Column {
            steps.forEachIndexed { index, step ->
                val next = steps.getOrNull(index + 1)
                TimelineRow(
                    step = step,
                    locale = locale,
                    hasLine = next != null,
                    lineReached = next?.state == TimelineStepState.DONE,
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    step: TimelineStep,
    locale: Locale,
    hasLine: Boolean,
    lineReached: Boolean,
) {
    val positive = SaludEnCasaTheme.statusColors.positive
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimelineDot(state = step.state)
            if (hasLine) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .width(Spacing.timelineStroke)
                            .background(if (lineReached) positive else outline),
                )
            }
        }
        Column(
            modifier = Modifier.padding(bottom = if (hasLine) Spacing.scale16 else Spacing.scale4),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = stringResource(step.titleRes),
                style =
                    if (step.state == TimelineStepState.CURRENT) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                color =
                    when (step.state) {
                        TimelineStepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        TimelineStepState.CANCELLED -> MaterialTheme.colorScheme.error
                        TimelineStepState.DONE, TimelineStepState.CURRENT -> MaterialTheme.colorScheme.onBackground
                    },
            )
            val detail =
                when {
                    step.at != null -> formatAppointment(step.at, locale)
                    step.state == TimelineStepState.CANCELLED -> null
                    else -> stringResource(R.string.attention_step_pending)
                }
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TimelineDot(state: TimelineStepState) {
    val colors = MaterialTheme.colorScheme
    val dot = Modifier.size(Spacing.scale24).background(colors.background, CircleShape)
    when (state) {
        TimelineStepState.DONE -> {
            Box(
                modifier = dot.background(SaludEnCasaTheme.statusColors.positive, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.surface,
                    modifier = Modifier.size(Spacing.scale16),
                )
            }
        }

        TimelineStepState.CANCELLED -> {
            Box(modifier = dot.background(colors.error, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = colors.onError,
                    modifier = Modifier.size(Spacing.scale16),
                )
            }
        }

        TimelineStepState.CURRENT -> {
            Box(modifier = dot.border(Spacing.timelineStroke, colors.primary, CircleShape))
        }

        TimelineStepState.PENDING -> {
            Box(modifier = dot.border(Spacing.timelineStroke, colors.outline, CircleShape))
        }
    }
}

@Composable
private fun NextStepBar(
    step: AttentionStep,
    content: AttentionUiState.Content,
    actions: AttentionActions,
) {
    BottomActionBar {
        content.notice?.let { notice ->
            Text(
                text = stringResource(notice.markMessageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        PrimaryButton(
            text = stringResource(step.actionRes()),
            onClick = actions.onMarkRequested,
            enabled = !content.advancing,
        )
    }
}

@Composable
private fun ConfirmStepDialog(
    step: AttentionStep,
    patientName: String,
    actions: AttentionActions,
) {
    AlertDialog(
        onDismissRequest = actions.onMarkDismissed,
        title = {
            Text(
                text =
                    stringResource(
                        when (step) {
                            AttentionStep.MarkArrival -> R.string.attention_confirm_arrival_title
                            AttentionStep.MarkCompleted -> R.string.attention_confirm_completed_title
                        },
                    ),
            )
        },
        text = {
            Text(
                text =
                    when (step) {
                        AttentionStep.MarkArrival -> {
                            stringResource(R.string.attention_confirm_arrival_body, patientName)
                        }

                        AttentionStep.MarkCompleted -> {
                            stringResource(R.string.attention_confirm_completed_body)
                        }
                    },
            )
        },
        confirmButton = {
            TextButton(onClick = actions.onMarkConfirmed) {
                Text(text = stringResource(step.actionRes()))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onMarkDismissed) {
                Text(text = stringResource(R.string.attention_mark_not_yet))
            }
        },
    )
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
private fun MessageColumn(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin)
                .padding(vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        ScreenHeader(title = stringResource(R.string.attention_title), onBack = onBack)
        content()
    }
}

@StringRes
private fun AttentionStep.actionRes(): Int =
    when (this) {
        AttentionStep.MarkArrival -> R.string.attention_mark_arrival
        AttentionStep.MarkCompleted -> R.string.attention_mark_completed
    }

internal fun ServiceStatus.tone(): StatusTone =
    when (this) {
        ServiceStatus.ASSIGNED -> StatusTone.NEUTRAL
        ServiceStatus.IN_PROGRESS -> StatusTone.RUNNING
        ServiceStatus.COMPLETED -> StatusTone.POSITIVE
        ServiceStatus.CANCELLED -> StatusTone.NEGATIVE
    }

@StringRes
internal fun ServiceStatus.labelRes(): Int =
    when (this) {
        ServiceStatus.ASSIGNED -> R.string.attention_status_assigned
        ServiceStatus.IN_PROGRESS -> R.string.attention_status_in_progress
        ServiceStatus.COMPLETED -> R.string.attention_status_completed
        ServiceStatus.CANCELLED -> R.string.attention_status_cancelled
    }

@StringRes
private fun ServiceError.messageRes(): Int =
    when (this) {
        ServiceError.NetworkUnavailable -> R.string.error_network_unavailable
        ServiceError.NotSignedIn, ServiceError.AttentionChanged, ServiceError.Unexpected -> R.string.error_unexpected
    }

@StringRes
private fun ServiceError.markMessageRes(): Int =
    when (this) {
        ServiceError.NetworkUnavailable -> R.string.attention_mark_failed_network
        ServiceError.AttentionChanged -> R.string.attention_mark_changed
        ServiceError.NotSignedIn, ServiceError.Unexpected -> R.string.error_unexpected
    }

private const val LARGE_FONT_SCALE = 1.5f

private val previewAttention =
    Attention(
        serviceId = "s1",
        requestId = "r1",
        status = ServiceStatus.ASSIGNED,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        counterpart = Counterpart(id = "p1", name = "Ana Quispe", photoUrl = null),
        iAmThePatient = false,
        createdAt = Instant.parse("2026-11-04T13:00:00Z"),
        scheduledAt = null,
        startedAt = null,
        finishedAt = null,
        description = "Curación de una herida en la pierna derecha. Tengo gasas y alcohol, pero no suero.",
        place =
            AttentionPlace(
                city = "La Paz",
                addressText = "Av. Arce 2631, edificio azul",
                reference = "Tercer piso, departamento 3B. El timbre no funciona.",
                latitude = -16.5103,
                longitude = -68.1218,
            ),
    )

private val previewInProgress =
    previewAttention.copy(status = ServiceStatus.IN_PROGRESS, startedAt = Instant.parse("2026-11-04T14:02:00Z"))

private val previewCompleted =
    previewInProgress.copy(status = ServiceStatus.COMPLETED, finishedAt = Instant.parse("2026-11-04T15:40:00Z"))

@Composable
private fun AttentionPreviewFrame(uiState: AttentionUiState) {
    SaludEnCasaTheme {
        AttentionContent(uiState = uiState, actions = AttentionActions())
    }
}

@Preview(name = "Attention", showBackground = true)
@Composable
private fun AttentionPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewAttention))
}

@Preview(name = "Attention dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewInProgress))
}

@Preview(name = "Attention large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewInProgress))
}

@Preview(name = "Attention scheduled", showBackground = true)
@Composable
private fun AttentionScheduledPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewAttention.copy(scheduledAt = Instant.parse("2026-11-05T19:00:00Z"))),
    )
}

@Preview(name = "Attention confirming", showBackground = true)
@Composable
private fun AttentionConfirmingPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewInProgress, confirming = AttentionStep.MarkCompleted),
    )
}

@Preview(name = "Attention confirming large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionConfirmingLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewAttention, confirming = AttentionStep.MarkArrival))
}

@Preview(name = "Attention not saved", showBackground = true)
@Composable
private fun AttentionNotSavedPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewAttention, notice = ServiceError.NetworkUnavailable),
    )
}

@Preview(name = "Attention professional completed", showBackground = true)
@Composable
private fun AttentionProfessionalCompletedPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(
            previewCompleted.copy(
                place =
                    AttentionPlace(
                        "La Paz",
                        addressText = null,
                        reference = null,
                        latitude = null,
                        longitude = null,
                    ),
            ),
        ),
    )
}

private val previewForThePatient =
    Counterpart(id = "p1", name = "María Flores", photoUrl = null)

@Preview(name = "Attention patient in progress", showBackground = true)
@Composable
private fun AttentionPatientInProgressPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewInProgress.copy(iAmThePatient = true, counterpart = previewForThePatient)),
    )
}

@Preview(name = "Attention patient completed", showBackground = true)
@Composable
private fun AttentionPatientCompletedPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewCompleted.copy(iAmThePatient = true, counterpart = previewForThePatient)),
    )
}

@Preview(name = "Attention cancelled", showBackground = true)
@Composable
private fun AttentionCancelledPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewAttention.copy(status = ServiceStatus.CANCELLED)))
}

@Preview(name = "Attention failed", showBackground = true)
@Composable
private fun AttentionFailedPreview() {
    AttentionPreviewFrame(AttentionUiState.Failed(ServiceError.NetworkUnavailable))
}
