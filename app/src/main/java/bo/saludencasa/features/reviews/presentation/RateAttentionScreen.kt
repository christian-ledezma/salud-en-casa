package bo.saludencasa.features.reviews.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.model.SubmitReviewError
import bo.saludencasa.features.reviews.domain.vo.ReviewComment
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionPlace
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.components.StarPicker
import bo.saludencasa.ui.components.StarsRead
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.time.Instant

class RateAttentionActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRatingChanged: (Int) -> Unit = {},
    val onCommentChanged: (String) -> Unit = {},
    val onSubmitRequested: () -> Unit = {},
    val onConfirmDismissed: () -> Unit = {},
    val onConfirmed: () -> Unit = {},
)

// docs/decisions.md, 2026-11-26, HU-29.
@Composable
fun RateAttentionScreen(
    requestId: String,
    rating: Int,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RateAttentionViewModel = koinViewModel { parametersOf(requestId, rating) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is RateAttentionUiState.Submitted) onSubmitted()
    }

    RateAttentionContent(
        uiState = uiState,
        actions =
            RateAttentionActions(
                onBack = onBack,
                onRetry = viewModel::refresh,
                onRatingChanged = viewModel::onRatingChanged,
                onCommentChanged = viewModel::onCommentChanged,
                onSubmitRequested = viewModel::onSubmitRequested,
                onConfirmDismissed = viewModel::onConfirmDismissed,
                onConfirmed = viewModel::onConfirmed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun RateAttentionContent(
    uiState: RateAttentionUiState,
    actions: RateAttentionActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            // Submitted leaves the screen at once, so what it draws is the screen it
            // is leaving rather than an empty frame.
            RateAttentionUiState.Loading, RateAttentionUiState.Submitted -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is RateAttentionUiState.Unavailable -> {
                Unavailable(error = uiState.error, actions = actions)
            }

            is RateAttentionUiState.Content -> {
                RateForm(content = uiState, actions = actions)
            }
        }
    }
}

@Composable
private fun Unavailable(
    error: ReviewError,
    actions: RateAttentionActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin)
                .padding(vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        ScreenHeader(title = stringResource(R.string.review_rate_title), onBack = actions.onBack)
        Text(
            text = stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        // Only a failure to read can come out different the next time.
        if (error == ReviewError.NetworkUnavailable || error == ReviewError.Unexpected) {
            PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
        }
    }
}

