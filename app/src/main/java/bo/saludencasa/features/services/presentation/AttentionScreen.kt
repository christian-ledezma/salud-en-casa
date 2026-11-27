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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionCancellation
import bo.saludencasa.features.services.domain.model.AttentionPayment
import bo.saludencasa.features.services.domain.model.AttentionPlace
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.MyReview
import bo.saludencasa.features.services.domain.model.PaymentAnswer
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.model.PaymentStatus
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.vo.CancellationReason
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.FormField
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
    val onCancelRequested: () -> Unit = {},
    val onCancelReasonChanged: (String) -> Unit = {},
    val onCancelDismissed: () -> Unit = {},
    val onCancelConfirmed: () -> Unit = {},
    val onPayRequested: () -> Unit = {},
    val onPaymentMethodChosen: (PaymentMethod) -> Unit = {},
    val onPayDismissed: () -> Unit = {},
    val onPayConfirmed: () -> Unit = {},
    val onAnswerRequested: (PaymentAnswer) -> Unit = {},
    val onAnswerDismissed: () -> Unit = {},
    val onAnswerConfirmed: () -> Unit = {},
    val onAcceptReceipt: () -> Unit = {},
    val onRate: (Int) -> Unit = {},
)

// docs/decisions.md, 2026-10-30, and 2026-11-04, HU-23.
@Composable
fun AttentionScreen(
    requestId: String,
    onBack: () -> Unit,
    onOpenProfessionalProfile: (String) -> Unit,
    onOpenConversation: () -> Unit,
    onRate: (Int) -> Unit,
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
                onCancelRequested = viewModel::onCancelRequested,
                onCancelReasonChanged = viewModel::onCancelReasonChanged,
                onCancelDismissed = viewModel::onCancelDismissed,
                onCancelConfirmed = viewModel::onCancelConfirmed,
                onPayRequested = viewModel::onPayRequested,
                onPaymentMethodChosen = viewModel::onPaymentMethodChosen,
                onPayDismissed = viewModel::onPayDismissed,
                onPayConfirmed = viewModel::onPayConfirmed,
                onAnswerRequested = viewModel::onAnswerRequested,
                onAnswerDismissed = viewModel::onAnswerDismissed,
                onAnswerConfirmed = viewModel::onAnswerConfirmed,
                onAcceptReceipt = viewModel::onAcceptReceipt,
                onRate = onRate,
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

            attention.cancellation?.let { cancellation ->
                CancellationReasonCard(cancellation = cancellation, modifier = margin)
            }

            // Only the patient's side exists so far; the professional's is HU-30.
            if (attention.canRate) {
                RateAttentionCard(
                    counterpartName = attention.counterpart.name,
                    onPick = actions.onRate,
                    modifier = margin,
                )
            }
            attention.myReview?.takeIf { attention.iAmThePatient }?.let { review ->
                MyReviewCard(review = review, counterpartName = attention.counterpart.name, modifier = margin)
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

            if (attention.canCancel) {
                CancelAttentionButton(
                    enabled = !content.advancing,
                    onClick = actions.onCancelRequested,
                    modifier = margin,
                )
            }
        }

        attention.nextStepForMe?.let { step ->
            NextStepBar(step = step, content = content, actions = actions)
        }

        if (attention.canDeclarePayment) {
            BottomActionBar {
                PrimaryButton(
                    text =
                        stringResource(
                            if (attention.iAmThePatient) {
                                R.string.attention_pay_action
                            } else {
                                R.string.attention_receipt_action
                            },
                        ),
                    onClick = actions.onPayRequested,
                )
            }
        }

        if (attention.canAcceptReceipt) {
            AcceptReceiptBar(content = content, onAccept = actions.onAcceptReceipt)
        }

        if (attention.canAnswerPayment) {
            BottomActionBar {
                PrimaryButton(
                    text = stringResource(R.string.attention_receipt_action),
                    onClick = { actions.onAnswerRequested(PaymentAnswer.RECEIVED) },
                )
                OutlinedErrorButton(
                    text = stringResource(R.string.attention_not_received_action),
                    enabled = true,
                    onClick = { actions.onAnswerRequested(PaymentAnswer.NOT_RECEIVED) },
                )
            }
        }
    }

    content.confirming?.let { step ->
        ConfirmStepDialog(step = step, patientName = attention.counterpart.name, actions = actions)
    }

    content.cancelDraft?.let { draft ->
        CancelAttentionDialog(draft = draft, counterpartName = attention.counterpart.name, actions = actions)
    }

    content.paymentDraft?.let { draft ->
        ConfirmPaymentDialog(draft = draft, attention = attention, actions = actions)
    }

    content.answerDraft?.let { draft ->
        AnswerPaymentDialog(draft = draft, attention = attention, actions = actions)
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
                    if (!attention.iAmThePatient && attention.conversationOpen) {
                        R.string.attention_hero_completed_professional_body
                    } else {
                        R.string.attention_hero_completed_patient_body
                    },
                )
            }

            ServiceStatus.CANCELLED -> {
                when (attention.cancellation?.byMe) {
                    true -> stringResource(R.string.attention_hero_cancelled_by_me_body)
                    false -> stringResource(R.string.attention_hero_cancelled_by_other_body, name)
                    null -> stringResource(R.string.attention_hero_cancelled_body)
                }
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
                PaymentLines(attention = attention, locale = locale)
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

// docs/decisions.md, 2026-11-24, HU-28: accepting only agrees with what is
// already recorded, so it goes without a dialog.
@Composable
private fun AcceptReceiptBar(
    content: AttentionUiState.Content,
    onAccept: () -> Unit,
) {
    BottomActionBar {
        content.notice?.let { notice ->
            Text(
                text = stringResource(notice.acceptMessageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        PrimaryButton(
            text = stringResource(R.string.attention_accept_receipt_action),
            onClick = onAccept,
            enabled = !content.accepting,
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

// RF-11.2, RF-11.4: what each party reads of the payment, under its amount.
@Composable
private fun PaymentLines(
    attention: Attention,
    locale: Locale,
) {
    val payment = attention.payment
    val counterpart = attention.counterpart.name
    val confirmedAt = payment.patientConfirmedAt
    val receivedAtInstant = payment.professionalConfirmedAt

    when {
        attention.status == ServiceStatus.CANCELLED -> {
            MutedLine(stringResource(R.string.attention_no_payment_pending))
        }

        attention.status == ServiceStatus.COMPLETED && payment.status == PaymentStatus.PENDING -> {
            Text(
                text = stringResource(R.string.attention_payment_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MutedLine(
                if (attention.iAmThePatient) {
                    stringResource(R.string.attention_payment_pending_patient_hint, counterpart)
                } else {
                    stringResource(R.string.attention_payment_pending_professional_hint, counterpart)
                },
            )
        }

        payment.status == PaymentStatus.PATIENT_CONFIRMED && confirmedAt != null -> {
            val at = formatAppointment(confirmedAt, locale)
            PaymentConfirmedChip(
                if (attention.iAmThePatient) {
                    stringResource(payment.method.youPaidRes(), at)
                } else {
                    stringResource(payment.method.patientPaidRes(), counterpart, at)
                },
            )
            MutedLine(
                if (attention.iAmThePatient) {
                    stringResource(R.string.attention_payment_waiting_professional, counterpart)
                } else {
                    stringResource(R.string.attention_payment_waiting_you)
                },
            )
        }

        payment.status == PaymentStatus.PROFESSIONAL_CONFIRMED && receivedAtInstant != null -> {
            val receivedAt = formatAppointment(receivedAtInstant, locale)
            if (attention.iAmThePatient) {
                PaymentConfirmedChip(stringResource(payment.method.professionalReceivedRes(), counterpart, receivedAt))
                MutedLine(stringResource(R.string.attention_payment_accept_hint))
            } else {
                PaymentConfirmedChip(stringResource(payment.method.youReceivedRes(), receivedAt))
                MutedLine(stringResource(R.string.attention_payment_whole_for_you))
                MutedLine(stringResource(R.string.attention_payment_waiting_acceptance, counterpart))
            }
        }

        payment.status == PaymentStatus.BOTH_CONFIRMED && confirmedAt != null && receivedAtInstant != null -> {
            val receivedAt = formatAppointment(receivedAtInstant, locale)
            if (attention.iAmThePatient) {
                PaymentConfirmedChip(
                    stringResource(payment.method.youPaidRes(), formatAppointment(confirmedAt, locale)),
                )
                MutedLine(stringResource(R.string.attention_payment_professional_received, counterpart, receivedAt))
            } else {
                PaymentConfirmedChip(stringResource(payment.method.youReceivedRes(), receivedAt))
                // RF-11.1, INV-10: the platform keeps no part of the amount.
                MutedLine(stringResource(R.string.attention_payment_whole_for_you))
            }
        }

        payment.status == PaymentStatus.DISPUTED && payment.disputedAt != null -> {
            val disputedAt = formatAppointment(payment.disputedAt, locale)
            PaymentInReviewChip()
            MutedLine(
                if (attention.iAmThePatient) {
                    stringResource(payment.method.disputedByProfessionalRes(), counterpart, disputedAt)
                } else {
                    stringResource(payment.method.disputedByYouRes(), counterpart, disputedAt)
                },
            )
            MutedLine(stringResource(R.string.attention_payment_review_next))
        }

        payment.status.isResolved && payment.disputedAt != null && payment.resolvedAt != null -> {
            val paid = payment.status == PaymentStatus.RESOLVED_PAID
            val resolvedAt = formatAppointment(payment.resolvedAt, locale)
            if (paid) {
                PaymentConfirmedChip(stringResource(R.string.attention_payment_resolved_paid))
            } else {
                PaymentNotPaidChip()
            }
            MutedLine(
                stringResource(
                    if (paid) {
                        R.string.attention_payment_resolved_paid_line
                    } else {
                        R.string.attention_payment_resolved_not_paid_line
                    },
                    resolvedAt,
                ),
            )
            val disputedAt = formatAppointment(payment.disputedAt, locale)
            MutedLine(
                if (attention.iAmThePatient) {
                    stringResource(payment.method.disputedByProfessionalRes(), counterpart, disputedAt)
                } else {
                    stringResource(payment.method.disputedByYouRes(), counterpart, disputedAt)
                },
            )
        }
    }
}

private val PaymentStatus.isResolved: Boolean
    get() = this == PaymentStatus.RESOLVED_PAID || this == PaymentStatus.RESOLVED_NOT_PAID

@Composable
private fun MutedLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PaymentConfirmedChip(text: String) {
    val onContainer = SaludEnCasaTheme.statusColors.onPositiveContainer
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(SaludEnCasaTheme.statusColors.positiveContainer)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = onContainer,
            modifier = Modifier.size(Spacing.scale16),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = onContainer)
    }
}

// A dispute is waiting on the administrator: the tone of what is still running.
@Composable
private fun PaymentInReviewChip() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(colors.tertiaryContainer)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_schedule),
            contentDescription = null,
            tint = colors.onTertiaryContainer,
            modifier = Modifier.size(Spacing.scale16),
        )
        Text(
            text = stringResource(R.string.attention_payment_in_review),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onTertiaryContainer,
        )
    }
}

// The dispute is closed against the patient's word: a settled fact, not an
// alarm, so it takes the neutral tone of a finished record.
@Composable
private fun PaymentNotPaidChip() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(colors.surfaceVariant)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(Spacing.scale16),
        )
        Text(
            text = stringResource(R.string.attention_payment_resolved_not_paid),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnswerPaymentDialog(
    draft: AnswerDraft,
    attention: Attention,
    actions: AttentionActions,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scroll = rememberScrollState()
    val amount = formatBob(attention.finalAmountBob, locale)
    val patient = attention.counterpart.name
    val method = attention.payment.method
    val received = draft.answer == PaymentAnswer.RECEIVED
    // The error sits under the body, below the fold at a large font.
    LaunchedEffect(draft.error) {
        if (draft.error != null) scroll.animateScrollTo(scroll.maxValue)
    }

    AlertDialog(
        onDismissRequest = actions.onAnswerDismissed,
        title = {
            Text(
                text =
                    stringResource(
                        if (received) {
                            R.string.attention_receipt_dialog_title
                        } else {
                            R.string.attention_not_received_dialog_title
                        },
                    ),
            )
        },
        text = {
            // docs/decisions.md, 2026-11-04, HT-29: AlertDialog clips its content.
            Column(
                modifier = Modifier.verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
            ) {
                Text(
                    text =
                        if (received) {
                            stringResource(method.receiptBodyRes(), amount, patient)
                        } else {
                            stringResource(method.notReceivedBodyRes(), patient, amount)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                )
                draft.error?.let { error ->
                    Text(
                        text = stringResource(error.answerMessageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions.onAnswerConfirmed,
                enabled = !draft.sending,
                colors =
                    if (received) {
                        ButtonDefaults.textButtonColors()
                    } else {
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    },
            ) {
                Text(
                    text =
                        stringResource(
                            if (received) {
                                R.string.attention_receipt_confirm
                            } else {
                                R.string.attention_not_received_confirm
                            },
                        ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onAnswerDismissed, enabled = !draft.sending) {
                Text(text = stringResource(R.string.attention_pay_back))
            }
        },
    )
}

@Composable
private fun ConfirmPaymentDialog(
    draft: PaymentDraft,
    attention: Attention,
    actions: AttentionActions,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scroll = rememberScrollState()
    val patient = attention.iAmThePatient
    // The error sits under the options, below the fold at a large font.
    LaunchedEffect(draft.error) {
        if (draft.error != null) scroll.animateScrollTo(scroll.maxValue)
    }

    AlertDialog(
        onDismissRequest = actions.onPayDismissed,
        title = {
            Text(
                text =
                    stringResource(
                        if (patient) R.string.attention_pay_dialog_title else R.string.attention_receipt_dialog_title,
                    ),
            )
        },
        text = {
            // docs/decisions.md, 2026-11-04, HT-29: AlertDialog clips its content.
            Column(
                modifier = Modifier.verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
            ) {
                Text(
                    text =
                        stringResource(
                            if (patient) {
                                R.string.attention_pay_dialog_body
                            } else {
                                R.string.attention_receipt_first_dialog_body
                            },
                            formatBob(attention.finalAmountBob, locale),
                            attention.counterpart.name,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text =
                        stringResource(
                            if (patient) {
                                R.string.attention_pay_method_label
                            } else {
                                R.string.attention_receipt_method_label
                            },
                        ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
                ) {
                    PaymentMethod.declarable.forEach { method ->
                        PaymentMethodOption(
                            method = method,
                            selected = draft.method == method,
                            enabled = !draft.sending,
                            onSelect = { actions.onPaymentMethodChosen(method) },
                        )
                    }
                }
                draft.error?.let { error ->
                    Text(
                        text = stringResource(error.messageRes(patient)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onPayConfirmed, enabled = draft.canConfirm) {
                Text(
                    text =
                        stringResource(
                            if (patient) R.string.attention_pay_confirm else R.string.attention_receipt_confirm,
                        ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onPayDismissed, enabled = !draft.sending) {
                Text(text = stringResource(R.string.attention_pay_back))
            }
        },
    )
}

@Composable
private fun PaymentMethodOption(
    method: PaymentMethod,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouchTarget)
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = if (selected) Spacing.timelineStroke else Spacing.borderWidth,
                    color = if (selected) colors.primary else colors.outline,
                    shape = MaterialTheme.shapes.medium,
                ).background(if (selected) colors.primaryContainer else colors.surface)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = stringResource(method.labelRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
        )
    }
}

@Composable
private fun CancellationReasonCard(
    cancellation: AttentionCancellation,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = stringResource(R.string.attention_cancellation_reason_title),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(text = cancellation.reason, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// docs/decisions.md, 2026-11-04, HU-24: far from the thumb, but it reads as a button.
@Composable
private fun CancelAttentionButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedErrorButton(
        text = stringResource(R.string.attention_cancel_action),
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun OutlinedErrorButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = MaterialTheme.colorScheme.error
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(Spacing.borderWidth, error),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = error),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CancelAttentionDialog(
    draft: CancelDraft,
    counterpartName: String,
    actions: AttentionActions,
) {
    val scroll = rememberScrollState()
    // The error sits under the field, below the fold at a large font.
    LaunchedEffect(draft.error) {
        if (draft.error != null) scroll.animateScrollTo(scroll.maxValue)
    }

    AlertDialog(
        onDismissRequest = actions.onCancelDismissed,
        title = { Text(text = stringResource(R.string.attention_cancel_dialog_title)) },
        text = {
            // Scrolls: at 200 % font size the text, the field and the error no longer fit.
            Column(
                modifier = Modifier.verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
            ) {
                Text(
                    text = stringResource(R.string.attention_cancel_dialog_body, counterpartName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FormField(
                    label = stringResource(R.string.attention_cancellation_reason_title),
                    value = draft.reason,
                    onValueChange = actions.onCancelReasonChanged,
                    singleLine = false,
                    errorText = draft.error?.let { stringResource(it.messageRes(), CancellationReason.MAX_LENGTH) },
                    counterText =
                        stringResource(
                            R.string.attention_cancel_reason_count,
                            CancellationReason.lengthOf(draft.reason),
                            CancellationReason.MAX_LENGTH,
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions.onCancelConfirmed,
                enabled = !draft.sending,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(R.string.attention_cancel_action))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCancelDismissed, enabled = !draft.sending) {
                Text(text = stringResource(R.string.attention_cancel_back))
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

@StringRes
private fun CancelAttentionError.messageRes(): Int =
    when (this) {
        CancelAttentionError.ReasonEmpty -> {
            R.string.attention_cancel_reason_empty
        }

        CancelAttentionError.ReasonTooLong -> {
            R.string.attention_cancel_reason_too_long
        }

        is CancelAttentionError.Service -> {
            when (error) {
                ServiceError.NetworkUnavailable -> R.string.attention_cancel_failed_network
                ServiceError.AttentionChanged -> R.string.attention_mark_changed
                ServiceError.NotSignedIn, ServiceError.Unexpected -> R.string.error_unexpected
            }
        }
    }

@StringRes
private fun PaymentMethod.labelRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.payment_method_cash
        PaymentMethod.DIRECT_QR -> R.string.payment_method_direct_qr
        PaymentMethod.GATEWAY -> error("unreachable: a person never chooses GATEWAY (FA-02)")
    }

@StringRes
private fun PaymentMethod?.youPaidRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.attention_payment_you_paid_cash
        PaymentMethod.DIRECT_QR -> R.string.attention_payment_you_paid_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.attention_payment_you_paid
    }

@StringRes
private fun PaymentMethod?.patientPaidRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.attention_payment_patient_paid_cash
        PaymentMethod.DIRECT_QR -> R.string.attention_payment_patient_paid_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.attention_payment_patient_paid
    }

@StringRes
private fun PaymentMethod?.youReceivedRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.attention_payment_you_received_cash
        PaymentMethod.DIRECT_QR -> R.string.attention_payment_you_received_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.attention_payment_you_received
    }

@StringRes
private fun PaymentMethod?.professionalReceivedRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.attention_payment_professional_declared_cash
        PaymentMethod.DIRECT_QR -> R.string.attention_payment_professional_declared_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.attention_payment_professional_declared
    }

@StringRes
private fun PaymentMethod?.disputedByYouRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.attention_payment_disputed_by_you_cash
        PaymentMethod.DIRECT_QR -> R.string.attention_payment_disputed_by_you_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.attention_payment_disputed_by_you
    }

@StringRes
private fun PaymentMethod?.disputedByProfessionalRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.attention_payment_disputed_by_professional_cash
        PaymentMethod.DIRECT_QR -> R.string.attention_payment_disputed_by_professional_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.attention_payment_disputed_by_professional
    }

@StringRes
private fun PaymentMethod?.receiptBodyRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.attention_receipt_dialog_body_cash
        PaymentMethod.DIRECT_QR -> R.string.attention_receipt_dialog_body_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.attention_receipt_dialog_body
    }

@StringRes
private fun PaymentMethod?.notReceivedBodyRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.attention_not_received_dialog_body_cash
        PaymentMethod.DIRECT_QR -> R.string.attention_not_received_dialog_body_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.attention_not_received_dialog_body
    }

@StringRes
private fun ServiceError.answerMessageRes(): Int =
    when (this) {
        ServiceError.NetworkUnavailable -> R.string.attention_answer_failed_network
        ServiceError.AttentionChanged -> R.string.attention_mark_changed
        ServiceError.NotSignedIn, ServiceError.Unexpected -> R.string.error_unexpected
    }

@StringRes
private fun ServiceError.acceptMessageRes(): Int =
    when (this) {
        ServiceError.NetworkUnavailable -> R.string.attention_accept_failed_network
        ServiceError.AttentionChanged -> R.string.attention_mark_changed
        ServiceError.NotSignedIn, ServiceError.Unexpected -> R.string.error_unexpected
    }

@StringRes
private fun ConfirmPaymentError.messageRes(patient: Boolean): Int =
    when (this) {
        ConfirmPaymentError.MethodNotAllowed -> {
            R.string.error_unexpected
        }

        is ConfirmPaymentError.Service -> {
            when (error) {
                ServiceError.NetworkUnavailable -> {
                    if (patient) R.string.attention_pay_failed_network else R.string.attention_receipt_failed_network
                }

                ServiceError.AttentionChanged -> {
                    R.string.attention_mark_changed
                }

                ServiceError.NotSignedIn, ServiceError.Unexpected -> {
                    R.string.error_unexpected
                }
            }
        }
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
        conversationOpen = true,
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

private val previewCancelled =
    previewAttention.copy(
        status = ServiceStatus.CANCELLED,
        iAmThePatient = true,
        counterpart = previewForThePatient,
        conversationOpen = false,
        cancellation =
            AttentionCancellation(
                reason = PREVIEW_REASON,
                byMe = false,
                at = Instant.parse("2026-11-04T20:20:00Z"),
            ),
    )

private const val PREVIEW_REASON = "Me surgió una emergencia familiar y hoy no voy a poder llegar."

@Preview(name = "Attention cancelled", showBackground = true)
@Composable
private fun AttentionCancelledPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCancelled))
}

@Preview(name = "Attention cancelled dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionCancelledDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCancelled))
}

@Preview(name = "Attention cancelled large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionCancelledLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCancelled))
}

@Preview(name = "Attention cancelled by me", showBackground = true)
@Composable
private fun AttentionCancelledByMePreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(
            previewCancelled.copy(
                iAmThePatient = false,
                counterpart = previewAttention.counterpart,
                cancellation = previewCancelled.cancellation?.copy(byMe = true),
            ),
        ),
    )
}

@Preview(name = "Attention cancelling", showBackground = true)
@Composable
private fun AttentionCancellingPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewAttention, cancelDraft = CancelDraft(reason = PREVIEW_REASON)),
    )
}

@Preview(name = "Attention cancelling dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionCancellingDarkPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewAttention, cancelDraft = CancelDraft(reason = PREVIEW_REASON)),
    )
}

@Preview(name = "Attention cancelling large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionCancellingLargeFontPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(
            previewAttention,
            cancelDraft = CancelDraft(error = CancelAttentionError.ReasonEmpty),
        ),
    )
}

@Preview(name = "Attention failed", showBackground = true)
@Composable
private fun AttentionFailedPreview() {
    AttentionPreviewFrame(AttentionUiState.Failed(ServiceError.NetworkUnavailable))
}

private val previewPatientCompleted = previewCompleted.copy(iAmThePatient = true, counterpart = previewForThePatient)

private val previewRated =
    previewPatientCompleted.copy(
        myReview =
            MyReview(
                rating = 4,
                comment = "Muy puntual y explicó cada paso de la curación.",
                createdAt = Instant.parse("2026-11-04T16:00:00Z"),
            ),
    )

@Preview(name = "Attention patient to rate dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionPatientToRateDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted))
}

@Preview(name = "Attention patient to rate large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionPatientToRateLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted))
}

@Preview(name = "Attention patient rated", showBackground = true)
@Composable
private fun AttentionPatientRatedPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewRated))
}

