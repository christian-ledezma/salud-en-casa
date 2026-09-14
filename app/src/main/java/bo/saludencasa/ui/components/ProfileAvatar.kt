package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import coil3.compose.AsyncImage

// docs/design-system.md, section 3: the avatar is circular. The size lives here
// rather than in the screen so no measurement is written into a screen.
@Composable
fun ProfileAvatar(
    photoUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl == null) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
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
        ProfileAvatar(photoUrl = null, contentDescription = "Fotografía de perfil")
    }
}
