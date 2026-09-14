package bo.saludencasa.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.presentation.messageRes
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun StartupScreen(
    onSignIn: () -> Unit,
    onChooseRole: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartupViewModel = koinViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            StartupDestination.Loading, is StartupDestination.Error -> Unit
            StartupDestination.SignIn -> onSignIn()
            StartupDestination.ChooseRole -> onChooseRole()
            StartupDestination.Home -> onHome()
        }
    }

    StartupContent(
        destination = destination,
        onRetryClick = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun StartupContent(
    destination: StartupDestination,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        contentAlignment = Alignment.Center,
    ) {
        if (destination is StartupDestination.Error) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(destination.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetryClick)
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = loadingDescription },
            )
        }
    }
}

@Preview(showBackground = true, name = "Arranque, claro")
@Composable
private fun StartupLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        StartupContent(destination = StartupDestination.Loading, onRetryClick = {})
    }
}

@Preview(showBackground = true, name = "Arranque, oscuro")
@Composable
private fun StartupDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        StartupContent(destination = StartupDestination.Loading, onRetryClick = {})
    }
}

@Preview(showBackground = true, name = "Arranque con error")
@Composable
private fun StartupErrorPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        StartupContent(
            destination = StartupDestination.Error(ProfileError.NetworkUnavailable),
            onRetryClick = {},
        )
    }
}
