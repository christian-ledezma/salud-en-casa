package bo.saludencasa.ui.theme

import androidx.compose.ui.graphics.Color

// Material 3 has no success role. docs/design-system.md, section 1, adds these
// as a theme extension instead of writing them into screens directly.
data class StatusColors(
    val positive: Color,
    val pending: Color,
    val negative: Color,
    val availableNow: Color,
)

val LightStatusColors =
    StatusColors(
        positive = Color(0xFF15782B),
        pending = Color(0xFFB87400),
        negative = Color(0xFFB3261E),
        availableNow = Color(0xFF15782B),
    )

val DarkStatusColors =
    StatusColors(
        positive = Color(0xFF6FD588),
        pending = Color(0xFFE0A83C),
        negative = Color(0xFFF2B8B5),
        availableNow = Color(0xFF6FD588),
    )
