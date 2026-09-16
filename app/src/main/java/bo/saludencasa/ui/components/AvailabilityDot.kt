package bo.saludencasa.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.core.util.rememberAnimationsEnabled
import bo.saludencasa.ui.theme.SaludEnCasaTheme

// docs/design-system.md, section 5: availability indicator. Blinks to draw
// the eye to a live signal; freezes at full opacity when the system's
// "remove animations" accessibility setting is on, per .claude/rules/compose.md.
@Composable
fun AvailabilityDot(
    modifier: Modifier = Modifier,
    color: Color = SaludEnCasaTheme.statusColors.availableNow,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val alpha =
        if (animationsEnabled) {
            val transition = rememberInfiniteTransition(label = "availability_dot")
            val animatedAlpha by
                transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.25f,
                    animationSpec =
                        infiniteRepeatable(
                            animation =
                                tween(
                                    durationMillis = LIVE_INDICATOR_CYCLE_MILLIS / 2,
                                    easing = LinearEasing,
                                ),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "availability_dot_alpha",
                )
            animatedAlpha
        } else {
            1f
        }

    Box(
        modifier =
            modifier
                .size(8.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(color),
    )
}

@Preview(showBackground = true)
@Composable
private fun AvailabilityDotLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AvailabilityDot()
    }
}
