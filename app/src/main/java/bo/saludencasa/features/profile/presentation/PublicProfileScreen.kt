package bo.saludencasa.features.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ProfileAvatar
import bo.saludencasa.ui.components.RatingBadge
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal

@Composable
fun PublicProfileScreen(
    professionalId: String,
    modifier: Modifier = Modifier,
    viewModel: PublicProfileViewModel = koinViewModel { parametersOf(professionalId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PublicProfileContent(
        uiState = uiState,
        onRetryClick = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun PublicProfileContent(
    uiState: PublicProfileUiState,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        PublicProfileUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        PublicProfileUiState.NotPublished -> {
            CenteredMessage(modifier = modifier) {
                Text(
                    text = stringResource(R.string.public_profile_not_published_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.public_profile_not_published_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is PublicProfileUiState.Failed -> {
            CenteredMessage(modifier = modifier) {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetryClick)
            }
        }

        is PublicProfileUiState.Content -> {
            PublishedCard(profile = uiState.profile, modifier = modifier)
        }
    }
}

@Composable
private fun PublishedCard(
    profile: PublicProfile,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.public_profile_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )

        ProfileAvatar(
            photoUrl = profile.photoUrl,
            contentDescription = stringResource(R.string.cd_professional_photo),
        )

        Text(
            text = profile.fullName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        profile.professionalType?.let { type ->
            Text(
                text = stringResource(type.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        profile.specialty?.let { specialty ->
            Text(
                text = specialty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Reputation(rating = profile.averageRating, totalReviews = profile.totalReviews)

        Text(
            text =
                pluralStringResource(
                    R.plurals.public_profile_years_of_experience,
                    profile.yearsOfExperience,
                    profile.yearsOfExperience,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text =
                profile.baseRateBob
                    ?.let { stringResource(R.string.public_profile_base_rate, formatBob(it, locale)) }
                    ?: stringResource(R.string.public_profile_base_rate_unset),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text =
                stringResource(
                    if (profile.availableNow) {
                        R.string.public_profile_available_now
                    } else {
                        R.string.public_profile_not_available_now
                    },
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text =
                pluralStringResource(
                    R.plurals.public_profile_total_services,
                    profile.totalServices,
                    profile.totalServices,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        profile.biography?.let { biography ->
            Text(
                text = biography,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// An average of zero over no ratings is not a bad professional, it is a
// professional nobody has rated. Drawing the badge anyway would read as one
// star out of five.
@Composable
private fun Reputation(
    rating: Double,
    totalReviews: Int,
    modifier: Modifier = Modifier,
) {
    if (totalReviews == 0) {
        Text(
            text = stringResource(R.string.public_profile_no_reviews),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        RatingBadge(rating = rating)
        Text(
            text = pluralStringResource(R.plurals.public_profile_total_reviews, totalReviews, totalReviews),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredMessage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

private fun previewProfile(
    baseRateBob: BigDecimal? = BigDecimal("120.00"),
    totalReviews: Int = 12,
): PublicProfile =
    PublicProfile(
        professionalId = "fcab94c0-0000-4000-8000-000000000000",
        fullName = "Ana Quispe",
        photoUrl = null,
        averageRating = 4.8,
        totalReviews = totalReviews,
        totalServices = 34,
        professionalType = ProfessionalType.NURSE,
        specialty = "Enfermería geriátrica",
        biography = "Diez años atendiendo a domicilio en La Paz.",
        yearsOfExperience = 10,
        baseRateBob = baseRateBob,
        availableNow = true,
    )

@Preview(showBackground = true, heightDp = 900, name = "Perfil publico, claro")
@Composable
private fun PublicProfileLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(PublicProfileUiState.Content(previewProfile()), {})
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Perfil publico, oscuro")
@Composable
private fun PublicProfileDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PublicProfileContent(PublicProfileUiState.Content(previewProfile()), {})
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Perfil publico sin tarifa ni calificaciones")
@Composable
private fun PublicProfileWithoutRatePreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(
            PublicProfileUiState.Content(previewProfile(baseRateBob = null, totalReviews = 0)),
            {},
        )
    }
}

@Preview(showBackground = true, name = "Perfil publico no publicado")
@Composable
private fun PublicProfileNotPublishedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(PublicProfileUiState.NotPublished, {})
    }
}

@Preview(showBackground = true, name = "Perfil publico cargando")
@Composable
private fun PublicProfileLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(PublicProfileUiState.Loading, {})
    }
}

@Preview(showBackground = true, name = "Perfil publico que no se pudo cargar")
@Composable
private fun PublicProfileFailedPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PublicProfileContent(PublicProfileUiState.Failed(ProfileError.NetworkUnavailable), {})
    }
}
