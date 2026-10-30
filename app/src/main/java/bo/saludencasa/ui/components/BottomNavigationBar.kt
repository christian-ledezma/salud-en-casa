package bo.saludencasa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.core.util.rememberAnimationsEnabled
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

private const val SELECTED_ICON_SCALE = 1.12f
private const val SELECTED_TINT_ALPHA = 0.12f

// Below 1 the spring overshoots: the indicator stretches past its width and
// settles back, which is the motion of the author's reference app.
private const val SPRING_DAMPING = 0.55f

// docs/design-system.md, section 5, and docs/decisions.md, 2026-10-30, HT-25.
// The labels come from the caller, as in SegmentedControl, because the
// destinations depend on who is using the bar.
@Composable
fun <T> BottomNavigationBar(
    destinations: List<T>,
    selected: T,
    onSelectedChange: (T) -> Unit,
    label: (T) -> String,
    icon: (destination: T, selected: Boolean) -> ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.scale4, vertical = Spacing.scale8)
                        .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                destinations.forEach { destination ->
                    val isSelected = destination == selected
                    NavigationItem(
                        label = label(destination),
                        icon = icon(destination, isSelected),
                        selected = isSelected,
                        onClick = { onSelectedChange(destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val primary = MaterialTheme.colorScheme.primary

    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) Spacing.scale32 else 0.dp,
        animationSpec = if (animationsEnabled) spring(SPRING_DAMPING, Spring.StiffnessMediumLow) else snap(),
        label = "navigation_indicator",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) SELECTED_ICON_SCALE else 1f,
        animationSpec = if (animationsEnabled) spring(SPRING_DAMPING, Spring.StiffnessMediumLow) else snap(),
        label = "navigation_icon_scale",
    )
    val background by animateColorAsState(
        targetValue = if (selected) primary.copy(alpha = SELECTED_TINT_ALPHA) else Color.Transparent,
        label = "navigation_background",
    )
    val contentColor = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(background)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .heightIn(min = Spacing.minTouchTarget)
                .padding(vertical = Spacing.scale8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
    ) {
        Box(
            modifier =
                Modifier
                    .width(indicatorWidth)
                    .height(Spacing.navIndicatorThickness)
                    .clip(ExtraShapes.pill)
                    .background(primary),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier =
                Modifier
                    .size(Spacing.scale24)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
        )
        NavigationLabel(text = label, selected = selected)
    }
}

// docs/decisions.md, 2026-10-30, HT-25: the label shrinks to fit its column,
// never below the size it has at 100 %.
@Composable
private fun NavigationLabel(
    text: String,
    selected: Boolean,
) {
    val style = MaterialTheme.typography.labelSmall
    val floor =
        with(LocalDensity.current) {
            style.fontSize.value.dp
                .toSp()
        }
    val color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    BasicText(
        text = text,
        style =
            style.copy(
                color = color,
                fontWeight = if (selected) FontWeight.SemiBold else style.fontWeight,
                textAlign = TextAlign.Center,
            ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = floor, maxFontSize = style.fontSize),
    )
}

private enum class PreviewDestination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME("Inicio", Icons.Outlined.Home, Icons.Filled.Home),
    REQUESTS("Solicitudes", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
    ACCOUNT("Cuenta", Icons.Outlined.Person, Icons.Filled.Person),
}

@Composable
private fun PreviewBar(selected: PreviewDestination) {
    BottomNavigationBar(
        destinations = PreviewDestination.entries,
        selected = selected,
        onSelectedChange = {},
        label = { it.label },
        icon = { destination, isSelected -> if (isSelected) destination.selectedIcon else destination.icon },
    )
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun BottomNavigationBarLightPreview() {
    SaludEnCasaTheme(darkTheme = false) { PreviewBar(PreviewDestination.HOME) }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun BottomNavigationBarDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) { PreviewBar(PreviewDestination.REQUESTS) }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
private fun BottomNavigationBarLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) { PreviewBar(PreviewDestination.REQUESTS) }
}
