package bo.saludencasa.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bo.saludencasa.core.util.rememberAnimationsEnabled
import bo.saludencasa.ui.theme.SaludEnCasaTheme

// docs/design-system.md, section 5: decorative circle of the welcome hero. A
// two-beat pulse (lub-dub) reads as a heartbeat instead of generic breathing;
// it freezes at rest size when "remove animations" is on, per
// .claude/rules/compose.md.
@Composable
fun PulsingHeartIcon(
    modifier: Modifier = Modifier,
    circleSize: Dp = 96.dp,
    iconSize: Dp = 40.dp,
    circleColor: Color = MaterialTheme.colorScheme.onPrimary,
    iconTint: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale =
        if (animationsEnabled) {
            val transition = rememberInfiniteTransition(label = "heartbeat")
            val animatedScale by
                transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation =
                                keyframes {
                                    durationMillis = LIVE_INDICATOR_CYCLE_MILLIS
                                    1f at 0
                                    1.18f at 150
                                    1f at 300
                                    1.12f at 450
                                    1f at 650
                                },
                            repeatMode = RepeatMode.Restart,
                        ),
                    label = "heartbeat_scale",
                )
            animatedScale
        } else {
            1f
        }

    Box(
        modifier =
            modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(circleColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize).scale(scale),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PulsingHeartIconPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PulsingHeartIcon()
    }
}
