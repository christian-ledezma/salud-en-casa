package bo.saludencasa.features.auth.presentation

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.rememberAnimationsEnabled
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.ui.animations.HeartbeatTrace
import bo.saludencasa.ui.animations.SETTLED_MARK_SCALE
import bo.saludencasa.ui.animations.Sweep
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.pow

// The brand on this screen is the startup's settled brand at this scale, so the
// entrance only has to shrink it and move it.
internal const val WELCOME_BRAND_SCALE = 0.82f

private const val BRAND_MOVE_MILLIS = 600
private const val CONTENT_REVEAL_START_MILLIS = 350
private const val CONTENT_REVEAL_MILLIS = 500
private const val ENTRANCE_MILLIS = CONTENT_REVEAL_START_MILLIS + CONTENT_REVEAL_MILLIS

// The ink of the mark spans 49 % of its canvas height; the rest is the safe zone
// the launcher and the system splash need, and here it would only push the
// name away.
private const val MARK_ROW_HEIGHT = 0.56f

private const val LARGE_FONT_SCALE = 1.5f

@Composable
fun WelcomeScreen(
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
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
        onTermsClick = onOpenTerms,
        onPrivacyClick = onOpenPrivacy,
        modifier = modifier,
    )
}

@Composable
internal fun WelcomeContent(
    uiState: WelcomeUiState,
    onSignInClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animate = rememberAnimationsEnabled() && !LocalInspectionMode.current
    var entranceDone by rememberSaveable { mutableStateOf(false) }
    val entrance = remember { Animatable(if (animate && !entranceDone) 0f else ENTRANCE_MILLIS.toFloat()) }
    LaunchedEffect(entrance) {
        if (entrance.value < ENTRANCE_MILLIS) {
            entrance.animateTo(
                targetValue = ENTRANCE_MILLIS.toFloat(),
                animationSpec = tween(durationMillis = ENTRANCE_MILLIS, easing = LinearEasing),
            )
        }
        entranceDone = true
    }
    val brandProgress = { easeInOut(entrance.value / BRAND_MOVE_MILLIS) }
    val contentReveal = { easeOut((entrance.value - CONTENT_REVEAL_START_MILLIS) / CONTENT_REVEAL_MILLIS) }

    val density = LocalDensity.current
    val wordmark = painterResource(R.drawable.ic_brand_wordmark)
    val startupWordmark = wordmark.intrinsicSize
    val container = remember { mutableStateOf(Rect.Zero) }
    val markBounds = remember { mutableStateOf(Rect.Zero) }
    val wordmarkBounds = remember { mutableStateOf(Rect.Zero) }
    val traceBounds = remember { mutableStateOf(Rect.Zero) }
    val startup = {
        with(density) { settledStartupBrand(container.value.size, startupWordmark) }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPlaced { container.value = it.unclippedBounds() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val markSize = Spacing.brandMarkSize * SETTLED_MARK_SCALE * WELCOME_BRAND_SCALE
            Box(
                modifier =
                    Modifier
                        .size(width = markSize, height = markSize * MARK_ROW_HEIGHT)
                        .onPlaced { markBounds.value = it.unclippedBounds() }
                        .entranceFrom(markBounds, container, brandProgress) { startup().mark },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_brand_mark),
                    contentDescription = null,
                    modifier = Modifier.requiredSize(markSize),
                )
            }
            Spacer(Modifier.height(Spacing.scale4))
            Image(
                painter = wordmark,
                contentDescription = stringResource(R.string.app_name),
                modifier =
                    Modifier
                        .width(with(density) { startupWordmark.width.toDp() } * WELCOME_BRAND_SCALE)
                        .onPlaced { wordmarkBounds.value = it.unclippedBounds() }
                        .entranceFrom(wordmarkBounds, container, brandProgress) { startup().wordmark },
            )
            HeartbeatTrace(
                sweep = { Sweep(head = 1f, repeated = false) },
                modifier =
                    Modifier
                        .width(Spacing.brandTraceMaxWidth * WELCOME_BRAND_SCALE)
                        .onPlaced { traceBounds.value = it.unclippedBounds() }
                        .entranceFrom(traceBounds, container, brandProgress) { startup().trace },
            )

            Column(
                modifier = Modifier.reveal(contentReveal),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.auth_welcome_slogan),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.scale20))
                WelcomeValues()
                Spacer(Modifier.height(Spacing.sectionGap))
                SignInSection(
                    uiState = uiState,
                    onSignInClick = onSignInClick,
                    onTermsClick = onTermsClick,
                    onPrivacyClick = onPrivacyClick,
                )
            }
        }
    }
}

// Moves a part of the brand from where the settled startup frame left it to its
// own place on this screen (docs/decisions.md, 2026-10-14, the welcome screen).
private fun Modifier.entranceFrom(
    bounds: MutableState<Rect>,
    container: MutableState<Rect>,
    progress: () -> Float,
    start: () -> BrandPart,
): Modifier =
    graphicsLayer {
        val remaining = 1f - progress()
        val own = bounds.value
        if (remaining == 0f || own.isEmpty || container.value.isEmpty) return@graphicsLayer
        val from = start()
        val here = own.center - container.value.topLeft
        translationX = (from.center.x - here.x) * remaining
        translationY = (from.center.y - here.y) * remaining
        val scale = 1f + (from.width / own.width - 1f) * remaining
        scaleX = scale
        scaleY = scale
    }

