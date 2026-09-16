package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/design-system.md, section 5: full width, 56 dp tall, radius 16 dp,
// fixed to the screen foot in multi-step flows (the caller's job, not this
// component's).
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        if (leadingIcon != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon()
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PrimaryButton(text = "Solicitar atención", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PrimaryButton(text = "Solicitar atención", onClick = {})
    }
}
