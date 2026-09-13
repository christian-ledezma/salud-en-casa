package bo.saludencasa.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun WelcomeScreen(
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = koinViewModel(),
    credentialClient: GoogleCredentialClient = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is WelcomeUiState.SignedIn) onSignedIn()
    }

    WelcomeContent(
        uiState = uiState,
        onSignInClick = { viewModel.signIn { credentialClient.requestIdToken(context) } },
        modifier = modifier,
    )
}

@Composable
private fun WelcomeContent(
    uiState: WelcomeUiState,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap),
    ) {
        // The heading scrolls and the call to action stays at the foot, which is
        // what keeps the screen usable at a 200 % system font size.
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.auth_welcome_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.auth_welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SignInSection(uiState = uiState, onSignInClick = onSignInClick)
    }
}

@Composable
private fun SignInSection(
    uiState: WelcomeUiState,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (uiState is WelcomeUiState.Error) {
            Text(
                text = stringResource(uiState.error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        when (uiState) {
            WelcomeUiState.SigningIn, WelcomeUiState.SignedIn -> {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
                Text(
                    text = stringResource(R.string.auth_welcome_signing_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            WelcomeUiState.Idle, is WelcomeUiState.Error -> {
                PrimaryButton(
                    text =
                        if (uiState is WelcomeUiState.Error) {
                            stringResource(R.string.common_retry)
                        } else {
                            stringResource(R.string.auth_welcome_sign_in_with_google)
                        },
                    onClick = onSignInClick,
                )
                Text(
                    text = stringResource(R.string.auth_welcome_no_password_notice),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Bienvenida en reposo, claro")
@Composable
private fun WelcomeIdleLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        WelcomeContent(uiState = WelcomeUiState.Idle, onSignInClick = {})
    }
}

@Preview(showBackground = true, name = "Bienvenida en reposo, oscuro")
@Composable
private fun WelcomeIdleDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        WelcomeContent(uiState = WelcomeUiState.Idle, onSignInClick = {})
    }
}

@Preview(showBackground = true, name = "Bienvenida ingresando")
@Composable
private fun WelcomeSigningInPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        WelcomeContent(uiState = WelcomeUiState.SigningIn, onSignInClick = {})
    }
}

@Preview(showBackground = true, name = "Bienvenida con error")
@Composable
private fun WelcomeErrorPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        WelcomeContent(uiState = WelcomeUiState.Error(AuthError.NoGoogleAccount), onSignInClick = {})
    }
}
