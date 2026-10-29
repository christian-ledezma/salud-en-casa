package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
// which is the last offer's whoever wrote it. Tapping it opens that negotiation
// (docs/decisions.md, 2026-10-27, HT-24). Stacked rather than in columns, so a
// long name and a large font still fit on a narrow phone.
@Composable
internal fun OfferCard(
    thread: OfferThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val name = thread.professionalName ?: stringResource(R.string.negotiation_offer_anonymous)
    val last = thread.lastOffer ?: return

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
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
                }
            }
            Text(
                text = formatBob(last.amountBob, locale),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            thread.distanceM?.let { distanceM ->
                Text(
                    text = inboxDistanceText(distanceM),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
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
            if (thread.offers.size > 1) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.negotiation_thread_rounds,
                            thread.offers.size,
                            thread.offers.size,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

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
            offers = listOf(previewOffer("o4", "p2", "140.00", OfferIssuer.PROFESSIONAL)),
            iAmTheProfessional = false,
            distanceM = 3400,
            suggestedBudgetBob = BigDecimal("130.00"),
        ),
    )
