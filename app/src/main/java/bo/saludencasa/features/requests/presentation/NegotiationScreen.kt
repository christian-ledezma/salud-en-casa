package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.RequestOffer
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.RatingBadge
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.time.Instant

// HU-17: the patient's read-only list of offers received, updated live.
// docs/decisions.md, 2026-10-23. HU-18 adds the counter-offer, HU-19 accepting.
@Composable
fun NegotiationScreen(
    requestId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NegotiationViewModel = koinViewModel { parametersOf(requestId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NegotiationContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
internal fun NegotiationContent(
    uiState: NegotiationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            NegotiationUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            NegotiationUiState.Empty -> {
                MessageColumn(onBack = onBack) {
                    Text(
                        text = stringResource(R.string.negotiation_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.negotiation_empty_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is NegotiationUiState.Failed -> {
                MessageColumn(onBack = onBack) {
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
                }
            }

            is NegotiationUiState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
                    contentPadding = PaddingValues(vertical = Spacing.sectionGap),
                ) {
                    item(key = "header") {
                        ScreenHeader(title = stringResource(R.string.negotiation_title), onBack = onBack)
                    }
                    items(uiState.offers, key = RequestOffer::id) { offer ->
                        OfferCard(offer = offer)
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(offer: RequestOffer) {
    val locale = LocalConfiguration.current.locales[0]
    val name = offer.professionalName ?: stringResource(R.string.negotiation_offer_anonymous)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardAvatar(name = name, photoUrl = offer.professionalPhotoUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val rating = offer.professionalRating?.toDouble()
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
                text = formatBob(offer.amountBob, locale),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            offer.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
        ScreenHeader(title = stringResource(R.string.negotiation_title), onBack = onBack)
        content()
    }
}

private fun previewOffers() =
    listOf(
        RequestOffer(
            id = "o1",
            professionalId = "p1",
            professionalName = "Ana Pérez Quiroga",
            professionalPhotoUrl = null,
            professionalRating = BigDecimal("4.80"),
            amountBob = BigDecimal("160.00"),
            message = "Llevo el material estéril y puedo llegar en una hora.",
            issuer = OfferIssuer.PROFESSIONAL,
            createdAt = Instant.parse("2026-10-23T18:10:00Z"),
        ),
        RequestOffer(
            id = "o2",
            professionalId = "p2",
            professionalName = "Luis Gutiérrez",
            professionalPhotoUrl = null,
            professionalRating = null,
            amountBob = BigDecimal("140.00"),
            message = null,
            issuer = OfferIssuer.PROFESSIONAL,
            createdAt = Instant.parse("2026-10-23T18:05:00Z"),
        ),
    )

@Composable
private fun PreviewScreen(
    uiState: NegotiationUiState,
    darkTheme: Boolean = false,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        NegotiationContent(uiState = uiState, onBack = {}, onRetry = {})
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 800, name = "Ofertas, claro")
@Composable
private fun NegotiationLightPreview() {
    PreviewScreen(NegotiationUiState.Content(previewOffers()))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 800, name = "Ofertas, oscuro")
@Composable
private fun NegotiationDarkPreview() {
    PreviewScreen(NegotiationUiState.Content(previewOffers()), darkTheme = true)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1500, fontScale = 2f, name = "Ofertas, 200 %")
@Composable
private fun NegotiationLargeFontPreview() {
    PreviewScreen(NegotiationUiState.Content(previewOffers()))
}

@Preview(showBackground = true, widthDp = 320, name = "Ofertas, vacío")
@Composable
private fun NegotiationEmptyPreview() {
    PreviewScreen(NegotiationUiState.Empty)
}

@Preview(showBackground = true, widthDp = 320, name = "Ofertas, error")
@Composable
private fun NegotiationFailedPreview() {
    PreviewScreen(NegotiationUiState.Failed(OfferError.NetworkUnavailable))
}
