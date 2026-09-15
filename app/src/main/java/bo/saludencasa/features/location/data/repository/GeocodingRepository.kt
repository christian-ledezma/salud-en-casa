package bo.saludencasa.features.location.data.repository

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.data.datasource.PlatformGeocoderDataSource
import bo.saludencasa.features.location.data.mapper.toAddressError
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.GeocodingResult
import bo.saludencasa.features.location.domain.model.Place
import bo.saludencasa.features.location.domain.repository.IGeocodingRepository
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import android.location.Address as PlatformAddress

// RF-03.3: a result already obtained is not asked for again. The lasting copy
// is the address_text column, which is why reopening a saved address consults
// nothing; these two maps only cover the same screen asking twice, which is what
// dragging the marker back and searching the same words do.
class GeocodingRepository(
    private val geocoder: PlatformGeocoderDataSource,
) : IGeocodingRepository {
    private val describedPoints = ConcurrentHashMap<String, Place>()
    private val foundQueries = ConcurrentHashMap<String, Place>()

    override suspend fun describe(coordinate: Coordinate): GeocodingResult {
        if (!geocoder.isAvailable()) return GeocodingResult.Failure(AddressError.GeocoderUnavailable)

        val key = keyOf(coordinate)
        describedPoints[key]?.let { return GeocodingResult.Found(it) }

        return lookUp(
            request = { geocoder.describe(coordinate.latitude, coordinate.longitude) },
            at = { coordinate },
            remember = { place -> describedPoints[key] = place },
        )
    }

    override suspend fun find(query: String): GeocodingResult {
        if (!geocoder.isAvailable()) return GeocodingResult.Failure(AddressError.GeocoderUnavailable)

        val key = query.trim().lowercase(Locale.ROOT)
        foundQueries[key]?.let { return GeocodingResult.Found(it) }

        return lookUp(
            request = { geocoder.find(query) },
            at = { found -> Coordinate.create(found.latitude, found.longitude).getOrNull() },
            remember = { place -> foundQueries[key] = place },
        )
    }

    private suspend fun lookUp(
        request: suspend () -> List<PlatformAddress>,
        at: (PlatformAddress) -> Coordinate?,
        remember: (Place) -> Unit,
    ): GeocodingResult =
        try {
            val place =
                request().firstOrNull()?.let { found ->
                    at(found)?.let { coordinate -> found.toPlace(coordinate) }
                } ?: return GeocodingResult.NotFound
            remember(place)
            GeocodingResult.Found(place)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            GeocodingResult.Failure(failure.toAddressError())
        }
}

private const val KEY_DECIMALS = 5

// Two points a metre apart are the same doorway. Keeping the full precision of
// the double would make the map ask again for every pixel the finger moved.
private fun keyOf(coordinate: Coordinate): String =
    String.format(Locale.ROOT, "%.${KEY_DECIMALS}f,%.${KEY_DECIMALS}f", coordinate.latitude, coordinate.longitude)

private fun PlatformAddress.toPlace(coordinate: Coordinate): Place? {
    val line = getAddressLine(0)?.trim().orEmpty()
    val city = listOfNotNull(locality, subAdminArea, adminArea).firstOrNull { it.isNotBlank() }.orEmpty()
    return if (line.isEmpty() && city.isEmpty()) {
        null
    } else {
        Place(coordinate = coordinate, addressText = line, city = city)
    }
}
