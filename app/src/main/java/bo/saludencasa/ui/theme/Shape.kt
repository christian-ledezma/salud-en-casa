package bo.saludencasa.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// docs/design-system.md, section 3. Material 3's five slots (extraSmall,
// small, medium, large, extraLarge) map onto the doc's per-element radii;
// large maps to the content card, not the featured one, because Shapes has no
// sixth slot for it.
val SaludEnCasaShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(12.dp), // Form field.
        medium = RoundedCornerShape(16.dp), // Primary and secondary button.
        large = RoundedCornerShape(20.dp), // Content card.
        extraLarge = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), // Bottom sheet.
    )

// The featured card, pill, and avatar radii have no slot in Shapes above.
object ExtraShapes {
    val featuredCard: Shape = RoundedCornerShape(24.dp)
    val pill: Shape = RoundedCornerShape(percent = 50)
    val avatar: Shape = CircleShape
}
