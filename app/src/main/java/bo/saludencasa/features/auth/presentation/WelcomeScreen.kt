package bo.saludencasa.features.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.ui.components.AvailabilityDot
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.PulsingHeartIcon
import bo.saludencasa.ui.theme.ExtraShapes
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
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
        ) {
            WelcomeHero()
            WelcomeHighlights(modifier = Modifier.padding(horizontal = Spacing.screenMargin))
        }

        SignInSection(
            uiState = uiState,
            onSignInClick = onSignInClick,
            modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        )
    }
}

// docs/design-system.md, section 5: header with gradient. The document's rule
// -no more than a third of the screen- describes the size at rest; this header
// does not force a fixed height because, at the system's 200% font size or on
// a narrow screen, a fixed height ended up wedging the decorative circle
// against the rating badge. The content decides its own height and breathes
// with spacing, never with overlapping absolute positions.
@Composable
private fun WelcomeHero(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.background,
                            ),
                    ),
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale20),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale24),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                RatingHighlight()
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PulsingHeartIcon()
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                AvailabilityHighlight()
            }
        }
    }
}

// Decorative badge: the figure is fixed marketing copy, not a value read from
// the database. No story backs an aggregate rating yet, so it is never
// presented as if it were one.
@Composable
private fun RatingHighlight(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .clip(ExtraShapes.featuredCard)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                Text(
                    text = stringResource(R.string.auth_welcome_rating_score),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.auth_welcome_rating_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.auth_welcome_rating_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Same caveat as RatingHighlight: the figure is marketing copy. The dot
// blinks with AvailabilityDot, the same component meant for a professional's
// real "available now" indicator once that story exists.
@Composable
private fun AvailabilityHighlight(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .clip(ExtraShapes.featuredCard)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvailabilityDot()
        Column {
            Text(
                text = stringResource(R.string.auth_welcome_availability_count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.auth_welcome_availability_caption),
                style = MaterialTheme.typography.labelSmall,
                color = SaludEnCasaTheme.statusColors.availableNow,
            )
        }
    }
}

@Composable
private fun WelcomeHighlights(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        Spacer(modifier = Modifier.height(Spacing.scale8))

        Text(
            text = stringResource(R.string.auth_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.auth_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Justify,
        )

        FeatureRow(
            icon = Icons.Filled.Check,
            title = stringResource(R.string.auth_welcome_feature_home_care_title),
            description = stringResource(R.string.auth_welcome_feature_home_care_description),
        )
        FeatureRow(
            icon = Icons.Filled.Place,
            title = stringResource(R.string.auth_welcome_feature_geolocation_title),
            description = stringResource(R.string.auth_welcome_feature_geolocation_description),
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.cardPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Justify,
            )
        }
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
                Text(
                    text = stringResource(R.string.auth_welcome_access_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text =
                        if (uiState is WelcomeUiState.Error) {
                            stringResource(R.string.common_retry)
                        } else {
                            stringResource(R.string.auth_welcome_sign_in_with_google)
                        },
                    onClick = onSignInClick,
                    leadingIcon = {
                        // The multi-color G is a fixed Google brand asset, drawn for a
                        // light background regardless of app theme, hence the literal white.
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_google_logo),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.auth_welcome_no_password_notice),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WelcomeLegalFooter()
            }
        }
    }
}

// Decorative links: there is no destination screen yet, so they do not react
// to touch. Once that story exists, they stop being Text and become real
// controls, as .claude/rules/compose.md requires.
@Composable
private fun WelcomeLegalFooter(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.auth_welcome_legal_footer),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
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
