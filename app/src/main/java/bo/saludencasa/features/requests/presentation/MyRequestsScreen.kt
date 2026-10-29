package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.notifications.presentation.NotificationPermissionPrompt
import bo.saludencasa.features.notifications.presentation.NotificationRationale
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.Instant

@Composable
fun MyRequestsScreen(
    justCreated: Boolean,
    onBack: () -> Unit,
    onCreateRequest: () -> Unit,
    onOpenRequest: (NegotiationEntry) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyRequestsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyRequestsContent(
        uiState = uiState,
        onBack = onBack,
        actions =
            MyRequestsActions(
                onCancel = viewModel::onCancel,
                onReachedEnd = viewModel::onReachedEnd,
                onRetry = viewModel::onRetry,
                onCreateRequest = onCreateRequest,
                onOpenRequest = onOpenRequest,
            ),
        modifier = modifier,
        permissionPrompt = {
            if (justCreated) NotificationPermissionPrompt(body = R.string.notification_permission_body_patient)
        },
    )
}

internal data class MyRequestsActions(
    val onCancel: (String) -> Unit = {},
    val onReachedEnd: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCreateRequest: () -> Unit = {},
    // The card opens where the negotiation is: the waiting screen or the thread.
    val onOpenRequest: (NegotiationEntry) -> Unit = {},
)

@Composable
internal fun MyRequestsContent(
    uiState: MyRequestsUiState,
    onBack: () -> Unit,
    actions: MyRequestsActions,
    modifier: Modifier = Modifier,
    permissionPrompt: @Composable () -> Unit = {},
) {
    val loadingDescription = stringResource(R.string.cd_loading)
    val header: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale16)) {
            ScreenHeader(title = stringResource(R.string.request_list_title), onBack = onBack)
            permissionPrompt()
        }
    }

    when (uiState) {
        MyRequestsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        MyRequestsUiState.Empty -> {
            RequestsColumn(modifier = modifier) {
                header()
                Text(
                    text = stringResource(R.string.request_list_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.request_list_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text = stringResource(R.string.request_list_create_action),
                    onClick = actions.onCreateRequest,
                )
            }
        }

        is MyRequestsUiState.Failed -> {
            RequestsColumn(modifier = modifier) {
                header()
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
            }
        }

        is MyRequestsUiState.Content -> {
            LazyColumn(
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

                items(uiState.requests, key = ServiceRequest::id) { request ->
                    RequestCard(
                        request = request,
                        cancelling = uiState.cancellingId == request.id,
                        cancelEnabled = uiState.cancellingId == null,
                        onCancel = { actions.onCancel(request.id) },
                        onOpenRequest = request.negotiationEntry()?.let { entry -> { actions.onOpenRequest(entry) } },
                    )
                }

                item(key = "footer") {
                    ListFooter(
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
private fun RequestCard(
    request: ServiceRequest,
    cancelling: Boolean,
    cancelEnabled: Boolean,
    onCancel: () -> Unit,
    // Null when there is nothing to negotiate: then the card is not tappable.
    onOpenRequest: (() -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val loadingDescription = stringResource(R.string.cd_loading)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    // Clipped to the card's own shape, so the ripple keeps its corners.
                    if (onOpenRequest != null) {
                        Modifier.clip(CardDefaults.shape).clickable(onClick = onOpenRequest)
                    } else {
                        Modifier
                    },
                ),
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
            request.professionalName?.let { name ->
                Text(
                    text = stringResource(R.string.request_list_with_professional, name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(request.status.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = request.addressText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            request.suggestedBudgetBob?.let { budget ->
                Text(
                    text = stringResource(R.string.request_list_budget, formatBob(budget, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (request.status.isCancellable) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = cancelEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (cancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics { contentDescription = loadingDescription },
                        )
                    } else {
                        Text(text = stringResource(R.string.request_list_cancel_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun ListFooter(
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
                    Text(text = stringResource(R.string.request_list_load_more))
                }
            }

            !endReached -> {
                OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.request_list_load_more))
                }
            }
        }
    }
}

@Composable
private fun RequestsColumn(
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
    status: RequestStatus,
    budget: BigDecimal? = BigDecimal("150.00"),
    scheduledAt: Instant? = null,
): ServiceRequest =
    ServiceRequest(
        id = id,
        serviceTypeName = "Colocación y control de vía venosa",
        addressText = "Avenida América 1234, Cochabamba",
        city = "Cochabamba",
        status = status,
        modality = if (scheduledAt == null) RequestModality.IMMEDIATE else RequestModality.SCHEDULED,
        description = "Necesito atención para un familiar.",
        suggestedBudgetBob = budget,
        expiresAt = scheduledAt ?: Instant.parse("2026-10-12T13:00:00Z"),
        createdAt = Instant.parse("2026-10-12T12:00:00Z"),
        scheduledAt = scheduledAt,
        professionalName = "Ana Quispe".takeIf { scheduledAt != null },
    )

private fun previewContent(): MyRequestsUiState.Content =
    MyRequestsUiState.Content(
        requests =
            listOf(
                previewRequest("r1", RequestStatus.PUBLISHED),
                previewRequest("r3", RequestStatus.PUBLISHED, scheduledAt = Instant.parse("2026-10-16T13:30:00Z")),
                previewRequest("r2", RequestStatus.EXPIRED, budget = null),
            ),
        loadingMore = false,
        endReached = true,
        notice = null,
        cancellingId = null,
    )

@Composable
private fun MyRequestsPreviewFrame(
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
        body = R.string.notification_permission_body_patient,
        onEnable = {},
        onDismiss = {},
    )
}

@Preview(showBackground = true, widthDp = 320, name = "Mis solicitudes, claro")
@Composable
private fun MyRequestsLightPreview() {
    MyRequestsPreviewFrame(darkTheme = false) {
        MyRequestsContent(
            previewContent(),
            onBack = {},
            actions = MyRequestsActions(),
            permissionPrompt = { PreviewPermissionPrompt() },
        )
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Mis solicitudes, oscuro")
@Composable
private fun MyRequestsDarkPreview() {
    MyRequestsPreviewFrame(darkTheme = true) {
        MyRequestsContent(
            previewContent(),
            onBack = {},
            actions = MyRequestsActions(),
            permissionPrompt = { PreviewPermissionPrompt() },
        )
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f, name = "Mis solicitudes, fuente 200%")
@Composable
private fun MyRequestsLargeFontPreview() {
    MyRequestsPreviewFrame(darkTheme = false) {
        MyRequestsContent(
            previewContent(),
            onBack = {},
            actions = MyRequestsActions(),
            permissionPrompt = { PreviewPermissionPrompt() },
        )
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Mis solicitudes vacío")
@Composable
private fun MyRequestsEmptyPreview() {
    MyRequestsPreviewFrame(darkTheme = false) {
        MyRequestsContent(MyRequestsUiState.Empty, onBack = {}, actions = MyRequestsActions())
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Mis solicitudes con error")
@Composable
private fun MyRequestsFailedPreview() {
    MyRequestsPreviewFrame(darkTheme = false) {
        MyRequestsContent(
            MyRequestsUiState.Failed(RequestError.NetworkUnavailable),
            onBack = {},
            actions = MyRequestsActions(),
        )
    }
}
