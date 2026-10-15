package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/design-system.md, section 5, primary button.
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale12),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
                content = content,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomActionBarLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        BottomActionBar { PrimaryButton(text = "Agendar cita", onClick = {}) }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomActionBarDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        BottomActionBar { PrimaryButton(text = "Agendar cita", onClick = {}) }
    }
}
