package bo.saludencasa.features.location.data.mapper

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.data.model.AddressDto
import bo.saludencasa.features.location.domain.model.AddressUpdate
import bo.saludencasa.features.location.domain.vo.AddressAlias
import bo.saludencasa.features.location.domain.vo.AddressReference
import bo.saludencasa.features.location.domain.vo.AddressText
import bo.saludencasa.features.location.domain.vo.CityName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AddressMapperTest {
    // The failure this test exists for: PostGIS reads the point as longitude
    // first and latitude second. Writing the pair the way it is spoken saves
    // without error and puts a La Paz address in the Southern Ocean, where the
    // proximity search finds nobody and nothing reports why.
    @Test
    fun thePointLeavesWithLongitudeFirst() {
        val ewkt = Coordinate.create(-16.4957, -68.1335).getOrThrow().toEwkt()

        assertEquals("SRID=4326;POINT(-68.1335000 -16.4957000)", ewkt)
    }

    // A device set to Spanish formats a decimal with a comma, and PostGIS reads
    // a comma as the separator between two points of a geometry.
    @Test
    fun thePointLeavesWithADecimalPointWhateverTheDeviceLanguageIs() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-BO"))

            assertTrue(
                Coordinate
                    .create(-16.5, -68.1)
                    .getOrThrow()
                    .toEwkt()
                    .contains("-68.1000000"),
            )
        } finally {
            Locale.setDefault(previous)
        }
    }

    // A coordinate close to the origin is where a double turns into scientific
    // notation, which is not text PostGIS parses.
    @Test
    fun aPointNearTheOriginLeavesAsPlainDigits() {
        val ewkt = Coordinate.create(0.0000012, -0.0000034).getOrThrow().toEwkt()

        assertEquals("SRID=4326;POINT(-0.0000034 0.0000012)", ewkt)
    }

    @Test
    fun theStoredRowComesBackAsTheSamePoint() {
        val address = dto(latitude = -16.4957, longitude = -68.1335).toAddress()

        assertEquals(-16.4957, address?.coordinate?.latitude)
        assertEquals(-68.1335, address?.coordinate?.longitude)
    }

    // A reference saved as spaces by an earlier version of the form would show
    // as a filled field that says nothing.
    @Test
    fun aBlankStoredReferenceIsReadAsAbsent() {
        assertNull(dto(reference = "   ").toAddress()?.reference)
    }

    @Test
    fun theSaveCarriesTheColumnsTheTableExpects() {
        val row = update().toAddressRow("08ddb28f-0000-4000-8000-000000000000")

        assertEquals("08ddb28f-0000-4000-8000-000000000000", row.profileId)
        assertEquals("Casa", row.alias)
        assertEquals("Avenida Arce 2081, La Paz", row.addressText)
        assertEquals("La Paz", row.city)
        assertEquals("Portón verde", row.reference)
    }
}

private fun dto(
    latitude: Double = -16.4957,
    longitude: Double = -68.1335,
    reference: String? = "Portón verde",
): AddressDto =
    AddressDto(
        id = "6b1f1f2e-0000-4000-8000-000000000000",
        alias = "Casa",
        addressText = "Avenida Arce 2081, La Paz",
        reference = reference,
        city = "La Paz",
        latitude = latitude,
        longitude = longitude,
        isPrimary = true,
    )

private fun update(): AddressUpdate =
    AddressUpdate(
        id = null,
        alias = AddressAlias.create("Casa").getOrThrow(),
        addressText = AddressText.create("Avenida Arce 2081, La Paz").getOrThrow(),
        reference = AddressReference.create("Portón verde").getOrThrow(),
        city = CityName.create("La Paz").getOrThrow(),
        coordinate = Coordinate.create(-16.4957, -68.1335).getOrThrow(),
    )
