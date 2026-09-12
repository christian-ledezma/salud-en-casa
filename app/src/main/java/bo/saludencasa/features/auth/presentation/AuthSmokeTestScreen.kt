package bo.saludencasa.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.network.SignInError
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Smoke test of the authentication chain (HT-05). Disposable: HU-01 replaces it.
 *
 * Spacing is written in raw units here because the theme has no spacing scale
 * yet; HT-06 introduces it and this screen disappears before that matters.
 */
@Composable
fun AuthSmokeTestScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthSmokeTestViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AuthSmokeTestContent(
        uiState = uiState,
        onSignInClick = { viewModel.signIn(context) },
        onSignOutClick = viewModel::signOut,
        modifier = modifier,
    )
}

@Composable
private fun AuthSmokeTestContent(
    uiState: AuthSmokeTestUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.auth_smoke_test_title),
            style = MaterialTheme.typography.titleLarge,
        )

        when (uiState) {
            AuthSmokeTestUiState.SignedOut -> {
                Text(
                    text = stringResource(R.string.auth_smoke_test_signed_out),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onSignInClick) {
                    Text(stringResource(R.string.auth_smoke_test_sign_in))
                }
            }

            AuthSmokeTestUiState.Loading -> {
                CircularProgressIndicator()
            }

            is AuthSmokeTestUiState.SignedIn -> {
                Text(
                    text = stringResource(R.string.auth_smoke_test_user_id, uiState.userId),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onSignOutClick) {
                    Text(stringResource(R.string.auth_smoke_test_sign_out))
                }
            }

            is AuthSmokeTestUiState.Error -> {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onSignInClick) {
                    Text(stringResource(R.string.common_retry))
                }
            }
        }
    }
}

/**
 * Where an error type becomes words. The domain never carries the sentence.
 */
private fun SignInError.messageRes(): Int =
    when (this) {
        SignInError.MissingConfiguration -> R.string.error_sign_in_missing_configuration
        SignInError.Cancelled -> R.string.error_sign_in_cancelled
        SignInError.NoGoogleAccount -> R.string.error_sign_in_no_google_account
        SignInError.CredentialManagerFailure -> R.string.error_sign_in_credential_manager_failure
        SignInError.TokenRejected -> R.string.error_sign_in_token_rejected
    }

@Preview(showBackground = true)
@Composable
private fun AuthSmokeTestSignedOutPreview() {
    SaludEnCasaTheme {
        AuthSmokeTestContent(AuthSmokeTestUiState.SignedOut, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthSmokeTestSignedInPreview() {
    SaludEnCasaTheme {
        AuthSmokeTestContent(AuthSmokeTestUiState.SignedIn("00000000-0000-0000-0000-000000000000"), {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthSmokeTestErrorPreview() {
    SaludEnCasaTheme {
        AuthSmokeTestContent(AuthSmokeTestUiState.Error(SignInError.NoGoogleAccount), {}, {})
    }
}