@Preview(name = "Attention patient rated dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionPatientRatedDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewRated))
}

@Preview(name = "Attention patient rated large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionPatientRatedLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewRated))
}

private val previewPaidInCash =
    AttentionPayment(
        status = PaymentStatus.PATIENT_CONFIRMED,
        method = PaymentMethod.CASH,
        patientConfirmedAt = Instant.parse("2026-11-04T15:50:00Z"),
        professionalConfirmedAt = null,
    )

@Preview(name = "Attention pay dialog", showBackground = true)
@Composable
private fun AttentionPayDialogPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewPatientCompleted, paymentDraft = PaymentDraft(method = PaymentMethod.CASH)),
    )
}

@Preview(name = "Attention pay dialog dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionPayDialogDarkPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewPatientCompleted, paymentDraft = PaymentDraft(method = PaymentMethod.CASH)),
    )
}

@Preview(name = "Attention pay dialog large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionPayDialogLargeFontPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(
            previewPatientCompleted,
            paymentDraft =
                PaymentDraft(
                    method = PaymentMethod.DIRECT_QR,
                    error = ConfirmPaymentError.Service(ServiceError.NetworkUnavailable),
                ),
        ),
    )
}

@Preview(name = "Attention patient paid", showBackground = true)
@Composable
private fun AttentionPatientPaidPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewPaidInCash)))
}

