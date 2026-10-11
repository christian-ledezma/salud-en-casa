package bo.saludencasa.features.auth.presentation

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
import bo.saludencasa.R
import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.presentation.activateRes
import bo.saludencasa.features.profile.presentation.messageRes
import bo.saludencasa.features.profile.presentation.shortLabelRes
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.SegmentedControl
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountScreen(
    onSignedOut: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAddress: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenDocumentReview: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AccountUiState.SignedOut) onSignedOut()
    }

    AccountContent(
        uiState = uiState,
        onSignOutClick = viewModel::signOut,
        onDismissError = viewModel::dismissError,
        onOpenProfile = onOpenProfile,
        onOpenSearch = onOpenSearch,
        onOpenAddress = onOpenAddress,
        onOpenVerification = onOpenVerification,
        onOpenDocumentReview = onOpenDocumentReview,
        onSwitchTo = viewModel::switchTo,
        onActivate = viewModel::activate,
        modifier = modifier,
    )
}

@Composable
internal fun AccountContent(
    uiState: AccountUiState,
    onSignOutClick: () -> Unit,
    onDismissError: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAddress: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenDocumentReview: () -> Unit,
    onSwitchTo: (UserRole) -> Unit,
    onActivate: (AssignableRole) -> Unit,
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
                RoleSwitch(
                    section = uiState.roleSection,
                    onSwitchTo = onSwitchTo,
                    onActivate = onActivate,
                )
                // docs/decisions.md, 2026-10-06, review panel lives in the app.
                if (uiState.roleSection.roles.isAdmin) {
                    PrimaryButton(
                        text = stringResource(R.string.auth_account_open_document_review),
                        onClick = onOpenDocumentReview,
                    )
                } else {
                    PrimaryButton(text = stringResource(R.string.auth_account_open_profile), onClick = onOpenProfile)
                    if (uiState.roleSection.roles.active == UserRole.PATIENT) {
                        OutlinedButton(
                            onClick = onOpenSearch,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.auth_account_open_search))
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenAddress,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.auth_account_open_address))
                    }
                    OutlinedButton(
                        onClick = onOpenVerification,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.auth_account_open_verification))
                    }
                }
                OutlinedButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.auth_account_sign_out))
                }
            }
        }
    }
}

// The control appears only when there is somewhere to switch into. For a person
// with one role it is replaced by the invitation to activate the other, so the
// screen never offers a choice that does nothing.
@Composable
private fun RoleSwitch(
    section: RoleSection,
    onSwitchTo: (UserRole) -> Unit,
    onActivate: (AssignableRole) -> Unit,
) {
    val active = section.roles.active
    // Resolved here because optionLabel is a plain lambda, not a composable one.
    val labels = UserRole.entries.associateWith { stringResource(it.shortLabelRes()) }
    val loadingDescription = stringResource(R.string.cd_loading)

    if (section.roles.canSwitch && active != null) {
        Text(
            text = stringResource(R.string.auth_account_role_switch_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedControl(
            options = section.roles.switchable,
            selected = active,
            onSelectedChange = onSwitchTo,
            optionLabel = { labels.getValue(it) },
        )
    }

    section.roles.addable.forEach { role ->
        OutlinedButton(
            onClick = { onActivate(role) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(role.activateRes()))
        }
    }

    if (section.busy) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = loadingDescription },
        )
    }

    section.error?.let { error ->
        Text(
            text = stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
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

private fun previewRoles(
    held: Set<UserRole>,
    active: UserRole,
    error: ProfileError? = null,
): RoleSection =
    RoleSection(
        roles = ProfileRoles.create(held, active).getOrThrow(),
        error = error,
    )

private val bothRoles = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL)
private val onlyPatient = setOf(UserRole.PATIENT)

@Preview(showBackground = true, name = "Cuenta con los dos roles, claro")
@Composable
private fun AccountBothRolesLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val state = AccountUiState.Content(previewSession(), previewRoles(bothRoles, UserRole.PATIENT))
        AccountContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta con los dos roles, oscuro")
@Composable
private fun AccountBothRolesDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        val state = AccountUiState.Content(previewSession(), previewRoles(bothRoles, UserRole.PROFESSIONAL))
        AccountContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta con un solo rol, claro")
@Composable
private fun AccountSingleRoleLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val state = AccountUiState.Content(previewSession(), previewRoles(onlyPatient, UserRole.PATIENT))
        AccountContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta con un solo rol, oscuro")
@Composable
private fun AccountSingleRoleDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        val state = AccountUiState.Content(previewSession(), previewRoles(onlyPatient, UserRole.PATIENT))
        AccountContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta de administrador")
@Composable
private fun AccountAdminPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val state = AccountUiState.Content(previewSession(), previewRoles(setOf(UserRole.ADMIN), UserRole.ADMIN))
        AccountContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta con cambio de rol fallido")
@Composable
private fun AccountRoleSwitchErrorPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val roles = previewRoles(bothRoles, UserRole.PATIENT, ProfileError.NetworkUnavailable)
        AccountContent(AccountUiState.Content(previewSession(), roles), {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta cargando")
@Composable
private fun AccountLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AccountContent(AccountUiState.Loading, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cuenta con error")
@Composable
private fun AccountErrorPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        AccountContent(AccountUiState.Error(AuthError.NetworkUnavailable), {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
