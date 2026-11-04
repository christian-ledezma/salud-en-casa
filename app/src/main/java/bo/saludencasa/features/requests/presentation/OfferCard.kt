package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferStatus
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.RequestOffer
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.RatingBadge
import bo.saludencasa.ui.theme.Spacing
import java.math.BigDecimal
import java.time.Instant

// One professional's offer as the patient sees it while waiting, the way InDrive
// stacks its drivers: who, how well rated, how far, and the amount on the table,
// which is the last offer's whoever wrote it. Accepting happens here, where the
// offers are compared; tapping the rest opens the negotiation (docs/decisions.md,
// 2026-11-04, HT-28).
@Composable
internal fun OfferCard(
    thread: OfferThread,
    enabled: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val name = thread.displayName()
    val last = thread.lastOffer ?: return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier =
            modifier
                .fillMaxWidth()
                // Clipped to the card's own shape, so the ripple keeps its corners.
                .clip(CardDefaults.shape)
                .clickable(enabled = enabled, onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardAvatar(name = name, photoUrl = thread.professionalPhotoUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        val rating = thread.professionalRating?.toDouble()
                        if (rating != null && rating > 0.0) {
                            RatingBadge(rating = rating)
                        } else {
                            Text(
                                text = stringResource(R.string.negotiation_no_rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        thread.distanceM?.let { distanceM ->
                            Text(
                                text = inboxDistanceText(distanceM),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatBob(last.amountBob, locale),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(thread.stateRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (thread.isMyTurn) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            OfferCardActions(
                isMyTurn = thread.isMyTurn,
                amount = formatBob(last.amountBob, locale),
                enabled = enabled,
                onOpen = onOpen,
                onAccept = onAccept,
            )
        }
    }
}

// Side by side while the text fits; with a larger font they stack, accepting on top.
@Composable
private fun OfferCardActions(
    isMyTurn: Boolean,
    amount: String,
    enabled: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
) {
    val buttonSize = Modifier.heightIn(min = Spacing.minTouchTarget)
    val buttonPadding = PaddingValues(horizontal = Spacing.scale12, vertical = Spacing.scale8)

    val open: @Composable (Modifier) -> Unit = { width ->
        OutlinedButton(
            onClick = onOpen,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            contentPadding = buttonPadding,
            modifier = width.then(buttonSize),
        ) {
            Text(
                text = stringResource(if (isMyTurn) R.string.offer_card_negotiate else R.string.offer_card_view),
                textAlign = TextAlign.Center,
            )
        }
    }
    val accept: @Composable (Modifier) -> Unit = { width ->
        Button(
            onClick = onAccept,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            contentPadding = buttonPadding,
            modifier = width.then(buttonSize),
        ) {
            Text(text = stringResource(R.string.offer_card_accept, amount), textAlign = TextAlign.Center)
        }
    }

    when {
        !isMyTurn -> {
            open(Modifier.fillMaxWidth())
        }

        LocalDensity.current.fontScale <= 1f -> {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                open(Modifier.weight(1f))
                accept(Modifier.weight(1f))
            }
        }

        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                accept(Modifier.fillMaxWidth())
                open(Modifier.fillMaxWidth())
            }
        }
    }
}

// The confirmation of both ways of accepting: the card and the negotiation.
@Composable
internal fun AcceptOfferDialog(
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.offer_accept_confirm_title)) },
        text = { Text(text = body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.offer_accept_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.offer_accept_confirm_back))
            }
        },
    )
}

@Composable
internal fun OfferThread.displayName(): String =
    professionalName ?: stringResource(R.string.negotiation_offer_anonymous)

private fun previewOffer(
    id: String,
    professionalId: String,
    amount: String,
    issuer: OfferIssuer,
    status: OfferStatus = OfferStatus.PROPOSED,
) = RequestOffer(
    id = id,
    parentOfferId = null,
    professionalId = professionalId,
    professionalName = null,
    professionalPhotoUrl = null,
    professionalRating = null,
    amountBob = BigDecimal(amount),
    message = null,
    issuer = issuer,
    status = status,
    createdAt = Instant.parse("2026-10-27T18:10:00Z"),
)

// Shared by the previews of the waiting screen and of the thread.
internal fun previewThreads() =
    listOf(
        OfferThread(
            professionalId = "p1",
            professionalName = "Ana Pérez Quiroga",
            professionalPhotoUrl = null,
            professionalRating = BigDecimal("4.80"),
            offers =
                listOf(
                    previewOffer("o1", "p1", "160.00", OfferIssuer.PROFESSIONAL),
                    previewOffer("o2", "p1", "140.00", OfferIssuer.PATIENT),
                    previewOffer("o3", "p1", "150.00", OfferIssuer.PROFESSIONAL),
                ),
            iAmTheProfessional = false,
            distanceM = 1200,
            suggestedBudgetBob = BigDecimal("130.00"),
        ),
        OfferThread(
            professionalId = "p2",
            professionalName = "Luis Gutiérrez",
            professionalPhotoUrl = null,
            professionalRating = null,
            offers =
                listOf(
                    previewOffer("o4", "p2", "160.00", OfferIssuer.PROFESSIONAL),
                    previewOffer("o5", "p2", "140.00", OfferIssuer.PATIENT),
                ),
            iAmTheProfessional = false,
            distanceM = 3400,
            suggestedBudgetBob = BigDecimal("130.00"),
        ),
    )
