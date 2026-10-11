@file:OptIn(MapsComposeExperimentalApi::class)

package bo.saludencasa.features.search.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.core.util.rememberAnimationsEnabled
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import bo.saludencasa.ui.animations.RADAR_CYCLE_MILLIS
import bo.saludencasa.ui.animations.RADAR_RING_COUNT
import bo.saludencasa.ui.animations.RADAR_START_ALPHA
import bo.saludencasa.ui.animations.REST_PROGRESS
import bo.saludencasa.ui.animations.radarRingProgress
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import java.math.BigDecimal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln

@Composable
internal fun ProfessionalSearchMap(
    model: SearchMapModel,
    onProfessionalClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var myLocationEnabled by remember { mutableStateOf(context.hasLocationPermission()) }

    // Read again on every resume rather than kept from the launcher's answer: a
    // permission revoked in the system settings would otherwise leave a stale
    // true behind, and isMyLocationEnabled throws without it.
    LifecycleResumeEffect(Unit) {
        myLocationEnabled = context.hasLocationPermission()
        onPauseOrDispose {}
    }

    var firstFrame by remember { mutableStateOf(true) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            myLocationEnabled = granted.values.any { it }
        }

    val target = LatLng(model.origin.latitude, model.origin.longitude)

    val cameraPositionState =
        rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(target, INITIAL_ZOOM) }

    BoxWithConstraints(modifier = modifier) {
        // The shorter side of the box the map really occupies, and not the width
        // of the screen: the map is inset by the page margin and is 280 dp tall,
        // so framing the circle against the screen width would push its edge and
        // the results nearest it out of sight, on both axes.
        val sideDp = minOf(maxWidth.value, maxHeight.value)
        val zoom = fitZoom(model.radius.km, model.origin.latitude, sideDp)

        // The first position is assigned and only the later ones animate: the
        // camera cannot be animated into a map that has not been measured yet
        // (docs/decisions.md, 2026-09-15, and HU-05's AddressMap).
        LaunchedEffect(target, zoom) {
            val update = CameraUpdateFactory.newLatLngZoom(target, zoom)
            if (firstFrame) {
                firstFrame = false
                cameraPositionState.move(update)
            } else {
                cameraPositionState.animate(update, CAMERA_MILLIS)
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentDescription = stringResource(R.string.cd_search_map),
            properties = MapProperties(isMyLocationEnabled = myLocationEnabled),
            uiSettings =
                MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                ),
            mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
            // A tap on the map and not on a pin puts the card away.
            onMapClick = { onProfessionalClick(null) },
        ) {
            SearchRadarCircles(
                center = target,
                radiusM = model.radius.km * METRES_IN_A_KILOMETRE,
                searching = model.searching,
            )

            Clustering(
                items = model.professionals.mapNotNull { it.toPin() },
                // True consumes the tap, which is what keeps the SDK from
                // opening its own info window over the card we are about to
                // raise below the map.
                onClusterItemClick = { pin ->
                    onProfessionalClick(pin.id)
                    true
                },
                clusterContent = { cluster -> ClusterBubble(count = cluster.size) },
                clusterItemContent = { pin -> RatePin(pin = pin, selected = pin.id == model.selectedId) },
            )
        }

        if (!myLocationEnabled) {
            FilledIconButton(
                onClick = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.scale12),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = stringResource(R.string.cd_allow_location),
                )
            }
        }
    }
}

