package bo.saludencasa.ui.animations

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.SaludEnCasaTheme

private val LINE_WIDTH = 3.dp
private val GLOW_WIDTH = 9.dp
private val HEAD_RADIUS = 3.6.dp
private val HALO_RADIUS = 12.dp
private const val GLOW_ALPHA = 0.18f
private const val HALO_ALPHA = 0.6f

// docs/decisions.md, 2026-10-13, the startup screen.
@Composable
internal fun HeartbeatTrace(
    sweep: () -> Sweep,
    modifier: Modifier = Modifier,
    color: Color = SaludEnCasaTheme.brandColors.heartbeat,
) {
    Spacer(
        modifier =
            modifier
                .aspectRatio(TRACE_ASPECT_RATIO)
                .drawWithCache {
                    val trace = heartbeatPath(size)
                    val line = Stroke(width = LINE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val glow = Stroke(width = GLOW_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val headRadius = HEAD_RADIUS.toPx()
                    val haloRadius = HALO_RADIUS.toPx()
                    onDrawBehind {
                        val current = sweep()
                        val headX = current.head * size.width
                        clipRect(right = headX) { drawPath(trace, color, style = line) }
                        if (current.repeated) {
                            clipRect(left = headX + ERASE_GAP * size.width) { drawPath(trace, color, style = line) }
                        }
                        if (current.head > 0f && current.head < 1f) {
                            clipRect(left = headX - FRESH_TRACE_LENGTH * size.width, right = headX) {
                                drawPath(trace, color, alpha = GLOW_ALPHA, style = glow)
                            }
                            val head = Offset(headX, traceY(current.head, size))
                            drawCircle(
                                brush =
                                    Brush.radialGradient(
                                        colors = listOf(color.copy(alpha = HALO_ALPHA), Color.Transparent),
                                        center = head,
                                        radius = haloRadius,
                                    ),
                                radius = haloRadius,
                                center = head,
                            )
                            drawCircle(color = color, radius = headRadius, center = head)
                        }
                    }
                },
    )
}

private fun heartbeatPath(size: Size): Path =
    Path().apply {
        heartbeatSamples.forEachIndexed { index, x ->
            if (index == 0) moveTo(x * size.width, traceY(x, size)) else lineTo(x * size.width, traceY(x, size))
        }
    }

private fun traceY(
    x: Float,
    size: Size,
): Float = size.height * TRACE_BASELINE + heartbeatY(x) * size.width
