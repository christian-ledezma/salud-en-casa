package bo.saludencasa.ui.animations

import bo.saludencasa.ui.components.LIVE_INDICATOR_CYCLE_MILLIS

// The radar shares the heartbeat of every other live indicator on screen, so a
// viewer reads one pulse and not two clocks at different speeds. The same
// numbers drive the canvas form of the radar and the one drawn as map circles,
// which is why they live apart from both.
internal const val RADAR_CYCLE_MILLIS = LIVE_INDICATOR_CYCLE_MILLIS * 2
internal const val RADAR_RING_COUNT = 3
internal const val RADAR_START_ALPHA = 0.45f
internal const val RADAR_STROKE_WIDTH = 4f
internal const val REST_PROGRESS = 0.6f

// Each ring runs the same cycle, a third of it apart from the next.
internal fun radarRingProgress(
    progress: Float,
    ring: Int,
): Float = (progress + ring.toFloat() / RADAR_RING_COUNT) % 1f