// Three rings growing out of the patient's point, the shape Uber and InDrive use
// while they look around you. Drawn as map circles rather than on a canvas over
// the map, so they stay anchored to the ground and scale with the zoom without
// projecting anything to screen coordinates.
@Composable
@GoogleMapComposable
private fun SearchRadarCircles(
    center: LatLng,
    radiusM: Int,
    searching: Boolean,
) {
    val animationsEnabled = rememberAnimationsEnabled()

    // The radius itself: the filter made visible, and the one piece of motion
    // that explains a choice instead of decorating it.
    val animatedRadius by
        animateFloatAsState(
            targetValue = radiusM.toFloat(),
            animationSpec =
                if (animationsEnabled) {
                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                } else {
                    snap()
                },
            label = "search_radius",
        )

    Circle(
        center = center,
        radius = animatedRadius.toDouble(),
        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = RADIUS_FILL_ALPHA),
        strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = RADIUS_STROKE_ALPHA),
        strokeWidth = RADIUS_STROKE_WIDTH,
    )

    if (!searching) return

    val progress =
        if (animationsEnabled) {
            val transition = rememberInfiniteTransition(label = "map_radar")
            val value by
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = RADAR_CYCLE_MILLIS, easing = LinearEasing),
                        ),
                    label = "map_radar_progress",
                )
            value
        } else {
            REST_PROGRESS
        }

    repeat(RADAR_RING_COUNT) { ring ->
        val ringProgress = radarRingProgress(progress, ring)
        Circle(
            center = center,
            radius = (animatedRadius * ringProgress).toDouble(),
            fillColor = Color.Transparent,
            strokeColor =
                MaterialTheme.colorScheme.primary.copy(alpha = RADAR_START_ALPHA * (1f - ringProgress)),
            strokeWidth = RADIUS_STROKE_WIDTH,
        )
    }
}

// The rate on the pin, which is what these applications put there. The pin is
// captured to a bitmap, so nothing inside it can animate: the availability dot
// is drawn still here and pulses only on the card.
@Composable
private fun RatePin(
    pin: ProfessionalPin,
    selected: Boolean,
) {
    val locale = LocalConfiguration.current.locales[0]
    val container =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content =
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier =
            Modifier
                .clip(ExtraShapes.pill)
                .background(container)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pin.availableNow) {
            Box(
                modifier =
                    Modifier
                        .size(Spacing.scale8)
                        .clip(CircleShape)
                        .background(SaludEnCasaTheme.statusColors.availableNow),
            )
        }
        Text(
            text = formatBob(pin.rateBob, locale),
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

@Composable
private fun ClusterBubble(count: Int) {
    Box(
        modifier =
            Modifier
                .size(CLUSTER_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

// The cluster manager re-renders a pin whose identity changed, so this is a data
// class keyed by the professional: without stable equality every page would
// redraw every bitmap.
internal data class ProfessionalPin(
    val id: String,
    val rateBob: BigDecimal,
    val availableNow: Boolean,
    override val position: LatLng,
) : ClusterItem {
    override val title: String? = null
    override val snippet: String? = null
    override val zIndex: Float? = null
}

private fun NearbyProfessional.toPin(): ProfessionalPin? =
    basePoint?.let { point ->
        ProfessionalPin(
            id = professionalId,
            rateBob = baseRateBob,
            availableNow = availableNow,
            position = LatLng(point.latitude, point.longitude),
        )
    }

// The zoom that fits a circle of this radius across the screen. Computed rather
// than asked of newLatLngBounds, which throws when the map has not been laid out
// yet -- the real shape of HU-05's "animating needs a measured map".
internal fun fitZoom(
    radiusKm: Int,
    latitude: Double,
    sideDp: Float,
): Float {
    val metresPerPixel = radiusKm * 2.0 * METRES_IN_A_KILOMETRE / sideDp
    val metresPerPixelAtZoomZero = EQUATOR_METRES_PER_PIXEL * cos(latitude * PI / 180.0)

    return (ln(metresPerPixelAtZoomZero / metresPerPixel) / ln(2.0)).toFloat()
}

private fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

private val LOCATION_PERMISSIONS =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

private const val METRES_IN_A_KILOMETRE = 1_000
private const val CAMERA_MILLIS = 600

// Only the seed of the camera state, replaced by the measured zoom on the first
// frame. MapDefaults keeps the same value for the address map.
private const val INITIAL_ZOOM = 15f
private const val RADIUS_FILL_ALPHA = 0.08f
private const val RADIUS_STROKE_ALPHA = 0.5f
private const val RADIUS_STROKE_WIDTH = 4f
private val CLUSTER_SIZE = Spacing.scale40

// A tile is 256 pixels wide at zoom zero and the equator measures 40075016.686 m.
private const val EQUATOR_METRES_PER_PIXEL = 40_075_016.686 / 256.0
