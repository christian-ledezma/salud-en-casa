package bo.saludencasa.ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import bo.saludencasa.core.util.rememberAnimationsEnabled
import bo.saludencasa.ui.theme.Spacing

private const val ENTRANCE_MILLIS = 450
private const val STEP_MILLIS = 60

// docs/decisions.md, 2026-10-30, HT-25, the home entrance: each block rises into
// place one step after the block above it.
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    var entered by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(Unit) { entered = true }

    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = ENTRANCE_MILLIS,
                delayMillis = index * STEP_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        label = "staggered_entrance",
    )
    val rise = with(LocalDensity.current) { Spacing.scale16.toPx() }

    Box(
        modifier =
            modifier.graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * rise
            },
    ) {
        content()
    }
}
