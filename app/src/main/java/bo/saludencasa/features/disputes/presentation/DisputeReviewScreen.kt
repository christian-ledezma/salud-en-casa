package bo.saludencasa.features.disputes.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.disputes.domain.model.DisputeError
import bo.saludencasa.features.disputes.domain.model.DisputeParty
import bo.saludencasa.features.disputes.domain.model.DisputeResolution
import bo.saludencasa.features.disputes.domain.model.PaymentDispute
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

internal data class DisputeReviewActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onResolveRequested: (DisputeResolution) -> Unit = {},
    val onResolveDismissed: () -> Unit = {},
    val onResolveConfirmed: () -> Unit = {},
)

@Composable
fun DisputeReviewScreen(
    paymentId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DisputeReviewViewModel = koinViewModel { parametersOf(paymentId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Resolved: back to the list, which reads itself again on resume.
    val resolved = (uiState as? DisputeReviewUiState.Content)?.resolved == true
    LaunchedEffect(resolved) {
        if (resolved) onBack()
    }

    DisputeReviewContent(
        uiState = uiState,
        actions =
            DisputeReviewActions(
                onBack = onBack,
                onRetry = viewModel::load,
                onResolveRequested = viewModel::onResolveRequested,
                onResolveDismissed = viewModel::onResolveDismissed,
                onResolveConfirmed = viewModel::onResolveConfirmed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun DisputeReviewContent(
    uiState: DisputeReviewUiState,
    actions: DisputeReviewActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            DisputeReviewUiState.Loading -> {
                val description = stringResource(R.string.cd_loading)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
                }
            }

            DisputeReviewUiState.Gone -> {
                MessageColumn(onBack = actions.onBack) {
                    Text(
                        text = stringResource(R.string.dispute_review_gone_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.dispute_review_gone_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is DisputeReviewUiState.Failed -> {
                MessageColumn(onBack = actions.onBack) {
                    Text(
                        text = stringResource(uiState.error.loadMessageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                }
            }

            is DisputeReviewUiState.Content -> {
                DisputeBody(content = uiState, actions = actions)
            }
        }
    }
}

@Composable
private fun DisputeBody(
    content: DisputeReviewUiState.Content,
    actions: DisputeReviewActions,
) {
    val dispute = content.dispute
    val locale = LocalConfiguration.current.locales[0]
    val margin = Modifier.padding(horizontal = Spacing.screenMargin)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        ) {
            ScreenHeader(
                title = stringResource(R.string.dispute_review_title),
                onBack = actions.onBack,
                modifier = margin,
            )

            Column(modifier = margin, verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                Text(
                    text = formatBob(dispute.amountBob, locale),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = dispute.serviceTypeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                dispute.finishedAt?.let { finishedAt ->
                    Text(
                        text = stringResource(R.string.dispute_review_completed, formatAppointment(finishedAt, locale)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(modifier = margin) {
                Text(
                    text = stringResource(R.string.dispute_review_declarations_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                DeclarationLine(
                    icon = Icons.Filled.Check,
                    tint = SaludEnCasaTheme.statusColors.positive,
                    text =
                        stringResource(
                            dispute.method.patientPaidRes(),
                            dispute.patient.name,
                            formatAppointment(dispute.patientConfirmedAt, locale),
                        ),
                )
                DeclarationLine(
                    icon = Icons.Filled.Close,
                    tint = MaterialTheme.colorScheme.error,
                    text =
                        stringResource(
                            R.string.dispute_review_professional_denied,
                            dispute.professional.name,
                            formatAppointment(dispute.disputedAt, locale),
                        ),
                )
            }

            Card(modifier = margin) {
                PartyBlock(label = stringResource(R.string.dispute_review_patient_label), party = dispute.patient)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PartyBlock(
                    label = stringResource(R.string.dispute_review_professional_label),
                    party = dispute.professional,
                )
            }

            Text(
                text = stringResource(R.string.dispute_review_contact_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = margin,
            )
        }

        // docs/decisions.md, 2026-11-24, HU-28: neither answer stands out, so the
        // screen does not lean the administrator towards one.
        BottomActionBar {
            ResolveButton(
                text = stringResource(R.string.dispute_review_paid_action),
                onClick = { actions.onResolveRequested(DisputeResolution.PAID) },
            )
            ResolveButton(
                text = stringResource(R.string.dispute_review_not_paid_action),
                onClick = { actions.onResolveRequested(DisputeResolution.NOT_PAID) },
            )
        }
    }

    content.draft?.let { draft ->
        ResolveDialog(draft = draft, dispute = dispute, actions = actions)
    }
}

@Composable
private fun Card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            content()
        }
    }
}

@Composable
private fun DeclarationLine(
    icon: ImageVector,
    tint: Color,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(Spacing.scale20))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PartyBlock(
    label: String,
    party: DisputeParty,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = party.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = party.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResolveButton(
    text: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ResolveDialog(
    draft: ResolveDraft,
    dispute: PaymentDispute,
    actions: DisputeReviewActions,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scroll = rememberScrollState()
    val paid = draft.resolution == DisputeResolution.PAID
    // The error sits under the body, below the fold at a large font.
    LaunchedEffect(draft.error) {
        if (draft.error != null) scroll.animateScrollTo(scroll.maxValue)
    }

    AlertDialog(
        onDismissRequest = actions.onResolveDismissed,
        title = {
            Text(
                text =
                    stringResource(
                        if (paid) {
                            R.string.dispute_review_paid_dialog_title
                        } else {
                            R.string.dispute_review_not_paid_dialog_title
                        },
                    ),
            )
        },
        text = {
            // docs/decisions.md, 2026-11-04, HT-29: AlertDialog clips its content.
            Column(
                modifier = Modifier.verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
            ) {
                Text(
                    text =
                        stringResource(
                            if (paid) {
                                R.string.dispute_review_paid_dialog_body
                            } else {
                                R.string.dispute_review_not_paid_dialog_body
                            },
                            formatBob(dispute.amountBob, locale),
                            dispute.patient.name,
                            dispute.professional.name,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                draft.error?.let { error ->
                    Text(
                        text = stringResource(error.resolveMessageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onResolveConfirmed, enabled = !draft.sending) {
                Text(
                    text =
                        stringResource(
                            if (paid) {
                                R.string.dispute_review_paid_confirm
                            } else {
                                R.string.dispute_review_not_paid_confirm
                            },
                        ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onResolveDismissed, enabled = !draft.sending) {
                Text(text = stringResource(R.string.dispute_review_back))
            }
        },
    )
}

@Composable
private fun MessageColumn(
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
        ScreenHeader(title = stringResource(R.string.dispute_review_title), onBack = onBack)
        content()
    }
}

@StringRes
private fun PaymentMethod?.patientPaidRes(): Int =
    when (this) {
        PaymentMethod.CASH -> R.string.dispute_review_patient_paid_cash
        PaymentMethod.DIRECT_QR -> R.string.dispute_review_patient_paid_direct_qr
        PaymentMethod.GATEWAY, null -> R.string.dispute_review_patient_paid
    }

@StringRes
private fun DisputeError.loadMessageRes(): Int =
    when (this) {
        DisputeError.NetworkUnavailable -> R.string.error_network_unavailable
        DisputeError.NotSignedIn, DisputeError.AlreadyResolved, DisputeError.Unexpected -> R.string.error_unexpected
    }

@StringRes
private fun DisputeError.resolveMessageRes(): Int =
    when (this) {
        DisputeError.NetworkUnavailable -> R.string.dispute_review_failed_network
        DisputeError.NotSignedIn, DisputeError.AlreadyResolved, DisputeError.Unexpected -> R.string.error_unexpected
    }

@Composable
private fun DisputeReviewPreviewFrame(
    uiState: DisputeReviewUiState,
    darkTheme: Boolean = false,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        DisputeReviewContent(uiState = uiState, actions = DisputeReviewActions())
    }
}

private val previewDispute = previewDisputes.first()

@Preview(name = "Dispute review", showBackground = true)
@Composable
private fun DisputeReviewPreview() {
    DisputeReviewPreviewFrame(DisputeReviewUiState.Content(previewDispute))
}

@Preview(name = "Dispute review dark", showBackground = true)
@Composable
private fun DisputeReviewDarkPreview() {
    DisputeReviewPreviewFrame(DisputeReviewUiState.Content(previewDispute), darkTheme = true)
}

@Preview(name = "Dispute review large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun DisputeReviewLargeFontPreview() {
    DisputeReviewPreviewFrame(DisputeReviewUiState.Content(previewDispute))
}

@Preview(name = "Dispute review paid dialog", showBackground = true)
@Composable
private fun DisputeReviewPaidDialogPreview() {
    DisputeReviewPreviewFrame(
        DisputeReviewUiState.Content(previewDispute, draft = ResolveDraft(DisputeResolution.PAID)),
    )
}

@Preview(name = "Dispute review not paid dialog dark", showBackground = true)
@Composable
private fun DisputeReviewNotPaidDialogDarkPreview() {
    DisputeReviewPreviewFrame(
        DisputeReviewUiState.Content(previewDispute, draft = ResolveDraft(DisputeResolution.NOT_PAID)),
        darkTheme = true,
    )
}

@Preview(name = "Dispute review failed dialog large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun DisputeReviewFailedDialogLargeFontPreview() {
    DisputeReviewPreviewFrame(
        DisputeReviewUiState.Content(
            previewDispute,
            draft = ResolveDraft(DisputeResolution.NOT_PAID, error = DisputeError.NetworkUnavailable),
        ),
    )
}

@Preview(name = "Dispute review gone", showBackground = true)
@Composable
private fun DisputeReviewGonePreview() {
    DisputeReviewPreviewFrame(DisputeReviewUiState.Gone)
}
