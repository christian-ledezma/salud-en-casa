package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.ui.theme.SaludEnCasaTheme

// A field to narrow a list by typing. The text field is the form field's
// shape (radius 12 dp, docs/design-system.md, section 3) on the surface colour,
// so it reads as a field on the grey of the screen. The clear button is only
// there when there is something to clear.
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val focusManager: FocusManager = LocalFocusManager.current
    val surface = MaterialTheme.colorScheme.surface

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        placeholder = { Text(text = placeholder) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon =
            if (value.isNotEmpty()) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = clearDescription)
                    }
                }
            } else {
                null
            },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = MaterialTheme.shapes.small,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = surface,
                unfocusedContainerColor = surface,
                disabledContainerColor = surface,
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchFieldLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        SearchField(
            value = "",
            onValueChange = {},
            placeholder = "Buscar en el catálogo",
            clearDescription = "Borrar la búsqueda",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchFieldDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        SearchField(
            value = "cuid",
            onValueChange = {},
            placeholder = "Buscar en el catálogo",
            clearDescription = "Borrar la búsqueda",
        )
    }
}