// Not boundsInRoot: that one is clipped by the scrolling column.
private fun LayoutCoordinates.unclippedBounds(): Rect = Rect(positionInRoot(), size.toSize())

private fun Modifier.reveal(progress: () -> Float): Modifier =
    graphicsLayer {
        val shown = progress()
        alpha = shown
        translationY = (1f - shown) * Spacing.scale16.toPx()
    }

@Composable
private fun WelcomeValues(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface),
    ) {
        ValueRow(
            icon = painterResource(R.drawable.ic_verified_user),
            title = stringResource(R.string.auth_welcome_value_verified_title),
            description = stringResource(R.string.auth_welcome_value_verified_description),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ValueRow(
            icon = rememberVectorPainter(Icons.Filled.Place),
            title = stringResource(R.string.auth_welcome_value_nearby_title),
            description = stringResource(R.string.auth_welcome_value_nearby_description),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ValueRow(
            icon = painterResource(R.drawable.ic_group),
            title = stringResource(R.string.auth_welcome_value_roles_title),
            description = stringResource(R.string.auth_welcome_value_roles_description),
        )
    }
}

// At a large font the icon goes above the text: beside it, a title such as
// «Profesionales verificados» is left a third of the width and breaks mid-word.
@Composable
private fun ValueRow(
    icon: Painter,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val badge =
        @Composable {
            Box(
                modifier =
                    Modifier
                        .size(Spacing.scale40)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Spacing.scale20),
                )
            }
        }
    val text =
        @Composable {
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
                )
            }
        }
    val rowModifier = modifier.fillMaxWidth().padding(Spacing.cardPadding)
    if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
        Column(modifier = rowModifier, verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
            badge()
            text()
        }
    } else {
        Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(Spacing.scale12)) {
            badge()
            text()
        }
    }
}

@Composable
private fun SignInSection(
    uiState: WelcomeUiState,
    onSignInClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (uiState is WelcomeUiState.Error) {
            Text(
                text = stringResource(uiState.error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
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
                    leadingIcon = { GoogleMark() },
                    containerColor = SaludEnCasaTheme.brandColors.button,
                    contentColor = SaludEnCasaTheme.brandColors.onButton,
                )
            }
        }

        Text(
            text = stringResource(R.string.auth_welcome_no_password_notice),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.auth_welcome_legal_notice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // No destination yet: the legal screens are their own story
        // (docs/decisions.md, 2026-10-14, the welcome screen).
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8, Alignment.CenterHorizontally),
        ) {
            TextButton(onClick = onTermsClick) {
                Text(text = stringResource(R.string.auth_welcome_terms), textAlign = TextAlign.Center)
            }
            TextButton(onClick = onPrivacyClick) {
                Text(text = stringResource(R.string.auth_welcome_privacy), textAlign = TextAlign.Center)
            }
        }
    }
}

// The multi-color G is a fixed Google brand asset, drawn for a light background
// regardless of app theme, hence the literal white. requiredSize keeps the circle
// round when a large font squeezes the row it sits in.
@Composable
private fun GoogleMark() {
    Box(
        modifier =
            Modifier
                .requiredSize(24.dp)
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
}

private fun easeOut(fraction: Float): Float = 1f - (1f - fraction.coerceIn(0f, 1f)).pow(3)

private fun easeInOut(fraction: Float): Float {
    val t = fraction.coerceIn(0f, 1f)
    return if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
}

@Preview(showBackground = true, heightDp = 900, name = "Bienvenida en reposo, claro")
@Composable
private fun WelcomeIdleLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        WelcomeContent(uiState = WelcomeUiState.Idle, onSignInClick = {}, onTermsClick = {}, onPrivacyClick = {})
    }
}

@Preview(
    showBackground = true,
    heightDp = 900,
    name = "Bienvenida en reposo, oscuro",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WelcomeIdleDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        WelcomeContent(uiState = WelcomeUiState.Idle, onSignInClick = {}, onTermsClick = {}, onPrivacyClick = {})
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 1800, fontScale = 2f, name = "Bienvenida al 200 %")
@Composable
private fun WelcomeIdleLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        WelcomeContent(uiState = WelcomeUiState.Idle, onSignInClick = {}, onTermsClick = {}, onPrivacyClick = {})
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Bienvenida ingresando")
@Composable
private fun WelcomeSigningInPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        WelcomeContent(uiState = WelcomeUiState.SigningIn, onSignInClick = {}, onTermsClick = {}, onPrivacyClick = {})
    }
}

@Preview(
    showBackground = true,
    heightDp = 900,
    name = "Bienvenida con error, oscuro",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WelcomeErrorDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        WelcomeContent(
            uiState = WelcomeUiState.Error(AuthError.NoGoogleAccount),
            onSignInClick = {},
            onTermsClick = {},
            onPrivacyClick = {},
        )
    }
}
