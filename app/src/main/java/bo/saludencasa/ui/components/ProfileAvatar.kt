package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import coil3.compose.AsyncImage

// docs/design-system.md, section 3: the avatar is circular. The size lives here
// rather than in the screen so no measurement is written into a screen.
//
// The initials, or the silhouette when there is no name to take them from, are
// drawn always and the photograph on top of them, so they serve as the
// placeholder while the image loads and as the error state if it never
// arrives, which .claude/rules/compose.md requires of every image.
@Composable
fun ProfileAvatar(
    photoUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    name: String? = null,
) {
    val background =
        if (name == null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier =
            modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(background)
                .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (name == null) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        } else {
            Text(
                text = initialsOf(name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileAvatarLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ProfileAvatar(photoUrl = null, contentDescription = "Fotografía de perfil")
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileAvatarDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ProfileAvatar(photoUrl = null, contentDescription = "Fotografía de perfil", name = "Ana Quispe")
    }
}
