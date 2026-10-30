package bo.saludencasa.ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import bo.saludencasa.core.util.rememberAnimationsEnabled
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// Three periods that share no common beat, so the shapes never move in step.
private val PERIODS_MILLIS = listOf(6_200, 8_400, 10_600)
private const val REST_PHASE = 0.5f

private const val DISC_ALPHA = 0.12f
private const val ACCENT_ALPHA = 0.10f
private const val RING_ALPHA = 0.18f
private const val RING_BREATH = 0.08f

// docs/decisions.md, 2026-10-30, HT-25, the home background. Written with
// rememberInfiniteTransition and read only while drawing, so the shapes move
// without recomposing the screen (docs/decisions.md, 2026-10-09).
@Composable
fun FloatingCircles(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = SaludEnCasaTheme.brandColors.heartbeat
    val animationsEnabled = rememberAnimationsEnabled()
    val drift = with(LocalDensity.current) { Spacing.scale24.toPx() }
    val stroke = with(LocalDensity.current) { Spacing.scale4.toPx() }

    val phases: List<State<Float>> =
        if (animationsEnabled) {
            val transition = rememberInfiniteTransition(label = "floating_circles")
            PERIODS_MILLIS.mapIndexed { index, period ->
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = period, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "floating_circle_$index",
                )
            }
        } else {
            remember { PERIODS_MILLIS.map { mutableFloatStateOf(REST_PHASE) } }
        }

    Canvas(modifier = modifier) {
        val width = size.width
        val (first, second, third) = phases.map { it.value }

        drawCircle(
            color = primary,
            alpha = DISC_ALPHA,
            radius = width * 0.24f,
            center = Offset(width * 0.94f - first * drift, width * 0.12f + first * drift),
        )
        drawCircle(
            color = accent,
            alpha = ACCENT_ALPHA,
            radius = width * 0.20f,
            center = Offset(second * drift, size.height * 0.56f - second * drift),
        )
        drawCircle(
            color = primary,
            alpha = RING_ALPHA,
            radius = width * 0.13f * (1f + third * RING_BREATH),
            center = Offset(width * 0.82f, size.height * 0.88f - third * drift),
            style = Stroke(width = stroke),
        )
    }
}
