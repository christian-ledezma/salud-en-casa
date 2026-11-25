package bo.saludencasa.features.subscription.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

// HU-38: why the inbox holds no new requests, above it, so an expired
// professional does not read the empty list as a quiet day.
@Composable
fun SubscriptionExpiredNotice(
    onOpenSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = Spacing.cardPadding,
                    end = Spacing.cardPadding,
                    top = Spacing.cardPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = SaludEnCasaTheme.statusColors.pending,
                    modifier = Modifier.size(Spacing.scale20),
                )
                Text(
                    text = stringResource(R.string.subscription_expired_notice_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.subscription_expired_notice_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onOpenSubscription,
                modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
            ) {
                Text(text = stringResource(R.string.subscription_expired_notice_open))
            }
        }
    }
}
