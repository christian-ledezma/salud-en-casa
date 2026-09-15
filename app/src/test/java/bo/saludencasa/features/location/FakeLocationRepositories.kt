package bo.saludencasa.features.location

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressUpdate
import bo.saludencasa.features.location.domain.model.GeocodingResult
import bo.saludencasa.features.location.domain.model.MyAddressResult
import bo.saludencasa.features.location.domain.model.Place
import bo.saludencasa.features.location.domain.model.PositionResult
import bo.saludencasa.features.location.domain.model.SaveAddressResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository
import bo.saludencasa.features.location.domain.repository.IDeviceLocationRepository
import bo.saludencasa.features.location.domain.repository.IGeocodingRepository

class FakeAddressRepository(
    var myAddressResult: MyAddressResult = MyAddressResult.NotRegistered,
    var saveResult: SaveAddressResult? = null,
) : IAddressRepository {
    var reads: Int = 0
        private set
    var saveAttempts: Int = 0
        private set
    var lastUpdate: AddressUpdate? = null
        private set

    override suspend fun getMyPrimaryAddress(): MyAddressResult {
        reads++
        return myAddressResult
    }

    override suspend fun saveAddress(update: AddressUpdate): SaveAddressResult {
        saveAttempts++
        lastUpdate = update
        return saveResult ?: SaveAddressResult.Success(
            address(
                alias = update.alias.value,
                addressText = update.addressText.value,
                reference = update.reference?.value,
                city = update.city.value,
                coordinate = update.coordinate,
            ),
        )
    }
}

class FakeGeocodingRepository(
    var describeResult: GeocodingResult = GeocodingResult.Found(place()),
    var findResult: GeocodingResult = GeocodingResult.Found(place()),
) : IGeocodingRepository {
    var describeCalls: Int = 0
        private set
    var findCalls: Int = 0
        private set
    var lastQuery: String? = null
        private set

    override suspend fun describe(coordinate: Coordinate): GeocodingResult {
        describeCalls++
        return describeResult
    }

    override suspend fun find(query: String): GeocodingResult {
        findCalls++
        lastQuery = query
        return findResult
    }
}

class FakeDeviceLocationRepository(
    var positionResult: PositionResult = PositionResult.Located(coordinate()),
) : IDeviceLocationRepository {
    var positionReads: Int = 0
        private set

    override suspend fun currentPosition(): PositionResult {
        positionReads++
        return positionResult
    }
}

fun coordinate(
    latitude: Double = -16.4957,
    longitude: Double = -68.1335,
): Coordinate = Coordinate.create(latitude, longitude).getOrThrow()

fun place(
    coordinate: Coordinate = coordinate(),
    addressText: String = "Avenida Arce 2081, La Paz",
    city: String = "La Paz",
): Place = Place(coordinate = coordinate, addressText = addressText, city = city)

fun address(
    id: String = "6b1f1f2e-0000-4000-8000-000000000000",
    alias: String = "Casa",
    addressText: String = "Avenida Arce 2081, La Paz",
    reference: String? = "Portón verde",
    city: String = "La Paz",
    coordinate: Coordinate = coordinate(),
    isPrimary: Boolean = true,
): Address =
    Address(
        id = id,
        alias = alias,
        addressText = addressText,
        reference = reference,
        city = city,
        coordinate = coordinate,
        isPrimary = isPrimary,
    )
