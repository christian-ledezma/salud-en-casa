package bo.saludencasa.ui.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import bo.saludencasa.core.util.rememberAnimationsEnabled

// The rings of a radar, the shape Uber and InDrive use while they look around
// you. Written with rememberInfiniteTransition and never with a frame loop of
// its own: that is the form the Compose test clock knows how to cancel
// (docs/decisions.md, 2026-10-09).
@Composable
fun RadarPulse(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val progress =
        if (animationsEnabled) {
            val transition = rememberInfiniteTransition(label = "radar")
            val value by
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = RADAR_CYCLE_MILLIS, easing = LinearEasing),
                        ),
                    label = "radar_progress",
                )
            value
        } else {
            // At rest the rings stand still, spread across the cycle, so the
            // figure still reads as a radar instead of disappearing.
            REST_PROGRESS
        }

    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        repeat(RADAR_RING_COUNT) { ring ->
            val ringProgress = radarRingProgress(progress, ring)
            drawCircle(
                color = color,
                radius = maxRadius * ringProgress,
                alpha = RADAR_START_ALPHA * (1f - ringProgress),
                style = Stroke(width = RADAR_STROKE_WIDTH),
            )
        }
    }
}
