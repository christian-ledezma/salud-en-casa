package bo.saludencasa.features.location.data.datasource

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DeviceLocationDataSource(
    private val context: Context,
) {
    fun hasPermission(): Boolean =
        PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    // The permission is checked by hasPermission() before this runs and the
    // caller reports a denial as its own outcome; static analysis cannot see
    // across the two methods.
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            LocationServices
                .getFusedLocationProviderClient(context)
                // Balanced accuracy is a city block, which is where the map
                // opens; the exact point is the one the person drops the marker
                // on, and the fine fix costs battery to arrive at a position
                // they are about to correct anyway.
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                .addOnSuccessListener { location -> if (continuation.isActive) continuation.resume(location) }
                .addOnFailureListener { failure ->
                    if (continuation.isActive) continuation.resumeWithException(failure)
                }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }

    private companion object {
        val PERMISSIONS = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
