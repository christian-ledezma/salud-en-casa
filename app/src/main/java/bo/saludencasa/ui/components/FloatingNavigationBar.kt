package bo.saludencasa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bo.saludencasa.R
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// docs/design-system.md, section 5, adapted per plan.md HT-06: three
// destinations, not the reference kit's four — this product has no calls
// screen. Fixed content, so it owns its own strings rather than taking labels
// from the caller.
enum class NavigationDestination(
    val icon: ImageVector,
    val labelRes: Int,
) {
    HOME(Icons.Filled.Home, R.string.common_nav_home),
    REQUESTS(Icons.Filled.List, R.string.common_nav_requests),
    PROFILE(Icons.Filled.Person, R.string.common_nav_profile),
}

@Composable
fun FloatingNavigationBar(
    selected: NavigationDestination,
    onSelectedChange: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(ExtraShapes.pill)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        NavigationDestination.entries.forEach { destination ->
            val isSelected = destination == selected
            val label = stringResource(destination.labelRes)

            Box(
                modifier =
                    Modifier
                        .size(Spacing.minTouchTarget)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.background(MaterialTheme.colorScheme.surface)
                            } else {
                                Modifier
                            },
                        ).clickable { onSelectedChange(destination) }
                        .semantics {
                            this.selected = isSelected
                            role = Role.Tab
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp).alpha(if (isSelected) 1f else 0.7f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FloatingNavigationBarLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        FloatingNavigationBar(
            selected = NavigationDestination.HOME,
            onSelectedChange = {},
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FloatingNavigationBarDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        FloatingNavigationBar(
            selected = NavigationDestination.REQUESTS,
            onSelectedChange = {},
            modifier = Modifier.padding(Spacing.screenMargin),
        )
    }
}
