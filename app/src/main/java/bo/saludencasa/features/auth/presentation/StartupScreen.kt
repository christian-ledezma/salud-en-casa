package bo.saludencasa.features.auth.presentation

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.rememberAnimationsEnabled
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.presentation.messageRes
import bo.saludencasa.ui.animations.HeartbeatTrace
import bo.saludencasa.ui.animations.INTRO_MILLIS
import bo.saludencasa.ui.animations.SETTLED_MARK_SCALE
import bo.saludencasa.ui.animations.SWEEP_MILLIS
import bo.saludencasa.ui.animations.SWEEP_START_MILLIS
import bo.saludencasa.ui.animations.Sweep
import bo.saludencasa.ui.animations.TRACE_ASPECT_RATIO
import bo.saludencasa.ui.animations.beatScale
import bo.saludencasa.ui.animations.firstSweep
import bo.saludencasa.ui.animations.markSettle
import bo.saludencasa.ui.animations.wordmarkReveal
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun StartupScreen(
    playIntro: Boolean,
    onSignIn: () -> Unit,
    onChooseRole: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartupViewModel = koinViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    var introFinished by rememberSaveable { mutableStateOf(!playIntro) }

    // docs/decisions.md, 2026-10-13, the startup screen.
    LaunchedEffect(destination, introFinished) {
        if (!introFinished) return@LaunchedEffect
        when (destination) {
            StartupDestination.Loading, is StartupDestination.Error -> Unit
            StartupDestination.SignIn -> onSignIn()
            StartupDestination.ChooseRole -> onChooseRole()
            StartupDestination.Home -> onHome()
        }
    }

    StartupContent(
        destination = destination,
        playIntro = playIntro && !introFinished,
        onRetryClick = viewModel::retry,
        modifier = modifier,
        onIntroFinished = { introFinished = true },
    )
}

@Composable
internal fun StartupContent(
    destination: StartupDestination,
    playIntro: Boolean,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onIntroFinished: () -> Unit = {},
) {
    val animate = rememberAnimationsEnabled() && !LocalInspectionMode.current
    val intro = remember { Animatable(if (playIntro && animate) 0f else INTRO_MILLIS.toFloat()) }
    val currentOnIntroFinished by rememberUpdatedState(onIntroFinished)
    LaunchedEffect(intro) {
        // A tween to the value it already holds would still take its full duration.
        if (intro.value < INTRO_MILLIS) {
            intro.animateTo(INTRO_MILLIS.toFloat(), tween(durationMillis = INTRO_MILLIS, easing = LinearEasing))
        }
        currentOnIntroFinished()
    }
    val introOver by remember { derivedStateOf { intro.value >= INTRO_MILLIS } }
    val loop = if (animate && introOver && destination == StartupDestination.Loading) rememberSweepLoop() else null

    val sweep = { loop?.let { Sweep(head = it.value, repeated = true) } ?: firstSweep(intro.value) }
    val beat = { beatScale(loop?.let { it.value * SWEEP_MILLIS } ?: (intro.value - SWEEP_START_MILLIS)) }
    var markLift by remember { mutableStateOf(Offset.Zero) }
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(Spacing.brandMarkSize * SETTLED_MARK_SCALE)
                        .onPlaced { markLift = it.offsetToRootCenter() },
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_brand_mark),
                    contentDescription = null,
                    modifier =
                        Modifier.fillMaxSize().graphicsLayer {
                            val settle = markSettle(intro.value)
                            val scale = (1f / SETTLED_MARK_SCALE + (1f - 1f / SETTLED_MARK_SCALE) * settle) * beat()
                            translationX = markLift.x * (1f - settle)
                            translationY = markLift.y * (1f - settle)
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_brand_wordmark),
                contentDescription = stringResource(R.string.app_name),
                modifier =
                    Modifier.graphicsLayer {
                        val reveal = wordmarkReveal(intro.value)
                        alpha = reveal
                        translationY = (1f - reveal) * Spacing.scale12.toPx()
                    },
            )
            Spacer(Modifier.height(Spacing.scale8))
            if (destination is StartupDestination.Error) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(destination.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetryClick)
                }
            } else {
                HeartbeatTrace(
                    sweep = sweep,
                    modifier =
                        Modifier
                            .widthIn(max = Spacing.brandTraceMaxWidth)
                            .fillMaxWidth()
                            .semantics { contentDescription = loadingDescription },
                )
            }
        }
    }
}

internal data class BrandPart(
    val center: Offset,
    val width: Float,
)

internal data class SettledStartupBrand(
    val mark: BrandPart,
    val wordmark: BrandPart,
    val trace: BrandPart,
)

// Where the settled frame of StartupContent leaves each part of the brand, from
// the top left of the screen area. WelcomeContent starts its entrance here, and
// StartupBrandGeometryTest holds this to the layout above.
internal fun Density.settledStartupBrand(
    container: Size,
    wordmark: Size,
): SettledStartupBrand {
    val mark = (Spacing.brandMarkSize * SETTLED_MARK_SCALE).toPx()
    val traceWidth = min(Spacing.brandTraceMaxWidth.toPx(), container.width - 2 * Spacing.screenMargin.toPx())
    val traceHeight = traceWidth / TRACE_ASPECT_RATIO
    val gap = Spacing.scale8.toPx()
    val height = mark + wordmark.height + gap + traceHeight
    val top = max((container.height - height) / 2, Spacing.sectionGap.toPx())
    val x = container.width / 2
    return SettledStartupBrand(
        mark = BrandPart(Offset(x, top + mark / 2), mark),
        wordmark = BrandPart(Offset(x, top + mark + wordmark.height / 2), wordmark.width),
        trace = BrandPart(Offset(x, top + mark + wordmark.height + gap + traceHeight / 2), traceWidth),
    )
}

// Picks up the mark where the system splash left it, at the center of the window.
private fun LayoutCoordinates.offsetToRootCenter(): Offset {
    val root = findRootCoordinates()
    val center = positionInRoot() + Offset(size.width / 2f, size.height / 2f)
    return Offset(root.size.width / 2f, root.size.height / 2f) - center
}

@Composable
private fun rememberSweepLoop(): State<Float> =
    rememberInfiniteTransition(label = "heartbeat").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = SWEEP_MILLIS, easing = LinearEasing)),
        label = "heartbeat_sweep",
    )

@Preview(showBackground = true, name = "Arranque, claro")
@Composable
private fun StartupLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        StartupContent(destination = StartupDestination.Loading, playIntro = true, onRetryClick = {})
    }
}

@Preview(showBackground = true, name = "Arranque, oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StartupDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        StartupContent(destination = StartupDestination.Loading, playIntro = true, onRetryClick = {})
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f, name = "Arranque al 200 %")
@Composable
private fun StartupLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        StartupContent(destination = StartupDestination.Loading, playIntro = true, onRetryClick = {})
    }
}

@Preview(showBackground = true, name = "Arranque con error")
@Composable
private fun StartupErrorPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        StartupContent(
            destination = StartupDestination.Error(ProfileError.NetworkUnavailable),
            playIntro = true,
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2f, name = "Arranque con error al 200 %")
@Composable
private fun StartupErrorLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        StartupContent(
            destination = StartupDestination.Error(ProfileError.NetworkUnavailable),
            playIntro = true,
            onRetryClick = {},
        )
    }
}