@Preview(name = "Attention patient paid dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionPatientPaidDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewPaidInCash)))
}

@Preview(name = "Attention professional paid", showBackground = true)
@Composable
private fun AttentionProfessionalPaidPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewPaidInCash)))
}

@Preview(name = "Attention professional paid large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionProfessionalPaidLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewPaidInCash)))
}

private val previewBothConfirmed =
    previewPaidInCash.copy(
        status = PaymentStatus.BOTH_CONFIRMED,
        professionalConfirmedAt = Instant.parse("2026-11-04T16:10:00Z"),
    )

private val previewDisputed =
    previewPaidInCash.copy(
        status = PaymentStatus.DISPUTED,
        disputedAt = Instant.parse("2026-11-04T16:10:00Z"),
    )

private val previewProfessionalAnswering = previewCompleted.copy(payment = previewPaidInCash)

@Preview(name = "Attention professional answers", showBackground = true)
@Composable
private fun AttentionProfessionalAnswersPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewProfessionalAnswering))
}

@Preview(name = "Attention professional answers dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionProfessionalAnswersDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewProfessionalAnswering))
}

@Preview(name = "Attention professional answers large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionProfessionalAnswersLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewProfessionalAnswering))
}

@Preview(name = "Attention receipt dialog", showBackground = true)
@Composable
private fun AttentionReceiptDialogPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewProfessionalAnswering, answerDraft = AnswerDraft(PaymentAnswer.RECEIVED)),
    )
}

