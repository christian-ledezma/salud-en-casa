package bo.saludencasa.features.notifications.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

// RF-09.13.
@Composable
fun NotificationPermissionPrompt(
    @StringRes body: Int,
    modifier: Modifier = Modifier,
    viewModel: NotificationPermissionViewModel = koinViewModel(),
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val answered by viewModel.answered.collectAsStateWithLifecycle()
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            granted = result
            viewModel.onAnswered()
        }

    if (answered || granted) return

    NotificationRationale(
        body = body,
        onEnable = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onDismiss = viewModel::onAnswered,
        modifier = modifier,
    )
}

@Composable
internal fun NotificationRationale(
    @StringRes body: Int,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            Text(
                text = stringResource(R.string.notification_permission_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryButton(text = stringResource(R.string.notification_permission_enable), onClick = onEnable)
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.notification_permission_dismiss))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Permiso de avisos, claro")
@Composable
private fun NotificationRationaleLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotificationRationale(
                body = R.string.notification_permission_body_professional,
                onEnable = {},
                onDismiss = {},
                modifier = Modifier.padding(Spacing.screenMargin),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320, name = "Permiso de avisos, oscuro")
@Composable
private fun NotificationRationaleDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotificationRationale(
                body = R.string.notification_permission_body_patient,
                onEnable = {},
                onDismiss = {},
                modifier = Modifier.padding(Spacing.screenMargin),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f, name = "Permiso de avisos, fuente 200%")
@Composable
private fun NotificationRationaleLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotificationRationale(
                body = R.string.notification_permission_body_professional,
                onEnable = {},
                onDismiss = {},
                modifier = Modifier.padding(Spacing.screenMargin),
            )
        }
    }
}
