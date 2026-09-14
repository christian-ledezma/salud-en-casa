package bo.saludencasa.features.auth.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import bo.saludencasa.BuildConfig
import bo.saludencasa.R
import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountScreen(
    role: UserRole,
    onSignedOut: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AccountUiState.SignedOut) onSignedOut()
    }

    AccountContent(
        uiState = uiState,
        role = role,
        onSignOutClick = viewModel::signOut,
        onDismissError = viewModel::dismissError,
        onOpenProfile = onOpenProfile,
        modifier = modifier,
    )
}

@Composable
private fun AccountContent(
    uiState: AccountUiState,
    role: UserRole,
    onSignOutClick: () -> Unit,
    onDismissError: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        AccountUiState.Loading, AccountUiState.SignedOut -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        is AccountUiState.Error -> {
            AccountColumn(modifier = modifier) {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onSignOutClick)
                OutlinedButton(
                    onClick = onDismissError,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.auth_account_keep_session))
                }
            }
        }

        is AccountUiState.Content -> {
            AccountColumn(modifier = modifier) {
                Text(
                    text = stringResource(R.string.auth_account_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text =
                        uiState.session.email
                            ?.value
                            ?: stringResource(R.string.auth_account_email_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(role.labelRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                PrimaryButton(text = stringResource(R.string.auth_account_open_profile), onClick = onOpenProfile)
                OutlinedButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.auth_account_sign_out))
                }
                ForceCrashButton()
            }
        }
    }
}

@StringRes
private fun UserRole.labelRes(): Int =
    when (this) {
        UserRole.PATIENT -> R.string.profile_role_patient
        UserRole.PROFESSIONAL -> R.string.profile_role_professional
        UserRole.ADMIN -> R.string.profile_role_admin
    }

@Composable
private fun ForceCrashButton() {
    if (!BuildConfig.DEBUG) return
    OutlinedButton(
        onClick = { throw IllegalStateException("Crashlytics smoke test") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(R.string.debug_force_crash))
    }
}

@Composable
private fun AccountColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        content()
    }
}

private fun previewSession(): AuthSession =
    AuthSession(
        userId = "00000000-0000-0000-0000-000000000000",
        email = Email.create("ana.quispe@example.com").getOrNull(),
        fullName = PersonName.create("Ana Quispe").getOrNull(),
    )

@Preview(showBackground = true, name = "Cuenta con contenido, claro")
@Composable
private fun AccountContentLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AccountContent(AccountUiState.Content(previewSession()), UserRole.PATIENT, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta con contenido, oscuro")
@Composable
private fun AccountContentDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        AccountContent(AccountUiState.Content(previewSession()), UserRole.PROFESSIONAL, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta cargando")
@Composable
private fun AccountLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AccountContent(AccountUiState.Loading, UserRole.PATIENT, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta con error")
@Composable
private fun AccountErrorPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AccountContent(AccountUiState.Error(AuthError.NetworkUnavailable), UserRole.PATIENT, {}, {}, {})
    }
}
