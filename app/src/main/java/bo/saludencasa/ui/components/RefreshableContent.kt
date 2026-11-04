package bo.saludencasa.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import bo.saludencasa.R
import bo.saludencasa.ui.theme.Spacing

// The content has to scroll, also when empty, or the gesture never reaches this box.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val actionLabel = stringResource(R.string.cd_refresh_list)

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier =
            modifier.fillMaxSize().semantics {
                customActions =
                    listOf(
                        CustomAccessibilityAction(actionLabel) {
                            onRefresh()
                            true
                        },
                    )
            },
        content = content,
    )
}

@Composable
fun RefreshFailedNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = stringResource(R.string.common_refresh_failed),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Spacing.scale12),
        )
    }
}
