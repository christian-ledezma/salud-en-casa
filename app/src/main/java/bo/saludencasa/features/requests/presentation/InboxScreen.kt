package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.notifications.presentation.NotificationPermissionPrompt
import bo.saludencasa.features.notifications.presentation.NotificationRationale
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.time.Instant

@Composable
fun InboxScreen(
    highlightRequestId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = koinViewModel { parametersOf(highlightRequestId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val highlight by viewModel.highlight.collectAsStateWithLifecycle()

    InboxContent(
        uiState = uiState,
        onBack = onBack,
        actions =
            InboxActions(
                onReachedEnd = viewModel::onReachedEnd,
                onRetry = viewModel::onRetry,
                onDismissUnavailable = viewModel::onDismissUnavailable,
            ),
        modifier = modifier,
        highlight = highlight,
        permissionPrompt = {
            NotificationPermissionPrompt(body = R.string.notification_permission_body_professional)
        },
    )
}

internal data class InboxActions(
    val onReachedEnd: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onDismissUnavailable: () -> Unit = {},
)

@Composable
internal fun InboxContent(
    uiState: InboxUiState,
    onBack: () -> Unit,
    actions: InboxActions,
    modifier: Modifier = Modifier,
    highlight: InboxHighlight = InboxHighlight.None,
    permissionPrompt: @Composable () -> Unit = {},
) {
    val loadingDescription = stringResource(R.string.cd_loading)
    val header: @Composable () -> Unit = {
        InboxHeader(
            onBack = onBack,
            unavailable = highlight == InboxHighlight.Unavailable,
            onDismissUnavailable = actions.onDismissUnavailable,
            permissionPrompt = permissionPrompt,
        )
    }

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
                header()
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
                header()
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
            }
        }

        is InboxUiState.Content -> {
            val listState = rememberLazyListState()
            val highlightedId = (highlight as? InboxHighlight.Row)?.requestId

            // The header is the one item before the first request.
            LaunchedEffect(highlightedId) {
                val index = uiState.requests.indexOfFirst { it.id == highlightedId }
                if (index >= 0) listState.scrollToItem(index + 1)
            }

            LazyColumn(
                state = listState,
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
                contentPadding = PaddingValues(vertical = Spacing.sectionGap),
            ) {
                item(key = "header") {
                    header()
                }

                items(uiState.requests, key = InboxRequest::id) { request ->
                    InboxCard(request = request, highlighted = request.id == highlightedId)
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
private fun InboxHeader(
    onBack: () -> Unit,
    unavailable: Boolean,
    onDismissUnavailable: () -> Unit,
    permissionPrompt: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale16)) {
        ScreenHeader(title = stringResource(R.string.request_inbox_title), onBack = onBack)
        permissionPrompt()
        if (unavailable) {
            UnavailableNotice(onDismiss = onDismissUnavailable)
        }
    }
}

@Composable
private fun UnavailableNotice(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            Text(
                text = stringResource(R.string.request_inbox_request_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.common_accept))
            }
        }
    }
}

@Composable
private fun InboxCard(
    request: InboxRequest,
    highlighted: Boolean,
) {
    val locale = LocalConfiguration.current.locales[0]
    val notifiedDescription = stringResource(R.string.cd_inbox_notified_request)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (highlighted) Modifier.semantics { stateDescription = notifiedDescription } else Modifier),
        border =
            if (highlighted) {
                BorderStroke(Spacing.highlightBorderWidth, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
    ) {
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

@Composable
private fun InboxPreviewFrame(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) { content() }
    }
}

@Composable
private fun PreviewPermissionPrompt() {
    NotificationRationale(
        body = R.string.notification_permission_body_professional,
        onEnable = {},
        onDismiss = {},
    )
}

@Preview(showBackground = true, widthDp = 320, name = "Bandeja, claro")
@Composable
private fun InboxLightPreview() {
    InboxPreviewFrame(darkTheme = false) {
        InboxContent(
            previewContent(),
            onBack = {},
            actions = InboxActions(),
            highlight = InboxHighlight.Row("r2"),
            permissionPrompt = { PreviewPermissionPrompt() },
        )
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Bandeja, oscuro")
@Composable
private fun InboxDarkPreview() {
    InboxPreviewFrame(darkTheme = true) {
        InboxContent(
            previewContent(),
            onBack = {},
            actions = InboxActions(),
            highlight = InboxHighlight.Row("r2"),
            permissionPrompt = { PreviewPermissionPrompt() },
        )
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f, name = "Bandeja, fuente 200%")
@Composable
private fun InboxLargeFontPreview() {
    InboxPreviewFrame(darkTheme = false) {
        InboxContent(
            previewContent(),
            onBack = {},
            actions = InboxActions(),
            highlight = InboxHighlight.Row("r2"),
            permissionPrompt = { PreviewPermissionPrompt() },
        )
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Bandeja, la solicitud del aviso ya no está")
@Composable
private fun InboxUnavailablePreview() {
    InboxPreviewFrame(darkTheme = false) {
        InboxContent(
            previewContent(),
            onBack = {},
            actions = InboxActions(),
            highlight = InboxHighlight.Unavailable,
        )
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Bandeja vacía")
@Composable
private fun InboxEmptyPreview() {
    InboxPreviewFrame(darkTheme = false) {
        InboxContent(InboxUiState.Empty, onBack = {}, actions = InboxActions())
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Bandeja con error")
@Composable
private fun InboxFailedPreview() {
    InboxPreviewFrame(darkTheme = false) {
        InboxContent(
            InboxUiState.Failed(RequestError.NetworkUnavailable),
            onBack = {},
            actions = InboxActions(),
        )
    }
}
