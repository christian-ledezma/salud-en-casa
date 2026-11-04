package bo.saludencasa.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.R
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

data class ServiceCardAmount(
    val label: String,
    val value: String,
    val agreed: Boolean,
)

// docs/decisions.md, 2026-11-04, HT-27, one card for every list.
@Composable
fun ServiceCard(
    title: String,
    statusLabel: String,
    statusTone: StatusTone,
    date: String,
    modifier: Modifier = Modifier,
    highlight: String? = null,
    counterpart: String? = null,
    distance: String? = null,
    place: String? = null,
    note: String? = null,
    amount: ServiceCardAmount? = null,
    onClick: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    // Clipped to the card's own shape, so the ripple keeps its corners.
                    if (onClick != null) {
                        Modifier.clip(CardDefaults.shape).clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            CardTitle(title = title, statusLabel = statusLabel, statusTone = statusTone)
            highlight?.let {
                Text(text = it, style = typography.labelLarge, color = colors.primary)
            }
            counterpart?.let {
                CardLine(rememberVectorPainter(Icons.Filled.Person), it, typography.labelLarge)
            }
            distance?.let {
                CardLine(painterResource(R.drawable.ic_near_me), it, typography.labelLarge)
            }
            CardLine(rememberVectorPainter(Icons.Filled.DateRange), date, typography.bodyMedium)
            place?.let {
                CardLine(
                    rememberVectorPainter(Icons.Filled.Place),
                    it,
                    typography.bodyMedium.copy(color = colors.onSurfaceVariant),
                )
            }
            note?.let {
                Text(text = it, style = typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            amount?.let { CardAmount(it) }
            footer?.invoke()
        }
    }
}

@Composable
private fun CardTitle(
    title: String,
    statusLabel: String,
    statusTone: StatusTone,
) {
    val text: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
        )
    }
    if (badgeFitsBesideTitle()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
            text(Modifier.weight(1f))
            StatusBadge(label = statusLabel, tone = statusTone)
        }
    } else {
        text(Modifier)
        StatusBadge(label = statusLabel, tone = statusTone)
    }
}

@Composable
private fun CardLine(
    icon: Painter,
    text: String,
    style: TextStyle,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.scale20),
        )
        Text(
            text = text,
            style = style,
            color = style.color.takeOrElse { MaterialTheme.colorScheme.onSurface },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CardAmount(amount: ServiceCardAmount) {
    val colors = MaterialTheme.colorScheme

    HorizontalDivider(color = colors.outlineVariant)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = amount.label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = amount.value,
            style = MaterialTheme.typography.titleMedium,
            color = if (amount.agreed) colors.primary else colors.onSurface,
        )
    }
}

@Composable
private fun ServiceCardPreviewContent(darkTheme: Boolean = false) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                ServiceCard(
                    title = "Colocación y control de vía venosa",
                    statusLabel = "En curso",
                    statusTone = StatusTone.RUNNING,
                    date = "Inició · jue, 5 nov, 10:02",
                    counterpart = "María Flores",
                    place = "Avenida América 1234, Cochabamba",
                    amount = ServiceCardAmount(label = "Acordado", value = "Bs 150", agreed = true),
                )
            }
        }
    }
}

@Preview(name = "Service card", showBackground = true, widthDp = 320)
@Composable
private fun ServiceCardPreview() {
    ServiceCardPreviewContent()
}

@Preview(name = "Service card dark", showBackground = true, widthDp = 320)
@Composable
private fun ServiceCardDarkPreview() {
    ServiceCardPreviewContent(darkTheme = true)
}

@Preview(name = "Service card large font", showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
private fun ServiceCardLargeFontPreview() {
    ServiceCardPreviewContent()
}
