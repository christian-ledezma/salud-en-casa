package bo.saludencasa.features.verification.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.verification.domain.model.PendingReviewQuery
import bo.saludencasa.features.verification.domain.model.PendingReviewSubject
import bo.saludencasa.features.verification.domain.model.ReviewQueueOrder
import bo.saludencasa.features.verification.domain.model.ReviewRoleFilter
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun DocumentReviewQueueScreen(
    onBack: () -> Unit,
    onOpenReview: (profileId: String) -> Unit,
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
        onOpenReview = { subject -> onOpenReview(subject.profileId) },
        criteria =
            QueueCriteriaActions(
                onSearchChanged = viewModel::onSearchChanged,
                onRoleChanged = viewModel::onRoleChanged,
                onOrderChanged = viewModel::onOrderChanged,
                onClearFilters = viewModel::onClearFilters,
            ),
        modifier = modifier,
    )
}

internal data class QueueCriteriaActions(
    val onSearchChanged: (String) -> Unit = {},
    val onRoleChanged: (ReviewRoleFilter) -> Unit = {},
    val onOrderChanged: (ReviewQueueOrder) -> Unit = {},
    val onClearFilters: () -> Unit = {},
)

@Composable
internal fun DocumentReviewQueueContent(
    uiState: DocumentReviewQueueUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onReachedEnd: () -> Unit,
    onOpenReview: (PendingReviewSubject) -> Unit,
    criteria: QueueCriteriaActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
    ) {
        item(key = "header") {
            ScreenHeader(
                title = stringResource(R.string.review_queue_title),
                onBack = onBack,
                subtitle = stringResource(R.string.review_queue_subtitle),
            )
        }

        item(key = "criteria") {
            QueueCriteria(query = uiState.query, criteria = criteria)
        }

        when (val list = uiState.list) {
            QueueListState.Loading -> {
                item(key = "loading") { CenteredProgress() }
            }

            QueueListState.Empty -> {
                item(key = "empty") {
                    QueueEmpty(narrowed = uiState.query.isNarrowed, onClearFilters = criteria.onClearFilters)
                }
            }

            is QueueListState.Failed -> {
                item(key = "failed") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
                        Text(
                            text = stringResource(list.error.messageRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
                    }
                }
            }

            is QueueListState.Content -> {
                queueRows(list = list, onReachedEnd = onReachedEnd, onOpenReview = onOpenReview)
            }
        }
    }
}

private fun LazyListScope.queueRows(
    list: QueueListState.Content,
    onReachedEnd: () -> Unit,
    onOpenReview: (PendingReviewSubject) -> Unit,
) {
    items(list.subjects, key = { it.profileId }) { subject ->
        PendingSubjectCard(subject = subject, onClick = { onOpenReview(subject) })
    }

    if (!list.endReached) {
        item(key = "footer") {
            if (list.notice != null && !list.loadingMore) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                    Text(
                        text = stringResource(list.notice.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onReachedEnd) {
                        Text(text = stringResource(R.string.review_queue_load_more_retry))
                    }
                }
            } else {
                LaunchedEffect(list.subjects.size) { onReachedEnd() }
                CenteredProgress()
            }
        }
    }
}

@Composable
private fun QueueCriteria(
    query: PendingReviewQuery,
    criteria: QueueCriteriaActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
        FormField(
            label = stringResource(R.string.review_queue_search_label),
            value = query.search,
            onValueChange = criteria.onSearchChanged,
        )
        ChipGroup(
            label = stringResource(R.string.review_queue_filter_role_label),
            options = ReviewRoleFilter.entries,
            selected = query.role,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = criteria.onRoleChanged,
        )
        ChipGroup(
            label = stringResource(R.string.review_queue_order_label),
            options = ReviewQueueOrder.entries,
            selected = query.order,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = criteria.onOrderChanged,
        )
    }
}

// FlowRow and not SegmentedControl: at 200 % font on a 320 dp phone three
// labels sharing one row have to wrap, and a segmented control clips them
// instead (docs/decisions.md, 2026-10-08).
@Composable
private fun <T> ChipGroup(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(text = optionLabel(option)) },
                )
            }
        }
    }
}

