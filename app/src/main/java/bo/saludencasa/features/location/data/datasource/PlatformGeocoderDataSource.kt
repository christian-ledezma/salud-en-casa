package bo.saludencasa.features.location.data.datasource

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// The platform geocoder, not the map provider's billed one. Resolving an
// address this way costs the project nothing, which is the same reason INV-08
// keeps the proximity search inside the database (docs/decisions.md,
// 2026-09-05).
class PlatformGeocoderDataSource(
    private val context: Context,
) {
    // A device without Play Services, and every bare emulator image, has no
    // geocoder at all. RF-03.6 asks the screen to keep working, so the caller
    // needs to be able to tell that apart from a lookup that found nothing.
    fun isAvailable(): Boolean = Geocoder.isPresent()

    suspend fun describe(
        latitude: Double,
        longitude: Double,
    ): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            awaitResults { listener -> geocoder().getFromLocation(latitude, longitude, MAX_RESULTS, listener) }
        } else {
            describeBlocking(latitude, longitude)
        }

    suspend fun find(query: String): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            awaitResults { listener -> geocoder().getFromLocationName(query, MAX_RESULTS, listener) }
        } else {
            findBlocking(query)
        }

    private fun geocoder(): Geocoder = Geocoder(context, Locale.getDefault())

    // The listener is the supported way from API 33 onward. The annotation is
    // what lets static analysis see that the two call sites above are guarded.
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun awaitResults(request: (Geocoder.GeocodeListener) -> Unit): List<Address> =
        suspendCancellableCoroutine { continuation ->
            request(
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses.toList())
                    }

                    override fun onError(message: String?) {
                        if (continuation.isActive) continuation.resumeWithException(IOException(message.orEmpty()))
                    }
                },
            )
        }

    // The two calls below were deprecated in favour of the listener above, which
    // only exists from API 33. minSdk is 26, so the older devices still need
    // them, and they block, which is why they run off the main thread.
    @Suppress("DEPRECATION")
    private suspend fun describeBlocking(
        latitude: Double,
        longitude: Double,
    ): List<Address> =
        withContext(Dispatchers.IO) {
            geocoder().getFromLocation(latitude, longitude, MAX_RESULTS).orEmpty()
        }

    @Suppress("DEPRECATION")
    private suspend fun findBlocking(query: String): List<Address> =
        withContext(Dispatchers.IO) {
            geocoder().getFromLocationName(query, MAX_RESULTS).orEmpty()
        }

    private companion object {
        const val MAX_RESULTS = 1
    }
}