@Preview(name = "Attention not received dialog", showBackground = true)
@Composable
private fun AttentionNotReceivedDialogPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewProfessionalAnswering, answerDraft = AnswerDraft(PaymentAnswer.NOT_RECEIVED)),
    )
}

@Preview(name = "Attention not received dialog dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionNotReceivedDialogDarkPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewProfessionalAnswering, answerDraft = AnswerDraft(PaymentAnswer.NOT_RECEIVED)),
    )
}

@Preview(name = "Attention not received dialog large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionNotReceivedDialogLargeFontPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(
            previewProfessionalAnswering,
            answerDraft = AnswerDraft(PaymentAnswer.NOT_RECEIVED, error = ServiceError.NetworkUnavailable),
        ),
    )
}

@Preview(name = "Attention professional received", showBackground = true)
@Composable
private fun AttentionProfessionalReceivedPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewBothConfirmed)))
}

@Preview(name = "Attention professional received large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionProfessionalReceivedLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewBothConfirmed)))
}

@Preview(name = "Attention patient both confirmed", showBackground = true)
@Composable
private fun AttentionPatientBothConfirmedPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewBothConfirmed)))
}

@Preview(
    name = "Attention patient both confirmed dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AttentionPatientBothConfirmedDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewBothConfirmed)))
}