@Composable
private fun QueueEmpty(
    narrowed: Boolean,
    onClearFilters: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
        Text(
            text =
                stringResource(
                    if (narrowed) R.string.review_queue_no_matches else R.string.review_queue_empty,
                ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (narrowed) {
            OutlinedButton(onClick = onClearFilters, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.review_queue_clear_filters))
            }
        }
    }
}

@Composable
private fun PendingSubjectCard(
    subject: PendingReviewSubject,
    onClick: () -> Unit,
) {
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
            Text(text = subject.fullName, style = MaterialTheme.typography.titleMedium)
            Text(text = subject.email, style = MaterialTheme.typography.bodyMedium)
            Text(
                text =
                    pluralStringResource(
                        R.plurals.review_queue_pending_count,
                        subject.pendingCount,
                        subject.pendingCount,
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CenteredProgress() {
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = loadingDescription },
        )
    }
}

private fun previewSubject(
    index: Int = 0,
    name: String = "Ana Quispe Mamani",
    pendingCount: Int = 3,
): PendingReviewSubject =
    PendingReviewSubject(
        profileId = "00000000-0000-0000-0000-00000000000$index",
        fullName = name,
        email = "ana.quispe@example.com",
        pendingCount = pendingCount,
        oldestPendingAt = Instant.parse("2026-10-01T15:30:00Z"),
    )

private fun previewState(
    list: QueueListState,
    query: PendingReviewQuery = PendingReviewQuery(),
): DocumentReviewQueueUiState = DocumentReviewQueueUiState(query, list)

private fun previewList(
    subjects: List<PendingReviewSubject>,
    loadingMore: Boolean = false,
    endReached: Boolean = true,
    notice: VerificationError? = null,
): QueueListState.Content = QueueListState.Content(subjects, loadingMore, endReached, notice)

private val previewSubjects =
    listOf(
        previewSubject(1),
        previewSubject(2, name = "Ernesto Arancibia", pendingCount = 5),
        previewSubject(3, name = "Noelia Tatiana Sejas", pendingCount = 1),
    )

@Preview(showBackground = true, heightDp = 900, name = "Cola por usuario, claro")
@Composable
private fun QueueLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewQueueContent(previewState(previewList(previewSubjects)), {}, {}, {}, {}, QueueCriteriaActions())
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Cola por usuario, oscuro")
@Composable
private fun QueueDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        val query = PendingReviewQuery(search = "ana", role = ReviewRoleFilter.PROFESSIONAL)
        DocumentReviewQueueContent(
            previewState(previewList(previewSubjects), query),
            {},
            {},
            {},
            {},
            QueueCriteriaActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 1800, fontScale = 2f, name = "Cola al 200 %")
@Composable
private fun QueueLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val long = "Maria del Carmen Condori de la Torre y Quispe"
        val subjects = listOf(previewSubject(1, name = long, pendingCount = 1), previewSubject(2, name = long))
        DocumentReviewQueueContent(
            previewState(previewList(subjects, endReached = false, loadingMore = true)),
            {},
            {},
            {},
            {},
            QueueCriteriaActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Cola con fallo en la pagina siguiente")
@Composable
private fun QueueNextPageFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val list = previewList(previewSubjects, endReached = false, notice = VerificationError.NetworkUnavailable)
        DocumentReviewQueueContent(previewState(list), {}, {}, {}, {}, QueueCriteriaActions())
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Cola vacia")
@Composable
private fun QueueEmptyPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewQueueContent(previewState(QueueListState.Empty), {}, {}, {}, {}, QueueCriteriaActions())
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Cola sin coincidencias")
@Composable
private fun QueueNoMatchesPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val query = PendingReviewQuery(search = "zzz", role = ReviewRoleFilter.PATIENT)
        DocumentReviewQueueContent(
            previewState(QueueListState.Empty, query),
            {},
            {},
            {},
            {},
            QueueCriteriaActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Cola con fallo")
@Composable
private fun QueueFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewQueueContent(
            previewState(QueueListState.Failed(VerificationError.NetworkUnavailable)),
            {},
            {},
            {},
            {},
            QueueCriteriaActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 700, name = "Cola cargando")
@Composable
private fun QueueLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewQueueContent(previewState(QueueListState.Loading), {}, {}, {}, {}, QueueCriteriaActions())
    }
}
