package bo.saludencasa.features.location.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.domain.model.MapDefaults
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

@Composable
internal fun AddressMap(
    point: Coordinate?,
    camera: MapCamera,
    myLocationEnabled: Boolean,
    onPointPicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Alejado mientras no haya punto elegido, cerca en cuanto lo hay.
    val zoom = if (point == null) MapDefaults.INITIAL_ZOOM else MapDefaults.POINT_ZOOM

    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(camera.target.toLatLng(), zoom)
        }

    // The position is assigned rather than animated on purpose: an animation
    // needs the map to have been measured already, and this effect runs the
    // first time the application relocates the marker, which can happen before
    // the first frame.
    LaunchedEffect(camera.moves) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(camera.target.toLatLng(), zoom)
    }

    val markerState = rememberUpdatedMarkerState((point ?: camera.target).toLatLng())

    LaunchedEffect(markerState) {
        snapshotFlow { markerState.isDragging }
            // The state opens as "not dragging", and that first value is not a
            // marker anyone has dropped.
            .drop(1)
            .filter { dragging -> !dragging }
            .collect { onPointPicked(markerState.position.latitude, markerState.position.longitude) }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentDescription = stringResource(R.string.cd_address_map),
        // The blue dot needs the permission; drawing it without one throws
        // instead of being ignored.
        properties = MapProperties(isMyLocationEnabled = myLocationEnabled),
        uiSettings =
            MapUiSettings(
                zoomControlsEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
            ),
        mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
        onMapClick = { position -> onPointPicked(position.latitude, position.longitude) },
    ) {
        if (point != null) {
            Marker(
                state = markerState,
                draggable = true,
                contentDescription = stringResource(R.string.cd_address_marker),
            )
        }
    }
}

private fun Coordinate.toLatLng(): LatLng = LatLng(latitude, longitude)
