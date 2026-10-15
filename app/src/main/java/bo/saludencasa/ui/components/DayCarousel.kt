package bo.saludencasa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.R
import bo.saludencasa.core.util.formatDayOfMonth
import bo.saludencasa.core.util.formatFullDate
import bo.saludencasa.core.util.formatMonthShort
import bo.saludencasa.core.util.formatWeekdayShort
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import java.time.LocalDate

// docs/design-system.md, section 5, the date and time carousel.
@Composable
fun DayCarousel(
    days: List<LocalDate>,
    selected: LocalDate?,
    onSelectedChange: (LocalDate) -> Unit,
    isAvailable: (LocalDate) -> Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state = rememberLazyListState()

    LaunchedEffect(selected) {
        val index = days.indexOf(selected)
        if (index >= 0 && state.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            state.scrollToItem(index)
        }
    }

    LazyRow(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        modifier = modifier.selectableGroup(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        items(items = days, key = { it.toEpochDay() }) { day ->
            DayCard(
                day = day,
                selected = day == selected,
                available = isAvailable(day),
                onClick = { onSelectedChange(day) },
            )
        }
    }
}

@Composable
private fun DayCard(
    day: LocalDate,
    selected: Boolean,
    available: Boolean,
    onClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val fullDate = formatFullDate(day, locale)
    val withoutTimes = stringResource(R.string.cd_day_without_times)
    val colors = MaterialTheme.colorScheme
    val container = if (selected) colors.primary else colors.surface
    val content =
        when {
            selected -> colors.onPrimary
            available -> colors.onSurface
            else -> colors.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
        }
    val secondary =
        when {
            selected -> colors.onPrimary
            available -> colors.onSurfaceVariant
            else -> content
        }

    Surface(
        modifier =
            Modifier
                .widthIn(min = Spacing.dayCardMinWidth)
                .heightIn(min = Spacing.minTouchTarget)
                .selectable(selected = selected, enabled = available, role = Role.RadioButton, onClick = onClick)
                .semantics {
                    contentDescription = fullDate
                    if (!available) stateDescription = withoutTimes
                },
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = if (selected) null else BorderStroke(Spacing.borderWidth, colors.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8)
                    .clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = formatWeekdayShort(day, locale),
                style = MaterialTheme.typography.labelMedium,
                color = secondary,
            )
            Text(text = formatDayOfMonth(day, locale), style = MaterialTheme.typography.titleLarge, color = content)
            Text(text = formatMonthShort(day, locale), style = MaterialTheme.typography.labelMedium, color = secondary)
        }
    }
}

// Material's disabled-content convention.
private const val DISABLED_CONTENT_ALPHA = 0.38f

private fun previewDays(): List<LocalDate> = (0..9L).map { LocalDate.of(2026, 10, 15).plusDays(it) }

@Preview(showBackground = true)
@Composable
private fun DayCarouselLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val days = previewDays()
        DayCarousel(
            days = days,
            selected = days[1],
            onSelectedChange = {},
            isAvailable = { it.dayOfWeek.value < 6 },
            contentPadding = PaddingValues(Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DayCarouselDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        val days = previewDays()
        DayCarousel(
            days = days,
            selected = days[1],
            onSelectedChange = {},
            isAvailable = { it.dayOfWeek.value < 6 },
            contentPadding = PaddingValues(Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
private fun DayCarouselLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val days = previewDays()
        DayCarousel(
            days = days,
            selected = days[1],
            onSelectedChange = {},
            isAvailable = { it.dayOfWeek.value < 6 },
            contentPadding = PaddingValues(Spacing.screenMargin),
        )
    }
}
