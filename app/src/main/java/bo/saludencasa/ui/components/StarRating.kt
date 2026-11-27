package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.R
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

const val STAR_COUNT = 5

// Five touch targets of 48 dp are 240 dp: they fit the card of a 320 dp phone and
// the stars do not grow with the font scale, because a larger star would not.
@Composable
fun StarPicker(
    rating: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        for (value in 1..STAR_COUNT) {
            val chosen = value <= rating
            IconButton(onClick = { onPick(value) }, modifier = Modifier.size(Spacing.minTouchTarget)) {
                Icon(
                    imageVector = if (chosen) Icons.Filled.Star else StarOutline,
                    contentDescription = pluralStringResource(R.plurals.cd_review_stars, value, value),
                    // tertiary and not the raw amber: the amber never carries a small
                    // icon on a light surface (docs/design-system.md, section 1).
                    tint = starTint(chosen),
                    modifier = Modifier.size(Spacing.scale40),
                )
            }
        }
    }
}

// What was rated, said once for the whole row instead of five times.
@Composable
fun StarsRead(
    rating: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.cd_review_rating_out_of_five, rating)
    Row(modifier = modifier.semantics { contentDescription = description }) {
        for (value in 1..STAR_COUNT) {
            val chosen = value <= rating
            Icon(
                imageVector = if (chosen) Icons.Filled.Star else StarOutline,
                contentDescription = null,
                tint = starTint(chosen),
                modifier = Modifier.size(Spacing.scale24).clearAndSetSemantics { },
            )
        }
    }
}

// The core icon set has no hollow star, and a filled gray one would tell a chosen star
// from an empty one by color alone.
private val StarOutline: ImageVector =
    ImageVector
        .Builder(
            name = "StarOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData =
                PathParser()
                    .parsePathString(
                        "M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03" +
                            "L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38" +
                            "-3.32 2.88 1 4.28L12 15.4z",
                    ).toNodes(),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black),
        ).build()

@Composable
private fun starTint(chosen: Boolean) =
    if (chosen) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun StarRatingLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        Row {
            StarPicker(rating = 3, onPick = {})
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun StarsReadDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        StarsRead(rating = 4)
    }
}
