package bo.saludencasa.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.R
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.core.vo.PartOfDay
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import java.time.LocalTime

// docs/design-system.md, section 5, the hour pills of the date and time
// carousel. Material 3's own carousels shrink and clip the items at the edges.
@Composable
fun TimeChipRow(
    times: List<LocalTime>,
    selected: LocalTime?,
    onSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val locale = LocalConfiguration.current.locales[0]

    LazyRow(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        modifier = modifier.selectableGroup(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        byPartOfDay(times)
            .entries
            .forEachIndexed { index, (part, partTimes) ->
                item(key = "part-" + part.name) {
                    Text(
                        text = stringResource(part.labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = if (index == 0) Modifier else Modifier.padding(start = Spacing.scale8),
                    )
                }
                items(partTimes, key = { "time-$it" }) { time ->
                    TimeChip(
                        text = formatTime(time, locale),
                        selected = time == selected,
                        onClick = { onSelected(time) },
                    )
                }
            }
    }
}

// The row interleaves a label before each part of the day, so the position of a
// time is not its position in the list.
fun timeChipIndex(
    times: List<LocalTime>,
    target: LocalTime,
): Int {
    var index = 0
    byPartOfDay(times).forEach { (_, partTimes) ->
        index++
        val position = partTimes.indexOf(target)
        if (position >= 0) return index + position
        index += partTimes.size
    }
    return 0
}

private fun byPartOfDay(times: List<LocalTime>): Map<PartOfDay, List<LocalTime>> =
    times.groupBy(PartOfDay::of).toSortedMap()

@Composable
fun TimeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier =
            Modifier
                .heightIn(min = Spacing.minTouchTarget)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = ExtraShapes.pill,
        color = if (selected) colors.primary else colors.surface,
        border = if (selected) null else BorderStroke(Spacing.borderWidth, colors.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Spacing.scale16, vertical = Spacing.scale12),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.onPrimary else colors.onSurface,
            )
        }
    }
}

@StringRes
fun PartOfDay.labelRes(): Int =
    when (this) {
        PartOfDay.MORNING -> R.string.common_part_morning
        PartOfDay.AFTERNOON -> R.string.common_part_afternoon
        PartOfDay.EVENING -> R.string.common_part_evening
    }

private fun previewTimes(): List<LocalTime> =
    listOf(LocalTime.of(10, 0), LocalTime.of(11, 30), LocalTime.of(14, 0), LocalTime.of(18, 30))

@Preview(showBackground = true)
@Composable
private fun TimeChipRowLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        TimeChipRow(
            times = previewTimes(),
            selected = LocalTime.of(11, 30),
            onSelected = {},
            contentPadding = PaddingValues(horizontal = Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeChipRowDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        TimeChipRow(
            times = previewTimes(),
            selected = null,
            onSelected = {},
            contentPadding = PaddingValues(horizontal = Spacing.screenMargin),
        )
    }
}
