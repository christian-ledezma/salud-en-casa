package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// docs/design-system.md, section 5: horizontal row of days; the selected day
// is a filled circle in primary; unavailable days are dimmed and inert.
@Composable
fun DatePickerRow(
    days: List<LocalDate>,
    selected: LocalDate?,
    onSelectedChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isAvailable: (LocalDate) -> Boolean = { true },
) {
    val locale = LocalConfiguration.current.locales[0]

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
    ) {
        items(items = days, key = { it.toEpochDay() }) { day ->
            val available = isAvailable(day)
            val isSelected = day == selected
            val circleColor =
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val contentColor =
                when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    available -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

            Column(
                modifier =
                    Modifier
                        .size(Spacing.minTouchTarget)
                        .clip(CircleShape)
                        .background(circleColor)
                        .then(
                            if (available) {
                                Modifier.clickable { onSelectedChange(day) }
                            } else {
                                Modifier
                            },
                        ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        day.dayOfWeek
                            .getDisplayName(TextStyle.SHORT, locale)
                            .take(1)
                            .uppercase(locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun previewWeek() = (0..6L).map { LocalDate.now().plusDays(it) }

@Preview(showBackground = true)
@Composable
private fun DatePickerRowLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val week = previewWeek()
        DatePickerRow(
            days = week,
            selected = week[2],
            onSelectedChange = {},
            isAvailable = { it != week[4] },
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DatePickerRowDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        val week = previewWeek()
        DatePickerRow(
            days = week,
            selected = week[2],
            onSelectedChange = {},
            isAvailable = { it != week[4] },
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}
