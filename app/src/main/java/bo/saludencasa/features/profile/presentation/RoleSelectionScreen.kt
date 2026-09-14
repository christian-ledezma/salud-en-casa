package bo.saludencasa.features.profile.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun RoleSelectionScreen(
    onRoleAssigned: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoleSelectionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        val assigned = uiState as? RoleSelectionUiState.Assigned ?: return@LaunchedEffect
        onRoleAssigned(assigned.role)
    }

    RoleSelectionContent(
        uiState = uiState,
        onRoleClick = viewModel::select,
        onConfirmClick = viewModel::confirm,
        modifier = modifier,
    )
}

@Composable
private fun RoleSelectionContent(
    uiState: RoleSelectionUiState,
    onRoleClick: (AssignableRole) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = uiState.selected()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        ) {
            Text(
                text = stringResource(R.string.profile_role_selection_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.profile_role_selection_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AssignableRole.entries.forEach { option ->
                RoleOption(
                    title = stringResource(option.titleRes()),
                    description = stringResource(option.descriptionRes()),
                    selected = option == selected,
                    enabled = uiState !is RoleSelectionUiState.Saving,
                    onClick = { onRoleClick(option) },
                )
            }
        }

        ConfirmSection(uiState = uiState, onConfirmClick = onConfirmClick)
    }
}

@Composable
private fun ConfirmSection(
    uiState: RoleSelectionUiState,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val savingDescription = stringResource(R.string.cd_loading)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (uiState is RoleSelectionUiState.Error) {
            Text(
                text = stringResource(uiState.error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (uiState is RoleSelectionUiState.Saving) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = savingDescription },
            )
        } else {
            PrimaryButton(
                text =
                    if (uiState is RoleSelectionUiState.Error) {
                        stringResource(R.string.common_retry)
                    } else {
                        stringResource(R.string.profile_role_selection_confirm)
                    },
                onClick = onConfirmClick,
                enabled = uiState.selected() != null,
            )
        }
    }
}

@Composable
private fun RoleOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouchTarget)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                ),
        shape = MaterialTheme.shapes.large,
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        contentColor =
            if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@StringRes
private fun AssignableRole.titleRes(): Int =
    when (this) {
        AssignableRole.PATIENT -> R.string.profile_role_patient
        AssignableRole.PROFESSIONAL -> R.string.profile_role_professional
    }

@StringRes
private fun AssignableRole.descriptionRes(): Int =
    when (this) {
        AssignableRole.PATIENT -> R.string.profile_role_patient_description
        AssignableRole.PROFESSIONAL -> R.string.profile_role_professional_description
    }

private fun RoleSelectionUiState.selected(): AssignableRole? =
    when (this) {
        is RoleSelectionUiState.Choosing -> selected
        is RoleSelectionUiState.Saving -> selected
        is RoleSelectionUiState.Error -> selected
        is RoleSelectionUiState.Assigned -> null
    }

@Preview(showBackground = true, name = "Eleccion de rol sin elegir, claro")
@Composable
private fun RoleSelectionEmptyPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        RoleSelectionContent(RoleSelectionUiState.Choosing(selected = null), {}, {})
    }
}

@Preview(showBackground = true, name = "Eleccion de rol con opcion elegida, claro")
@Composable
private fun RoleSelectionChosenLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        RoleSelectionContent(RoleSelectionUiState.Choosing(AssignableRole.PATIENT), {}, {})
    }
}

@Preview(showBackground = true, name = "Eleccion de rol con opcion elegida, oscuro")
@Composable
private fun RoleSelectionChosenDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        RoleSelectionContent(RoleSelectionUiState.Choosing(AssignableRole.PROFESSIONAL), {}, {})
    }
}

@Preview(showBackground = true, name = "Eleccion de rol guardando")
@Composable
private fun RoleSelectionSavingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        RoleSelectionContent(RoleSelectionUiState.Saving(AssignableRole.PATIENT), {}, {})
    }
}

@Preview(showBackground = true, name = "Eleccion de rol con error")
@Composable
private fun RoleSelectionErrorPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        RoleSelectionContent(
            RoleSelectionUiState.Error(AssignableRole.PROFESSIONAL, ProfileError.NetworkUnavailable),
            {},
            {},
        )
    }
}
