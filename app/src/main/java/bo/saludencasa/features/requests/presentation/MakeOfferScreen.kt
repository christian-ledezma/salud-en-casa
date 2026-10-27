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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import bo.saludencasa.features.requests.domain.model.OfferableRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal

// HU-17: the professional's detail and offer form. docs/decisions.md, 2026-10-23.
@Composable
fun MakeOfferScreen(
    requestId: String,
    onBack: () -> Unit,
    onSent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MakeOfferViewModel = koinViewModel { parametersOf(requestId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MakeOfferContent(
        uiState = uiState,
        onBack = onBack,
        onSent = onSent,
        actions =
            MakeOfferActions(
                onAmountChanged = viewModel::onAmountChanged,
                onMessageChanged = viewModel::onMessageChanged,
                onSubmit = viewModel::onSubmit,
                onRetry = viewModel::load,
            ),
        modifier = modifier,
    )
}

internal data class MakeOfferActions(
    val onAmountChanged: (String) -> Unit = {},
    val onMessageChanged: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
internal fun MakeOfferContent(
    uiState: MakeOfferUiState,
    onBack: () -> Unit,
    onSent: () -> Unit,
    actions: MakeOfferActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            MakeOfferUiState.Loading -> {
                CenteredProgress()
            }

            MakeOfferUiState.Gone -> {
                MessagePanel(onBack = onBack) {
                    Text(
                        text = stringResource(R.string.offer_request_gone_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.offer_request_gone_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            MakeOfferUiState.Sent -> {
                MessagePanel(onBack = onBack) {
                    Text(
                        text = stringResource(R.string.offer_sent_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.offer_sent_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryButton(text = stringResource(R.string.immediate_waiting_my_requests), onClick = onSent)
                }
            }

            is MakeOfferUiState.Failed -> {
                MessagePanel(onBack = onBack) {
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                }
            }

            is MakeOfferUiState.Content -> {
                MakeOfferForm(content = uiState, onBack = onBack, actions = actions)
            }
        }
    }
}

@Composable
private fun MakeOfferForm(
    content: MakeOfferUiState.Content,
    onBack: () -> Unit,
    actions: MakeOfferActions,
) {
    val locale = LocalConfiguration.current.locales[0]
    val request = content.request
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

            RequestDetail(request = request, modifier = margin)

            if (request.alreadyOffered) {
                Surface(
                    modifier = margin.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                    ) {
                        Text(
                            text = stringResource(R.string.offer_already_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.offer_already_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                FormField(
                    label = stringResource(R.string.offer_amount_label),
                    value = content.amount,
                    onValueChange = actions.onAmountChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    errorText =
                        stringResource(R.string.error_offer_invalid_amount)
                            .takeIf { content.error == OfferError.InvalidAmount },
                    modifier = margin,
                )
                FormField(
                    label = stringResource(R.string.offer_message_label),
                    value = content.message,
                    onValueChange = actions.onMessageChanged,
                    singleLine = false,
                    errorText =
                        stringResource(R.string.error_offer_message_required)
                            .takeIf { content.error == OfferError.MessageRequired },
                    modifier = margin,
                )
            }
        }

        if (!request.alreadyOffered) {
            BottomActionBar {
                content.error
                    ?.takeUnless { it == OfferError.InvalidAmount || it == OfferError.MessageRequired }
                    ?.let { error ->
                        Text(
                            text = stringResource(error.messageRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                PrimaryButton(
                    text = stringResource(R.string.offer_submit),
                    onClick = actions.onSubmit,
                    enabled = content.canSubmit,
                )
            }
        }
    }
}

@Composable
private fun RequestDetail(
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
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessagePanel(
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
        ScreenHeader(title = stringResource(R.string.offer_make_title), onBack = onBack)
        content()
    }
}

private fun previewRequest(alreadyOffered: Boolean = false) =
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
        alreadyOffered = alreadyOffered,
    )

@Composable
private fun PreviewScreen(
    uiState: MakeOfferUiState,
    darkTheme: Boolean = false,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        MakeOfferContent(uiState = uiState, onBack = {}, onSent = {}, actions = MakeOfferActions())
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900, name = "Hacer oferta, claro")
@Composable
private fun MakeOfferLightPreview() {
    PreviewScreen(
        MakeOfferUiState.Content(previewRequest(), amount = "140", message = "", submitting = false, error = null),
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900, name = "Hacer oferta, oscuro")
@Composable
private fun MakeOfferDarkPreview() {
    PreviewScreen(
        MakeOfferUiState.Content(
            previewRequest(),
            amount = "140",
            message = "Llevo todo el material.",
            submitting = false,
            error = null,
        ),
        darkTheme = true,
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1500, fontScale = 2f, name = "Hacer oferta, 200 %")
@Composable
private fun MakeOfferLargeFontPreview() {
    PreviewScreen(
        MakeOfferUiState.Content(previewRequest(), amount = "140", message = "", submitting = false, error = null),
    )
}

@Preview(showBackground = true, widthDp = 320, name = "Ya ofertaste")
@Composable
private fun MakeOfferAlreadyPreview() {
    PreviewScreen(
        MakeOfferUiState.Content(
            previewRequest(alreadyOffered = true),
            amount = "",
            message = "",
            submitting = false,
            error = null,
        ),
    )
}

@Preview(showBackground = true, widthDp = 320, name = "Oferta enviada")
@Composable
private fun MakeOfferSentPreview() {
    PreviewScreen(MakeOfferUiState.Sent)
}
