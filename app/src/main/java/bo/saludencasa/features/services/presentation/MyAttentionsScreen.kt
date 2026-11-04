package bo.saludencasa.features.services.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import bo.saludencasa.core.util.AppointmentZone
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.core.util.formatFullDate
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.Instant

internal data class MyAttentionsActions(
    val onOpenAttention: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
)

@Composable
fun MyAttentionsScreen(
    onOpenAttention: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyAttentionsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    MyAttentionsContent(
        uiState = uiState,
        actions =
            MyAttentionsActions(
                onOpenAttention = onOpenAttention,
                onRetry = viewModel::load,
                onLoadMore = viewModel::onLoadMore,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun MyAttentionsContent(
    uiState: MyAttentionsUiState,
    actions: MyAttentionsActions,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        MyAttentionsUiState.Loading -> {
            val description = stringResource(R.string.cd_loading)
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
            }
        }

        MyAttentionsUiState.Empty -> {
            MessageColumn(modifier = modifier) {
                Text(
                    text = stringResource(R.string.my_attentions_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.my_attentions_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is MyAttentionsUiState.Failed -> {
            MessageColumn(modifier = modifier) {
                Text(
                    text = stringResource(uiState.error.listMessageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
            }
        }

        is MyAttentionsUiState.Content -> {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = Spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
                contentPadding = PaddingValues(vertical = Spacing.scale16),
            ) {
                items(uiState.attentions, key = AttentionSummary::requestId) { attention ->
                    AttentionCard(
                        attention = attention,
                        onClick = { actions.onOpenAttention(attention.requestId) },
                    )
                }
                item(key = "footer") {
                    Footer(content = uiState, onLoadMore = actions.onLoadMore)
                }
            }
        }
    }
}

@Composable
private fun AttentionCard(
    attention: AttentionSummary,
    onClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Text(
                text = attention.serviceTypeName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(attention.status.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (attention.status == ServiceStatus.CANCELLED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
            Text(
                text = attention.patientName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatBob(attention.finalAmountBob, locale),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    attention.scheduledAt?.let { at ->
                        stringResource(R.string.request_scheduled_for, formatAppointment(at, locale))
                    } ?: stringResource(
                        R.string.my_attentions_agreed_on,
                        formatFullDate(attention.createdAt.atZone(AppointmentZone).toLocalDate(), locale),
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Footer(
    content: MyAttentionsUiState.Content,
    onLoadMore: () -> Unit,
) {
    val description = stringResource(R.string.cd_loading)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            content.loadingMore -> {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
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
    OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.my_attentions_load_more))
    }
}

@Composable
private fun MessageColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale16),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        content()
    }
}

@StringRes
private fun ServiceError.listMessageRes(): Int =
    when (this) {
        ServiceError.NetworkUnavailable -> R.string.error_network_unavailable
        ServiceError.NotSignedIn, ServiceError.AttentionChanged, ServiceError.Unexpected -> R.string.error_unexpected
    }

private val previewAttentions =
    listOf(
        AttentionSummary(
            requestId = "r1",
            status = ServiceStatus.ASSIGNED,
            finalAmountBob = BigDecimal("150.00"),
            serviceTypeName = "Colocación y control de vía venosa",
            patientName = "María Fernanda Quispe",
            createdAt = Instant.parse("2026-11-03T13:00:00Z"),
            scheduledAt = Instant.parse("2026-11-05T14:30:00Z"),
        ),
        AttentionSummary(
            requestId = "r2",
            status = ServiceStatus.CANCELLED,
            finalAmountBob = BigDecimal("45.00"),
            serviceTypeName = "Aplicación de inyectables",
            patientName = "Jorge Mamani",
            createdAt = Instant.parse("2026-11-01T13:00:00Z"),
            scheduledAt = null,
        ),
    )

@Composable
private fun MyAttentionsPreviewFrame(uiState: MyAttentionsUiState) {
    SaludEnCasaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MyAttentionsContent(uiState = uiState, actions = MyAttentionsActions())
        }
    }
}

@Preview(name = "My attentions", showBackground = true)
@Composable
private fun MyAttentionsPreview() {
    MyAttentionsPreviewFrame(MyAttentionsUiState.Content(previewAttentions, endReached = false))
}

@Preview(
    name = "My attentions dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MyAttentionsDarkPreview() {
    MyAttentionsPreviewFrame(MyAttentionsUiState.Content(previewAttentions, endReached = true))
}

@Preview(name = "My attentions large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun MyAttentionsLargeFontPreview() {
    MyAttentionsPreviewFrame(MyAttentionsUiState.Content(previewAttentions, endReached = false))
}

@Preview(name = "My attentions empty", showBackground = true)
@Composable
private fun MyAttentionsEmptyPreview() {
    MyAttentionsPreviewFrame(MyAttentionsUiState.Empty)
}

@Preview(name = "My attentions failed", showBackground = true)
@Composable
private fun MyAttentionsFailedPreview() {
    MyAttentionsPreviewFrame(MyAttentionsUiState.Failed(ServiceError.NetworkUnavailable))
}
