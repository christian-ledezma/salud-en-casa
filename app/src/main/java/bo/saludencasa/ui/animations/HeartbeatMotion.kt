package bo.saludencasa.ui.animations

import kotlin.math.exp
import kotlin.math.pow

// docs/decisions.md, 2026-10-13, the startup screen.
// Times are milliseconds since the intro started. Along the trace, x and y are
// fractions of its width, and a negative y lies above the baseline.
internal const val MARK_SETTLE_START_MILLIS = 150
internal const val MARK_SETTLE_END_MILLIS = 600
internal const val WORDMARK_REVEAL_START_MILLIS = 260
internal const val WORDMARK_REVEAL_END_MILLIS = 680
internal const val SWEEP_START_MILLIS = 650
internal const val SWEEP_MILLIS = 1100
internal const val INTRO_MILLIS = SWEEP_START_MILLIS + SWEEP_MILLIS

internal const val SETTLED_MARK_SCALE = 0.7f

internal const val TRACE_ASPECT_RATIO = 4f
internal const val TRACE_BASELINE = 0.64f
internal const val ERASE_GAP = 0.07f
internal const val FRESH_TRACE_LENGTH = 0.09f

internal const val R_PEAK_X = 0.5f
private const val P_WAVE_OFFSET = -0.072f
private const val P_WAVE_HEIGHT = 0.020f
private const val P_WAVE_SPREAD = 0.012f
private const val T_WAVE_OFFSET = 0.070f
private const val T_WAVE_HEIGHT = 0.030f
private const val T_WAVE_SPREAD = 0.019f

// Q, R and S as offsets from the R peak and heights, with the baseline on
// either side.
private val QRS_COMPLEX =
    listOf(
        -0.018f to 0f,
        -0.010f to 0.014f,
        0f to -0.11f,
        0.010f to 0.042f,
        0.019f to 0f,
    )

private const val SAMPLES = 400

private class Beat(
    val riseMillis: Int,
    val fallMillis: Int,
    val amplitude: Float,
)

private val LUB = Beat(riseMillis = 90, fallMillis = 150, amplitude = 0.07f)
private val DUB = Beat(riseMillis = 80, fallMillis = 140, amplitude = 0.03f)
private const val DUB_DELAY_MILLIS = 250

internal data class Sweep(
    val head: Float,
    val repeated: Boolean,
)

internal fun markSettle(elapsedMillis: Float): Float =
    phase(elapsedMillis, MARK_SETTLE_START_MILLIS, MARK_SETTLE_END_MILLIS)

internal fun wordmarkReveal(elapsedMillis: Float): Float =
    phase(elapsedMillis, WORDMARK_REVEAL_START_MILLIS, WORDMARK_REVEAL_END_MILLIS)

internal fun firstSweep(elapsedMillis: Float): Sweep =
    Sweep(
        head = ((elapsedMillis - SWEEP_START_MILLIS) / SWEEP_MILLIS).coerceIn(0f, 1f),
        repeated = false,
    )

// Lub and dub, as the head of the trace crosses the R peak.
internal fun beatScale(sweepMillis: Float): Float {
    val sinceCrossing = sweepMillis - R_PEAK_X * SWEEP_MILLIS
    return 1f + bump(sinceCrossing, LUB) + bump(sinceCrossing - DUB_DELAY_MILLIS, DUB)
}

internal fun heartbeatY(x: Float): Float =
    -P_WAVE_HEIGHT * gaussian(x, R_PEAK_X + P_WAVE_OFFSET, P_WAVE_SPREAD) -
        T_WAVE_HEIGHT * gaussian(x, R_PEAK_X + T_WAVE_OFFSET, T_WAVE_SPREAD) +
        qrs(x - R_PEAK_X)

// Every knot of the complex is a sample of its own, so the peaks are drawn
// sharp and at their full height.
internal val heartbeatSamples: List<Float> =
    ((0..SAMPLES).map { it.toFloat() / SAMPLES } + QRS_COMPLEX.map { R_PEAK_X + it.first })
        .distinct()
        .sorted()

private fun qrs(offset: Float): Float {
    for ((from, to) in QRS_COMPLEX.zipWithNext()) {
        if (offset >= from.first && offset < to.first) {
            val fraction = (offset - from.first) / (to.first - from.first)
            return from.second + (to.second - from.second) * fraction
        }
    }
    return 0f
}

private fun bump(
    millis: Float,
    beat: Beat,
): Float =
    when {
        millis < 0f || millis > beat.riseMillis + beat.fallMillis -> 0f
        millis < beat.riseMillis -> beat.amplitude * easeOut(millis / beat.riseMillis)
        else -> beat.amplitude * (1f - easeOut((millis - beat.riseMillis) / beat.fallMillis))
    }

private fun phase(
    elapsedMillis: Float,
    startMillis: Int,
    endMillis: Int,
): Float = easeOut(((elapsedMillis - startMillis) / (endMillis - startMillis)).coerceIn(0f, 1f))

private fun easeOut(fraction: Float): Float = 1f - (1f - fraction).pow(3)

private fun gaussian(
    x: Float,
    center: Float,
    spread: Float,
): Float = exp(-((x - center) / spread).pow(2))
