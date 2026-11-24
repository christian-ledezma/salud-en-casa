package bo.saludencasa.features.disputes.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputeParty
import bo.saludencasa.features.disputes.domain.model.PaymentDispute
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.Instant

internal data class DisputeQueueActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onOpenDispute: (String) -> Unit = {},
)

@Composable
fun DisputeQueueScreen(
    onBack: () -> Unit,
    onOpenDispute: (paymentId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DisputeQueueViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    DisputeQueueContent(
        uiState = uiState,
        actions =
            DisputeQueueActions(
                onBack = onBack,
                onRetry = viewModel::load,
                onLoadMore = viewModel::onLoadMore,
                onOpenDispute = onOpenDispute,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun DisputeQueueContent(
    uiState: DisputeQueueUiState,
    actions: DisputeQueueActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(R.string.dispute_queue_title),
                    onBack = actions.onBack,
                    subtitle = stringResource(R.string.dispute_queue_subtitle),
                )
            }

            when (uiState) {
                DisputeQueueUiState.Loading -> {
                    item(key = "loading") { CenteredProgress() }
                }

                DisputeQueueUiState.Empty -> {
                    item(key = "empty") {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                            Text(
                                text = stringResource(R.string.dispute_queue_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(R.string.dispute_queue_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                is DisputeQueueUiState.Failed -> {
                    item(key = "failed") {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
                            Text(
                                text = stringResource(uiState.error.listMessageRes()),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                        }
                    }
                }

                is DisputeQueueUiState.Content -> {
                    items(uiState.disputes, key = PaymentDispute::paymentId) { dispute ->
                        DisputeRow(dispute = dispute, onClick = { actions.onOpenDispute(dispute.paymentId) })
                    }
                    item(key = "footer") {
                        Footer(content = uiState, onLoadMore = actions.onLoadMore)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisputeRow(
    dispute: PaymentDispute,
    onClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
                Text(
                    text = dispute.serviceTypeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatBob(dispute.amountBob, locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(R.string.dispute_queue_parties, dispute.patient.name, dispute.professional.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.dispute_queue_since, formatAppointment(dispute.disputedAt, locale)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Footer(
    content: DisputeQueueUiState.Content,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            content.loadingMore -> {
                CenteredProgress()
            }

            content.notice != null -> {
                Text(
                    text = stringResource(content.notice.listMessageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                LoadMoreButton(onLoadMore = onLoadMore)
            }

            !content.endReached -> {
                LoadMoreButton(onLoadMore = onLoadMore)
            }
        }
    }
}

@Composable
private fun LoadMoreButton(onLoadMore: () -> Unit) {
    OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget)) {
        Text(text = stringResource(R.string.dispute_queue_load_more))
    }
}

@Composable
private fun CenteredProgress() {
    val description = stringResource(R.string.cd_loading)
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@StringRes
private fun DisputeError.listMessageRes(): Int =
    when (this) {
        DisputeError.NetworkUnavailable -> R.string.error_network_unavailable
        DisputeError.NotSignedIn, DisputeError.AlreadyResolved, DisputeError.Unexpected -> R.string.error_unexpected
    }

internal val previewDisputes =
    listOf(
        PaymentDispute(
            paymentId = "p1",
            requestId = "r1",
            serviceTypeName = "Aplicación de inyectables",
            amountBob = BigDecimal("45.00"),
            method = PaymentMethod.DIRECT_QR,
            finishedAt = Instant.parse("2026-11-23T13:38:00Z"),
            patientConfirmedAt = Instant.parse("2026-11-23T13:40:00Z"),
            disputedAt = Instant.parse("2026-11-23T14:15:00Z"),
            patient = DisputeParty("Ana Quispe", "ana.quispe@ejemplo.com"),
            professional = DisputeParty("María Flores", "maria.flores@ejemplo.com"),
        ),
        PaymentDispute(
            paymentId = "p2",
            requestId = "r2",
            serviceTypeName = "Curación de heridas",
            amountBob = BigDecimal("95.00"),
            method = PaymentMethod.CASH,
            finishedAt = Instant.parse("2026-11-23T21:10:00Z"),
            patientConfirmedAt = Instant.parse("2026-11-23T21:15:00Z"),
            disputedAt = Instant.parse("2026-11-23T22:40:00Z"),
            patient = DisputeParty("Jorge Mamani", "jorge.mamani@ejemplo.com"),
            professional = DisputeParty("Lucía Rojas", "lucia.rojas@ejemplo.com"),
        ),
    )

@Composable
private fun DisputeQueuePreviewFrame(
    uiState: DisputeQueueUiState,
    darkTheme: Boolean = false,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        DisputeQueueContent(uiState = uiState, actions = DisputeQueueActions())
    }
}

@Preview(name = "Dispute queue", showBackground = true)
@Composable
private fun DisputeQueuePreview() {
    DisputeQueuePreviewFrame(DisputeQueueUiState.Content(previewDisputes, endReached = false))
}

@Preview(name = "Dispute queue dark", showBackground = true)
@Composable
private fun DisputeQueueDarkPreview() {
    DisputeQueuePreviewFrame(DisputeQueueUiState.Content(previewDisputes, endReached = true), darkTheme = true)
}

@Preview(name = "Dispute queue large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun DisputeQueueLargeFontPreview() {
    DisputeQueuePreviewFrame(DisputeQueueUiState.Content(previewDisputes, endReached = false))
}

@Preview(name = "Dispute queue empty", showBackground = true)
@Composable
private fun DisputeQueueEmptyPreview() {
    DisputeQueuePreviewFrame(DisputeQueueUiState.Empty)
}

@Preview(name = "Dispute queue empty large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun DisputeQueueEmptyLargeFontPreview() {
    DisputeQueuePreviewFrame(DisputeQueueUiState.Empty, darkTheme = true)
}

@Preview(name = "Dispute queue failed", showBackground = true)
@Composable
private fun DisputeQueueFailedPreview() {
    DisputeQueuePreviewFrame(DisputeQueueUiState.Failed(DisputeError.NetworkUnavailable))
}
