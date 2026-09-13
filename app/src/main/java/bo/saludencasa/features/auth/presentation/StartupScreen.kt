package bo.saludencasa.features.auth.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun StartupScreen(
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartupViewModel = koinViewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        when (sessionState) {
            SessionState.Loading -> Unit
            SessionState.SignedOut -> onSignedOut()
            is SessionState.SignedIn -> onSignedIn()
        }
    }

    StartupContent(modifier = modifier)
}

@Composable
private fun StartupContent(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = loadingDescription },
        )
    }
}

@Preview(showBackground = true, name = "Arranque, claro")
@Composable
private fun StartupLightPreview() {
    SaludEnCasaTheme(darkTheme = false) { StartupContent() }
}

@Preview(showBackground = true, name = "Arranque, oscuro")
@Composable
private fun StartupDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) { StartupContent() }
}
