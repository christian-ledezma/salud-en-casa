package bo.saludencasa.features.requests.presentation

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.Instant

@Composable
fun InboxScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InboxContent(
        uiState = uiState,
        onBack = onBack,
        actions =
            InboxActions(
                onReachedEnd = viewModel::onReachedEnd,
                onRetry = viewModel::onRetry,
            ),
        modifier = modifier,
    )
}

internal data class InboxActions(
    val onReachedEnd: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
internal fun InboxContent(
    uiState: InboxUiState,
    onBack: () -> Unit,
    actions: InboxActions,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        InboxUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        InboxUiState.Empty -> {
            InboxColumn(modifier = modifier) {
                ScreenHeader(title = stringResource(R.string.request_inbox_title), onBack = onBack)
                Text(
                    text = stringResource(R.string.request_inbox_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.request_inbox_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is InboxUiState.Failed -> {
            InboxColumn(modifier = modifier) {
                ScreenHeader(title = stringResource(R.string.request_inbox_title), onBack = onBack)
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
            }
        }

        is InboxUiState.Content -> {
            LazyColumn(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
                contentPadding = PaddingValues(vertical = Spacing.sectionGap),
            ) {
                item(key = "header") {
                    ScreenHeader(title = stringResource(R.string.request_inbox_title), onBack = onBack)
                }

                items(uiState.requests, key = InboxRequest::id) { request ->
                    InboxCard(request = request)
                }

                item(key = "footer") {
                    InboxFooter(
                        loadingMore = uiState.loadingMore,
                        endReached = uiState.endReached,
                        notice = uiState.notice,
                        onLoadMore = actions.onReachedEnd,
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxCard(request: InboxRequest) {
    val locale = LocalConfiguration.current.locales[0]

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            ModalityBadge(modality = request.modality)
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
                    text = stringResource(R.string.request_inbox_budget, formatBob(budget, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InboxFooter(
    loadingMore: Boolean,
    endReached: Boolean,
    notice: RequestError?,
    onLoadMore: () -> Unit,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            loadingMore -> {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }

            notice != null -> {
                Text(
                    text = stringResource(notice.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.request_inbox_load_more))
                }
            }

            !endReached -> {
                OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.request_inbox_load_more))
                }
            }
        }
    }
}

@Composable
private fun InboxColumn(
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
        content()
    }
}

private fun previewRequest(
    id: String,
    distanceM: Int,
    budget: BigDecimal? = BigDecimal("150.00"),
    scheduledAt: Instant? = null,
): InboxRequest =
    InboxRequest(
        id = id,
        serviceTypeName = "Colocación y control de vía venosa",
        city = "Cochabamba",
        description = "Necesito atención para un familiar esta tarde.",
        suggestedBudgetBob = budget,
        distanceM = distanceM,
        status = RequestStatus.PUBLISHED,
        createdAt = Instant.parse("2026-10-13T12:00:00Z"),
        modality = if (scheduledAt == null) RequestModality.IMMEDIATE else RequestModality.SCHEDULED,
        scheduledAt = scheduledAt,
    )

private fun previewContent(): InboxUiState.Content =
    InboxUiState.Content(
        requests =
            listOf(
                previewRequest("r1", distanceM = 0),
                previewRequest(
                    "r2",
                    distanceM = 1200,
                    budget = null,
                    scheduledAt = Instant.parse("2026-10-16T13:30:00Z"),
                ),
                previewRequest("r3", distanceM = 4800),
            ),
        loadingMore = false,
        endReached = true,
        notice = null,
    )

@Preview(showBackground = true, widthDp = 320, name = "Bandeja, claro")
@Composable
private fun InboxLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        InboxContent(previewContent(), onBack = {}, actions = InboxActions())
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Bandeja, oscuro")
@Composable
private fun InboxDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        InboxContent(previewContent(), onBack = {}, actions = InboxActions())
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f, name = "Bandeja, fuente 200%")
@Composable
private fun InboxLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        InboxContent(previewContent(), onBack = {}, actions = InboxActions())
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Bandeja vacía")
@Composable
private fun InboxEmptyPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        InboxContent(InboxUiState.Empty, onBack = {}, actions = InboxActions())
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Bandeja con error")
@Composable
private fun InboxFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        InboxContent(
            InboxUiState.Failed(RequestError.NetworkUnavailable),
            onBack = {},
            actions = InboxActions(),
        )
    }
}
