package bo.saludencasa.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import bo.saludencasa.R
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.auth.presentation.RoleSection
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.presentation.activateRes
import bo.saludencasa.features.profile.presentation.messageRes
import bo.saludencasa.ui.components.initialsOf
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

@Composable
internal fun AccountTabContent(
    session: AuthSession,
    roleSection: RoleSection,
    onOpenProfile: () -> Unit,
    onActivate: (AssignableRole) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val professional = roleSection.roles.active == UserRole.PROFESSIONAL

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        IdentityCard(session = session)
        HomeRowGroup {
            HomeRow(
                title = stringResource(R.string.auth_account_open_profile),
                details =
                    listOf(
                        stringResource(
                            if (professional) {
                                R.string.home_account_profile_body_professional
                            } else {
                                R.string.home_account_profile_body_patient
                            },
                        ),
                    ),
                icon = rememberVectorPainter(Icons.Filled.Person),
                onClick = onOpenProfile,
            )
        }
        if (roleSection.roles.addable.isNotEmpty()) {
            HomeRowGroup {
                roleSection.roles.addable.forEachIndexed { index, role ->
                    if (index > 0) HomeRowDivider()
                    HomeRow(
                        title = stringResource(role.activateRes()),
                        icon = rememberVectorPainter(Icons.Filled.Add),
                        onClick = { onActivate(role) },
                    )
                }
            }
        }
        if (roleSection.busy) CenteredProgress(modifier = Modifier.fillMaxWidth())
        roleSection.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        HomeRowGroup {
            HomeRow(
                title = stringResource(R.string.auth_account_sign_out),
                icon = rememberVectorPainter(Icons.AutoMirrored.Filled.ExitToApp),
                tone = RowTone.DANGER,
                showChevron = false,
                onClick = onSignOut,
            )
        }
    }
}

@Composable
private fun IdentityCard(session: AuthSession) {
    val name = session.fullName?.value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SaludEnCasaTheme.extraShapes.featuredCard,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(Spacing.scale48)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                if (name != null) {
                    Text(
                        text = initialsOf(name),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = session.email?.value ?: stringResource(R.string.auth_account_email_unavailable),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
