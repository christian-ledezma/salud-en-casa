package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// One row of a group that reads as a single surface with dividers, the shape
// HomeRowGroup gives the rows of Inicio. A LazyColumn lays its rows out one by
// one, so each row draws its own corners by where it sits: the first rounds the
// top, the last rounds the bottom, and the ones between stay square.
@Composable
fun GroupedRow(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val round = MaterialTheme.shapes.large
    val shape =
        round.copy(
            topStart = if (index == 0) round.topStart else ZeroCornerSize,
            topEnd = if (index == 0) round.topEnd else ZeroCornerSize,
            bottomStart = if (index == count - 1) round.bottomStart else ZeroCornerSize,
            bottomEnd = if (index == count - 1) round.bottomEnd else ZeroCornerSize,
        )

    Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = containerColor) {
        Column {
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun PreviewGroup() {
    Column(modifier = Modifier.padding(Spacing.screenMargin)) {
        listOf("Consulta médica general", "Control prenatal y posparto", "Sondaje vesical").forEachIndexed {
            index,
            name,
            ->
            GroupedRow(index = index, count = 3) {
                Row(modifier = Modifier.padding(Spacing.cardPadding)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupedRowLightPreview() {
    SaludEnCasaTheme(darkTheme = false) { PreviewGroup() }
}

@Preview(showBackground = true)
@Composable
private fun GroupedRowDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) { PreviewGroup() }
}
