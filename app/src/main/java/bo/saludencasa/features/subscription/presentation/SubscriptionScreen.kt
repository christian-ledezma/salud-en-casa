package bo.saludencasa.features.subscription.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatDate
import bo.saludencasa.features.subscription.domain.model.Subscription
import bo.saludencasa.features.subscription.domain.model.SubscriptionError
import bo.saludencasa.features.subscription.domain.model.SubscriptionStatus
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SubscriptionContent(uiState = uiState, onBack = onBack, onRetry = viewModel::load, modifier = modifier)
}

@Composable
internal fun SubscriptionContent(
    uiState: SubscriptionUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale20),
        ) {
            ScreenHeader(title = stringResource(R.string.subscription_title), onBack = onBack)

            when (uiState) {
                SubscriptionUiState.Loading -> {
                    val description = stringResource(R.string.cd_loading)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
                    }
                }

                SubscriptionUiState.NotRequired -> {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                        Text(
                            text = stringResource(R.string.subscription_not_required_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(R.string.subscription_not_required_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is SubscriptionUiState.Failed -> {
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
                }

                is SubscriptionUiState.Content -> {
                    SubscriptionBody(subscription = uiState.subscription)
                }
            }
        }
    }
}

@Composable
private fun SubscriptionBody(subscription: Subscription) {
    val locale = LocalConfiguration.current.locales[0]
    val valid = subscription.isValid
    val end = formatDate(subscription.endsAt, locale)
    val status = SaludEnCasaTheme.statusColors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            SubscriptionStatusPill(status = subscription.status)
            Text(
                text =
                    stringResource(
                        if (valid) R.string.subscription_valid_until else R.string.subscription_expired_on,
                        end,
                    ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text =
                    stringResource(
                        if (valid) R.string.subscription_trial_body else R.string.subscription_expired_body,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Section(
        title =
            stringResource(
                if (valid) R.string.subscription_gains_title_valid else R.string.subscription_gains_title_expired,
            ),
    ) {
        GAINS.forEach { gain ->
            Line(
                icon = if (valid) Icons.Filled.Check else Icons.Filled.Close,
                tint = if (valid) status.positive else status.pending,
                text = stringResource(gain),
            )
        }
    }

    Section(
        title =
            stringResource(
                if (valid) R.string.subscription_keeps_title_valid else R.string.subscription_keeps_title_expired,
            ),
    ) {
        KEEPS.forEach { keep ->
            Line(icon = Icons.Filled.Check, tint = status.positive, text = stringResource(keep))
        }
    }

    Text(
        text = stringResource(R.string.subscription_purchase_soon),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun SubscriptionStatusPill(
    status: SubscriptionStatus,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, onContainer) =
        when (status) {
            SubscriptionStatus.TRIAL -> colors.secondaryContainer to colors.onSecondaryContainer
            SubscriptionStatus.EXPIRED -> colors.surfaceVariant to SaludEnCasaTheme.statusColors.pending
        }
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraLarge, color = container) {
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = onContainer,
            modifier = Modifier.padding(horizontal = Spacing.scale12, vertical = Spacing.scale4),
        )
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun Line(
    icon: ImageVector,
    tint: Color,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(Spacing.scale20))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

// RF-14.5: what a professional has while the subscription is valid.
private val GAINS =
    listOf(
        R.string.subscription_gain_search,
        R.string.subscription_gain_requests,
        R.string.subscription_gain_booking,
        R.string.subscription_gain_offers,
    )

private val KEEPS =
    listOf(
        R.string.subscription_keep_negotiations,
        R.string.subscription_keep_attentions,
    )

@StringRes
internal fun SubscriptionStatus.labelRes(): Int =
    when (this) {
        SubscriptionStatus.TRIAL -> R.string.subscription_status_trial
        SubscriptionStatus.EXPIRED -> R.string.subscription_status_expired
    }

@StringRes
private fun SubscriptionError.messageRes(): Int =
    when (this) {
        SubscriptionError.NetworkUnavailable -> {
            R.string.error_network_unavailable
        }

        SubscriptionError.NotSignedIn, SubscriptionError.UnreadableResult, SubscriptionError.Unexpected -> {
            R.string.error_unexpected
        }
    }

internal val previewTrial = Subscription(SubscriptionStatus.TRIAL, Instant.parse("2026-12-24T16:00:00Z"))
internal val previewExpired = Subscription(SubscriptionStatus.EXPIRED, Instant.parse("2026-12-24T16:00:00Z"))

@Composable
private fun SubscriptionPreviewFrame(
    uiState: SubscriptionUiState,
    darkTheme: Boolean = false,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        SubscriptionContent(uiState = uiState, onBack = {}, onRetry = {})
    }
}

@Preview(name = "Subscription in trial", showBackground = true)
@Composable
private fun SubscriptionTrialPreview() {
    SubscriptionPreviewFrame(SubscriptionUiState.Content(previewTrial))
}

@Preview(name = "Subscription in trial dark", showBackground = true)
@Composable
private fun SubscriptionTrialDarkPreview() {
    SubscriptionPreviewFrame(SubscriptionUiState.Content(previewTrial), darkTheme = true)
}

@Preview(name = "Subscription in trial large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun SubscriptionTrialLargeFontPreview() {
    SubscriptionPreviewFrame(SubscriptionUiState.Content(previewTrial))
}

@Preview(name = "Subscription expired", showBackground = true)
@Composable
private fun SubscriptionExpiredPreview() {
    SubscriptionPreviewFrame(SubscriptionUiState.Content(previewExpired))
}

@Preview(name = "Subscription expired dark", showBackground = true)
@Composable
private fun SubscriptionExpiredDarkPreview() {
    SubscriptionPreviewFrame(SubscriptionUiState.Content(previewExpired), darkTheme = true)
}

@Preview(name = "Subscription expired large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun SubscriptionExpiredLargeFontPreview() {
    SubscriptionPreviewFrame(SubscriptionUiState.Content(previewExpired))
}

@Preview(name = "Subscription not required", showBackground = true)
@Composable
private fun SubscriptionNotRequiredPreview() {
    SubscriptionPreviewFrame(SubscriptionUiState.NotRequired)
}

@Preview(name = "Subscription failed", showBackground = true)
@Composable
private fun SubscriptionFailedPreview() {
    SubscriptionPreviewFrame(SubscriptionUiState.Failed(SubscriptionError.NetworkUnavailable))
}