@Preview(name = "Attention patient in review", showBackground = true)
@Composable
private fun AttentionPatientInReviewPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewDisputed)))
}

@Preview(name = "Attention patient in review dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionPatientInReviewDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewDisputed)))
}

@Preview(name = "Attention patient in review large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionPatientInReviewLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewDisputed)))
}

@Preview(name = "Attention professional in review", showBackground = true)
@Composable
private fun AttentionProfessionalInReviewPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewDisputed)))
}

private val previewReceivedFirst =
    AttentionPayment(
        status = PaymentStatus.PROFESSIONAL_CONFIRMED,
        method = PaymentMethod.CASH,
        patientConfirmedAt = null,
        professionalConfirmedAt = Instant.parse("2026-11-04T15:45:00Z"),
    )

private val previewResolvedPaid =
    previewDisputed.copy(status = PaymentStatus.RESOLVED_PAID, resolvedAt = Instant.parse("2026-11-05T15:20:00Z"))

private val previewResolvedNotPaid = previewResolvedPaid.copy(status = PaymentStatus.RESOLVED_NOT_PAID)

@Preview(name = "Attention professional declares", showBackground = true)
@Composable
private fun AttentionProfessionalDeclaresPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted))
}

@Preview(name = "Attention receipt first dialog", showBackground = true)
@Composable
private fun AttentionReceiptFirstDialogPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewCompleted, paymentDraft = PaymentDraft(method = PaymentMethod.CASH)),
    )
}