@Composable
private fun RateForm(
    content: RateAttentionUiState.Content,
    actions: RateAttentionActions,
) {
    val attention = content.attention
    val locale = LocalConfiguration.current.locales[0]
    val name = attention.counterpart.name

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenMargin)
                    .padding(vertical = Spacing.scale16),
            verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap),
        ) {
            ScreenHeader(title = stringResource(R.string.review_rate_title), onBack = actions.onBack)

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardAvatar(name = name, photoUrl = attention.counterpart.photoUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.review_attention_meta,
                                attention.serviceTypeName,
                                formatAppointment(attention.finishedAt ?: attention.createdAt, locale),
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
                ) {
                    Text(
                        text = stringResource(R.string.review_rate_question),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    StarPicker(rating = content.rating, onPick = actions.onRatingChanged)
                    if (content.rating > 0) {
                        Text(
                            text = stringResource(content.rating.labelRes()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            FormField(
                label = stringResource(R.string.review_comment_label),
                value = content.comment,
                onValueChange = actions.onCommentChanged,
                singleLine = false,
                errorText = content.error?.let { stringResource(it.messageRes(), ReviewComment.MAX_LENGTH) },
                counterText =
                    stringResource(
                        R.string.review_comment_count,
                        ReviewComment.lengthOf(content.comment),
                        ReviewComment.MAX_LENGTH,
                    ),
            )

            Text(
                text = stringResource(R.string.review_rate_notice, name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BottomActionBar {
            PrimaryButton(
                text = stringResource(R.string.review_submit_action),
                onClick = actions.onSubmitRequested,
                enabled = content.canSubmit,
            )
        }
    }

    if (content.confirming) {
        ConfirmDialog(content = content, actions = actions)
    }
}

@Composable
private fun ConfirmDialog(
    content: RateAttentionUiState.Content,
    actions: RateAttentionActions,
) {
    val name = content.attention.counterpart.name
    val hasComment = content.comment.isNotBlank()

    AlertDialog(
        onDismissRequest = actions.onConfirmDismissed,
        title = { Text(text = stringResource(R.string.review_confirm_title)) },
        // Scrolls: at 200 % font size the stars and the longest body no longer fit
        // a small phone.
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
            ) {
                StarsRead(rating = content.rating)
                Text(
                    text =
                        pluralStringResource(
                            if (hasComment) {
                                R.plurals.review_confirm_body_with_comment
                            } else {
                                R.plurals.review_confirm_body_without_comment
                            },
                            content.rating,
                            content.rating,
                            name,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirmed) {
                Text(text = stringResource(R.string.review_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onConfirmDismissed) {
                Text(text = stringResource(R.string.review_confirm_back))
            }
        },
    )
}

private fun Int.labelRes(): Int =
    when (this) {
        1 -> R.string.review_rating_1
        2 -> R.string.review_rating_2
        3 -> R.string.review_rating_3
        4 -> R.string.review_rating_4
        else -> R.string.review_rating_5
    }

private val previewAttention =
    Attention(
        serviceId = "s1",
        requestId = "r1",
        status = ServiceStatus.COMPLETED,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        counterpart = Counterpart(id = "p1", name = "Ana Pérez Quiroga", photoUrl = null),
        iAmThePatient = true,
        createdAt = Instant.parse("2026-11-25T13:00:00Z"),
        scheduledAt = null,
        startedAt = Instant.parse("2026-11-25T14:02:00Z"),
        finishedAt = Instant.parse("2026-11-25T15:40:00Z"),
        description = null,
        place =
            AttentionPlace(
                city = "La Paz",
                addressText = null,
                reference = null,
                latitude = null,
                longitude = null,
            ),
        conversationOpen = true,
    )

private const val PREVIEW_COMMENT = "Muy puntual y explicó cada paso de la curación."

@Composable
private fun RatePreviewFrame(uiState: RateAttentionUiState) {
    SaludEnCasaTheme {
        RateAttentionContent(uiState = uiState, actions = RateAttentionActions())
    }
}

@Preview(name = "Rate", showBackground = true)
@Composable
private fun RatePreview() {
    RatePreviewFrame(
        RateAttentionUiState.Content(attention = previewAttention, rating = 4, comment = PREVIEW_COMMENT),
    )
}

@Preview(name = "Rate dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RateDarkPreview() {
    RatePreviewFrame(
        RateAttentionUiState.Content(attention = previewAttention, rating = 4, comment = PREVIEW_COMMENT),
    )
}

@Preview(name = "Rate large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun RateLargeFontPreview() {
    RatePreviewFrame(
        RateAttentionUiState.Content(attention = previewAttention, rating = 4, comment = PREVIEW_COMMENT),
    )
}

@Preview(name = "Rate without stars", showBackground = true)
@Composable
private fun RateWithoutStarsPreview() {
    RatePreviewFrame(RateAttentionUiState.Content(attention = previewAttention, rating = 0))
}

@Preview(name = "Rate comment too long", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun RateCommentTooLongPreview() {
    RatePreviewFrame(
        RateAttentionUiState.Content(
            attention = previewAttention,
            rating = 5,
            comment = "x".repeat(ReviewComment.MAX_LENGTH + 1),
            error = SubmitReviewError.CommentTooLong,
        ),
    )
}

@Preview(name = "Rate confirming", showBackground = true)
@Composable
private fun RateConfirmingPreview() {
    RatePreviewFrame(
        RateAttentionUiState.Content(
            attention = previewAttention,
            rating = 4,
            comment = PREVIEW_COMMENT,
            confirming = true,
        ),
    )
}

@Preview(name = "Rate unavailable", showBackground = true)
@Composable
private fun RateUnavailablePreview() {
    RatePreviewFrame(RateAttentionUiState.Unavailable(ReviewError.AlreadyRated))
}

@Preview(name = "Rate unavailable large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun RateUnavailableLargeFontPreview() {
    RatePreviewFrame(RateAttentionUiState.Unavailable(ReviewError.NetworkUnavailable))
}
