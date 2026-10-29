package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferStatus
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.OfferableRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestOffer
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.RatingBadge
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal

// One screen per negotiation, for both sides. The professional makes the first
// offer here and stays to negotiate; the patient arrives from an offer card or a
// notification. docs/decisions.md, 2026-10-27 (HU-18 and HT-24).
@Composable
fun OfferThreadScreen(
    requestId: String,
    professionalId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfferThreadViewModel = koinViewModel { parametersOf(requestId, professionalId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OfferThreadContent(
        uiState = uiState,
        onBack = onBack,
        actions =
            OfferThreadActions(
                onCounter = viewModel::onCounterClicked,
                onCounterDismissed = viewModel::onCounterDismissed,
                onAmountChanged = viewModel::onAmountChanged,
                onMessageChanged = viewModel::onMessageChanged,
                onSubmit = viewModel::onSubmit,
                onReject = viewModel::onRejectClicked,
                onRejectConfirmed = viewModel::onRejectConfirmed,
                onRejectDismissed = viewModel::onRejectDismissed,
                onRetry = viewModel::refresh,
            ),
        modifier = modifier,
    )
}

internal data class OfferThreadActions(
    val onCounter: () -> Unit = {},
    val onCounterDismissed: () -> Unit = {},
    val onAmountChanged: (String) -> Unit = {},
    val onMessageChanged: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onReject: () -> Unit = {},
    val onRejectConfirmed: () -> Unit = {},
    val onRejectDismissed: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
internal fun OfferThreadContent(
    uiState: OfferThreadUiState,
    onBack: () -> Unit,
    actions: OfferThreadActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            OfferThreadUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            OfferThreadUiState.Gone -> {
                ThreadMessageColumn(onBack = onBack) {
                    Text(
                        text = stringResource(R.string.thread_gone_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.thread_gone_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is OfferThreadUiState.Failed -> {
                ThreadMessageColumn(onBack = onBack) {
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                }
            }

            is OfferThreadUiState.Opening -> {
                OpeningBody(opening = uiState, onBack = onBack, actions = actions)
            }

            is OfferThreadUiState.Content -> {
                ThreadBody(content = uiState, onBack = onBack, actions = actions)
            }
        }
    }
}

// The professional before their first offer: what the request is, how much they
// ask and, if they want, a message. Nothing else stands between the inbox and the
// negotiation.
@Composable
private fun OpeningBody(
    opening: OfferThreadUiState.Opening,
    onBack: () -> Unit,
    actions: OfferThreadActions,
) {
    val margin = Modifier.padding(horizontal = Spacing.screenMargin)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        ) {
            ScreenHeader(title = stringResource(R.string.offer_make_title), onBack = onBack, modifier = margin)
            RequestDetail(request = opening.request, modifier = margin)
            AmountAndMessage(
                amountLabel = stringResource(R.string.offer_amount_label),
                messageLabel = stringResource(R.string.offer_message_label),
                amount = opening.amount,
                message = opening.message,
                error = opening.error,
                floorBob = null,
                actions = actions,
                modifier = margin,
            )
        }

        BottomActionBar {
            BarError(error = opening.error)
            PrimaryButton(
                text = stringResource(R.string.offer_submit),
                onClick = actions.onSubmit,
                enabled = !opening.submitting,
            )
        }
    }
}

@Composable
private fun ThreadBody(
    content: OfferThreadUiState.Content,
    onBack: () -> Unit,
    actions: OfferThreadActions,
) {
    val thread = content.thread
    val margin = Modifier.padding(horizontal = Spacing.screenMargin)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        ) {
            ScreenHeader(title = stringResource(R.string.thread_title), onBack = onBack, modifier = margin)

            // The header is decided by which side is reading, not by whether the
            // detail could be read: the professional without it must not be
            // shown the counterpart header, which would be their own card.
            if (thread.iAmTheProfessional) {
                content.request?.let { RequestDetail(request = it, modifier = margin) }
            } else {
                CounterpartHeader(thread = thread, modifier = margin)
            }

            Column(
                modifier = margin.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
            ) {
                thread.offers.forEach { offer ->
                    OfferRow(offer = offer, mine = offer.issuer == thread.myIssuer)
                }
            }

            Text(
                text = stringResource(thread.stateRes()),
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (thread.isMyTurn) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = margin,
            )

            if (content.countering) {
                AmountAndMessage(
                    amountLabel = stringResource(R.string.offer_counter_amount_label),
                    messageLabel = stringResource(R.string.offer_counter_message_label),
                    amount = content.amount,
                    message = content.message,
                    error = content.error,
                    floorBob = thread.counterFloorBob,
                    actions = actions,
                    modifier = margin,
                )
            }
        }

        if (thread.isMyTurn) {
            BottomActionBar {
                BarError(error = content.error)

                if (content.countering) {
                    PrimaryButton(
                        text = stringResource(R.string.offer_counter_submit),
                        onClick = actions.onSubmit,
                        enabled = content.canSubmit,
                    )
                    TextButton(onClick = actions.onCounterDismissed, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.common_cancel))
                    }
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.offer_counter_open),
                        onClick = actions.onCounter,
                        enabled = content.canAnswer,
                    )
                    OutlinedButton(
                        onClick = actions.onReject,
                        enabled = content.canAnswer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.offer_reject))
                    }
                }
            }
        }
    }

    if (content.confirmingRejection) {
        AlertDialog(
            onDismissRequest = actions.onRejectDismissed,
            title = { Text(text = stringResource(R.string.offer_reject_confirm_title)) },
            text = { Text(text = stringResource(R.string.offer_reject_confirm_body)) },
            confirmButton = {
                TextButton(onClick = actions.onRejectConfirmed) {
                    Text(text = stringResource(R.string.offer_reject_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onRejectDismissed) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

// The amount and the optional message, the same for the first offer and for a
// counter-offer. The patient's floor shows under the amount before they type, so
// the refusal is never the first time they hear of it.
@Composable
private fun AmountAndMessage(
    amountLabel: String,
    messageLabel: String,
    amount: String,
    message: String,
    error: OfferError?,
    floorBob: BigDecimal?,
    actions: OfferThreadActions,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.scale16)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
            FormField(
                label = amountLabel,
                value = amount,
                onValueChange = actions.onAmountChanged,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                errorText = amountErrorText(error),
            )
            floorBob?.let { floor ->
                Text(
                    text = stringResource(R.string.offer_counter_minimum, formatBob(floor, locale)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FormField(
            label = messageLabel,
            value = message,
            onValueChange = actions.onMessageChanged,
            singleLine = false,
        )
    }
}

@Composable
private fun amountErrorText(error: OfferError?): String? {
    val locale = LocalConfiguration.current.locales[0]
    return when (error) {
        OfferError.InvalidAmount -> {
            stringResource(R.string.error_offer_invalid_amount)
        }

        is OfferError.BelowMinimum -> {
            error.minimumBob?.let { stringResource(R.string.error_offer_below_minimum, formatBob(it, locale)) }
                ?: stringResource(R.string.error_offer_below_your_last)
        }

        else -> {
            null
        }
    }
}

// The errors that belong to no field: the refusal, the network.
@Composable
private fun BarError(error: OfferError?) {
    error
        ?.takeUnless { it == OfferError.InvalidAmount || it is OfferError.BelowMinimum }
        ?.let {
            Text(
                text = stringResource(it.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
}

// What the professional is negotiating about: the inbox projection of the request,
// never its exact location or the patient's identity (RF-08.6).
@Composable
internal fun RequestDetail(
    request: OfferableRequest,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Text(
                text = request.serviceTypeName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            request.scheduledAt?.let { at ->
                Text(
                    text = stringResource(R.string.request_scheduled_for, formatAppointment(at, locale)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = inboxDistanceText(request.distanceM),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = request.city,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            request.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            request.suggestedBudgetBob?.let { budget ->
                Text(
                    text = stringResource(R.string.offer_budget_suggested, formatBob(budget, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CounterpartHeader(
    thread: OfferThread,
    modifier: Modifier = Modifier,
) {
    val name = thread.professionalName ?: stringResource(R.string.negotiation_offer_anonymous)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardAvatar(name = name, photoUrl = thread.professionalPhotoUrl)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val rating = thread.professionalRating?.toDouble()
            if (rating != null && rating > 0.0) {
                RatingBadge(rating = rating)
            } else {
                Text(
                    text = stringResource(R.string.negotiation_no_rating),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            thread.distanceM?.let { distanceM ->
                Text(
                    text = inboxDistanceText(distanceM),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Who wrote it and how much, never a role name the reader is not: the same row
// serves the patient and the professional.
@Composable
private fun OfferRow(
    offer: RequestOffer,
    mine: Boolean,
) {
    val locale = LocalConfiguration.current.locales[0]

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color =
            if (mine) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = stringResource(offer.issuer.threadLabelRes(mine)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatBob(offer.amountBob, locale),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            offer.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (offer.status == OfferStatus.REJECTED) {
                Text(
                    text = stringResource(R.string.thread_rejected_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ThreadMessageColumn(
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
        ScreenHeader(title = stringResource(R.string.thread_title), onBack = onBack)
        content()
    }
}

internal fun previewRequest() =
    OfferableRequest(
        id = "r1",
        serviceTypeName = "Colocación y control de vía venosa",
        city = "Cochabamba",
        description = "Mi padre necesita una vía para hidratación esta tarde.",
        suggestedBudgetBob = BigDecimal("150.00"),
        distanceM = 1200,
        status = RequestStatus.PUBLISHED,
        modality = RequestModality.IMMEDIATE,
        scheduledAt = null,
        alreadyOffered = false,
    )

private fun previewContent(
    countering: Boolean = false,
    closed: Boolean = false,
) = OfferThreadUiState.Content(
    thread =
        previewThreads()
            .first()
            .let { thread ->
                if (closed) {
                    thread.copy(
                        offers =
                            thread.offers.dropLast(1) + thread.offers.last().copy(status = OfferStatus.REJECTED),
                    )
                } else {
                    thread
                }
            },
    countering = countering,
    amount = if (countering) "145" else "",
)

@Composable
private fun PreviewScreen(
    uiState: OfferThreadUiState,
    darkTheme: Boolean = false,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        OfferThreadContent(uiState = uiState, onBack = {}, actions = OfferThreadActions())
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900, name = "Hilo, claro")
@Composable
private fun OfferThreadLightPreview() {
    PreviewScreen(previewContent())
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900, name = "Hilo, oscuro")
@Composable
private fun OfferThreadDarkPreview() {
    PreviewScreen(previewContent(), darkTheme = true)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1800, fontScale = 2f, name = "Hilo, 200 %")
@Composable
private fun OfferThreadLargeFontPreview() {
    PreviewScreen(previewContent())
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1000, name = "Hilo, contraofertando")
@Composable
private fun OfferThreadCounteringPreview() {
    PreviewScreen(previewContent(countering = true))
}

// The other half of the shared screen: the professional reads the request above
// the thread, and no counterpart header.
@Preview(showBackground = true, widthDp = 320, heightDp = 1000, name = "Hilo, profesional")
@Composable
private fun OfferThreadProfessionalPreview() {
    val thread = previewThreads().first()
    PreviewScreen(
        OfferThreadUiState.Content(
            thread = thread.copy(iAmTheProfessional = true),
            request = previewRequest(),
        ),
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900, name = "Hilo, cerrado")
@Composable
private fun OfferThreadClosedPreview() {
    PreviewScreen(previewContent(closed = true))
}

// The professional before the first offer, from the inbox or the notification.
@Preview(showBackground = true, widthDp = 320, heightDp = 900, name = "Hilo, primera oferta")
@Composable
private fun OfferThreadOpeningPreview() {
    PreviewScreen(OfferThreadUiState.Opening(request = previewRequest(), amount = "160"))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1800, fontScale = 2f, name = "Hilo, primera oferta, 200 %")
@Composable
private fun OfferThreadOpeningLargeFontPreview() {
    PreviewScreen(OfferThreadUiState.Opening(request = previewRequest(), amount = "160"))
}