@Preview(name = "Attention receipt first dialog dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionReceiptFirstDialogDarkPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(previewCompleted, paymentDraft = PaymentDraft(method = PaymentMethod.CASH)),
    )
}

@Preview(name = "Attention receipt first dialog large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionReceiptFirstDialogLargeFontPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(
            previewCompleted,
            paymentDraft =
                PaymentDraft(
                    method = PaymentMethod.DIRECT_QR,
                    error = ConfirmPaymentError.Service(ServiceError.NetworkUnavailable),
                ),
        ),
    )
}

@Preview(name = "Attention professional declared", showBackground = true)
@Composable
private fun AttentionProfessionalDeclaredPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewReceivedFirst)))
}

@Preview(name = "Attention professional declared large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionProfessionalDeclaredLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewReceivedFirst)))
}

@Preview(name = "Attention patient accepts", showBackground = true)
@Composable
private fun AttentionPatientAcceptsPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewReceivedFirst)))
}

@Preview(name = "Attention patient accepts dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionPatientAcceptsDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewReceivedFirst)))
}

@Preview(name = "Attention patient accepts large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionPatientAcceptsLargeFontPreview() {
    AttentionPreviewFrame(
        AttentionUiState.Content(
            previewPatientCompleted.copy(payment = previewReceivedFirst),
            notice = ServiceError.NetworkUnavailable,
        ),
    )
}

@Preview(name = "Attention patient resolved paid", showBackground = true)
@Composable
private fun AttentionPatientResolvedPaidPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewResolvedPaid)))
}

@Preview(name = "Attention professional resolved not paid", showBackground = true)
@Composable
private fun AttentionProfessionalResolvedNotPaidPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewResolvedNotPaid)))
}

@Preview(
    name = "Attention professional resolved not paid dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AttentionProfessionalResolvedNotPaidDarkPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewCompleted.copy(payment = previewResolvedNotPaid)))
}

@Preview(name = "Attention patient resolved not paid large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun AttentionPatientResolvedNotPaidLargeFontPreview() {
    AttentionPreviewFrame(AttentionUiState.Content(previewPatientCompleted.copy(payment = previewResolvedNotPaid)))
}
