package bo.saludencasa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// docs/design-system.md, section 1. Every role a component of this app reads is
// set: the Material 3 baseline fills the rest with violet (docs/decisions.md,
// 2026-10-15, the search and profile screens).
private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF1A6F8F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCFE8F2),
        onPrimaryContainer = Color(0xFF08303F),
        secondary = Color(0xFF4A7C8F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD5E6ED),
        onSecondaryContainer = Color(0xFF0B3A4C),
        tertiary = Color(0xFFB87400),
        tertiaryContainer = Color(0xFFFFA600),
        onTertiaryContainer = Color(0xFF3D2800),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        background = Color(0xFFF1F4F6),
        onBackground = Color(0xFF0D0D0D),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0D0D0D),
        surfaceVariant = Color(0xFFE3EBEF),
        onSurfaceVariant = Color(0xFF4C5B62),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F9FA),
        surfaceContainer = Color(0xFFEEF2F4),
        surfaceContainerHigh = Color(0xFFE8EEF1),
        surfaceContainerHighest = Color(0xFFE3EBEF),
        outline = Color(0xFFC2CCD1),
        outlineVariant = Color(0xFFDEE5E8),
    )

// Not the light scheme inverted: the teal lightens to hold contrast on a dark
// surface (docs/design-system.md, section 1).
private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF7FC5DE),
        onPrimary = Color(0xFF00344A),
        primaryContainer = Color(0xFF0F5570),
        onPrimaryContainer = Color(0xFFCFE8F2),
        secondary = Color(0xFFA6CBDA),
        onSecondary = Color(0xFF0B3A4C),
        secondaryContainer = Color(0xFF1D4555),
        onSecondaryContainer = Color(0xFFCFE8F2),
        tertiary = Color(0xFFE0A83C),
        onTertiary = Color(0xFF2B1C00),
        tertiaryContainer = Color(0xFFE0A83C),
        onTertiaryContainer = Color(0xFF2B1C00),
        background = Color(0xFF0E1518),
        onBackground = Color(0xFFE6EDF0),
        surface = Color(0xFF162025),
        onSurface = Color(0xFFE6EDF0),
        surfaceVariant = Color(0xFF22313A),
        onSurfaceVariant = Color(0xFFA9BAC1),
        surfaceContainerLowest = Color(0xFF0A1013),
        surfaceContainerLow = Color(0xFF131B1F),
        surfaceContainer = Color(0xFF182226),
        surfaceContainerHigh = Color(0xFF1E2A30),
        surfaceContainerHighest = Color(0xFF26343A),
        outline = Color(0xFF3B4A51),
        outlineVariant = Color(0xFF26343A),
    )

private val LocalStatusColors = compositionLocalOf { LightStatusColors }

private val LocalBrandColors = compositionLocalOf { LightBrandColors }

// Exposes spacing, status and brand colors, and shapes with no Material 3 slot
// the same way MaterialTheme exposes colorScheme and typography.
object SaludEnCasaTheme {
    val spacing: Spacing get() = Spacing

    val extraShapes: ExtraShapes get() = ExtraShapes

    val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current

    val brandColors: BrandColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBrandColors.current

    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
        val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors
        val brandColors = if (darkTheme) DarkBrandColors else LightBrandColors

        CompositionLocalProvider(
            LocalStatusColors provides statusColors,
            LocalBrandColors provides brandColors,
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = SaludEnCasaShapes,
                content = content,
            )
        }
    }
}
