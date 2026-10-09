package bo.saludencasa.features.verification.presentation

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.PendingDocumentReview
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.util.Date

@Composable
fun DocumentReviewQueueScreen(
    onBack: () -> Unit,
    onOpenReview: (profileId: String, documentType: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentReviewQueueViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    DocumentReviewQueueContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onReachedEnd = viewModel::onReachedEnd,
        onOpenReview = { review -> onOpenReview(review.profileId, review.type.name) },
        modifier = modifier,
    )
}

@Composable
internal fun DocumentReviewQueueContent(
    uiState: DocumentReviewQueueUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onReachedEnd: () -> Unit,
    onOpenReview: (PendingDocumentReview) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        DocumentReviewQueueUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        DocumentReviewQueueUiState.Empty -> {
            CenteredColumn(modifier = modifier) {
                Text(
                    text = stringResource(R.string.review_queue_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        }

        is DocumentReviewQueueUiState.Failed -> {
            CenteredColumn(modifier = modifier) {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        }

        is DocumentReviewQueueUiState.Content -> {
            QueueList(
                content = uiState,
                onReachedEnd = onReachedEnd,
                onOpenReview = onOpenReview,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun QueueList(
    content: DocumentReviewQueueUiState.Content,
    onReachedEnd: () -> Unit,
    onOpenReview: (PendingDocumentReview) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                Text(
                    text = stringResource(R.string.review_queue_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.review_queue_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(content.reviews, key = { it.key }) { review ->
            PendingReviewRow(review = review, onClick = { onOpenReview(review) })
        }

        if (!content.endReached) {
            item(key = "footer") {
                if (content.notice != null && !content.loadingMore) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                        Text(
                            text = stringResource(content.notice.messageRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onReachedEnd) {
                            Text(text = stringResource(R.string.review_queue_load_more_retry))
                        }
                    }
                } else {
                    LaunchedEffect(content.reviews.size) { onReachedEnd() }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics { contentDescription = loadingDescription },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingReviewRow(
    review: PendingDocumentReview,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val submitted =
        remember(review.createdAt) { DateFormat.getMediumDateFormat(context).format(Date.from(review.createdAt)) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(text = review.fullName, style = MaterialTheme.typography.titleMedium)
            Text(text = review.email, style = MaterialTheme.typography.bodyMedium)
            // FlowRow for the reason documented in DocumentRow: a long title and a
            // date must wrap, not squeeze each other.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                Text(
                    text = stringResource(review.type.titleRes()),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.review_queue_item_submitted, submitted),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            review.caption?.let { caption ->
                Text(
                    text = stringResource(R.string.verification_stored_caption_prefix, caption),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun previewReview(
    name: String = "Ana Quispe Mamani",
    type: DocumentType = DocumentType.ID_FRONT,
    caption: String? = null,
): PendingDocumentReview =
    PendingDocumentReview(
        profileId = "00000000-0000-0000-0000-000000000001",
        type = type,
        caption = caption,
        createdAt = Instant.parse("2026-10-01T15:30:00Z"),
        fullName = name,
        email = "ana.quispe@example.com",
    )

private fun previewContent(
    reviews: List<PendingDocumentReview>,
    loadingMore: Boolean = false,
    endReached: Boolean = true,
    notice: VerificationError? = null,
): DocumentReviewQueueUiState.Content = DocumentReviewQueueUiState.Content(reviews, loadingMore, endReached, notice)

private val previewReviews =
    listOf(
        previewReview(),
        previewReview(type = DocumentType.DEGREE),
        previewReview(type = DocumentType.OTHER, caption = "Diploma de posgrado en enfermería"),
    )

@Preview(showBackground = true, heightDp = 700, name = "Cola con documentos, claro")
@Composable
private fun QueueLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewQueueContent(previewContent(previewReviews), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Cola con documentos, oscuro")
@Composable
private fun QueueDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        DocumentReviewQueueContent(previewContent(previewReviews), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 1400, fontScale = 2f, name = "Cola al 200 %")
@Composable
private fun QueueLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val long = "Maria del Carmen Condori de la Torre y Quispe"
        val reviews = listOf(previewReview(name = long, type = DocumentType.STUDENT_CARD), previewReview(name = long))
        DocumentReviewQueueContent(previewContent(reviews, endReached = false, loadingMore = true), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cola con fallo en la pagina siguiente")
@Composable
private fun QueueNextPageFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val content = previewContent(previewReviews, endReached = false, notice = VerificationError.NetworkUnavailable)
        DocumentReviewQueueContent(content, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cola vacia")
@Composable
private fun QueueEmptyPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewQueueContent(DocumentReviewQueueUiState.Empty, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cola con fallo")
@Composable
private fun QueueFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewQueueContent(
            DocumentReviewQueueUiState.Failed(VerificationError.NetworkUnavailable),
            {},
            {},
            {},
            {},
        )
    }
}

@Preview(showBackground = true, name = "Cola cargando")
@Composable
private fun QueueLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewQueueContent(DocumentReviewQueueUiState.Loading, {}, {}, {}, {})
    }
}
