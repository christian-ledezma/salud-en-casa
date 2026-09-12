package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/design-system.md, section 5: label above the field in labelMedium, the
// field delineated with a 12 dp radius, the error below in the error color.
@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.labelToField))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            isError = errorText != null,
            singleLine = singleLine,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(),
        )
        if (errorText != null) {
            Spacer(modifier = Modifier.height(Spacing.scale4))
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormFieldLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        Column {
            FormField(label = "Correo electrónico", value = "", onValueChange = {})
            FormField(
                label = "Teléfono",
                value = "700",
                onValueChange = {},
                errorText = "Ingresa un número de al menos 7 dígitos.",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormFieldDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        Column {
            FormField(label = "Correo electrónico", value = "", onValueChange = {})
            FormField(
                label = "Teléfono",
                value = "700",
                onValueChange = {},
                errorText = "Ingresa un número de al menos 7 dígitos.",
            )
        }
    }
}
