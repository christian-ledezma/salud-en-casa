package bo.saludencasa.features.search.data.mapper

import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.search.data.model.NearbyProfessionalDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class NearbyProfessionalMapperTest {
    private fun row(
        professionalType: String? = "NURSE",
        rate: BigDecimal? = BigDecimal("180.00"),
        distanceM: Double = 1100.0,
        latitude: Double? = -17.368,
        longitude: Double? = -66.174,
    ) = NearbyProfessionalDto(
        id = "f1b0c0de-0000-4000-8000-000000000000",
        fullName = "Ana Pérez",
        professionalType = professionalType,
        baseRateBob = rate,
        distanceM = distanceM,
        baseLatitude = latitude,
        baseLongitude = longitude,
    )

    // The rate is the figure the patient decides with, and the engine's own
    // check constraint guarantees it for every approved professional. A row
    // without one is a row this version cannot read, so it refuses rather than
    // drawing a card with no price.
    @Test
    fun aRowWithoutARateCannotBeRead() {
        assertNull(row(rate = null).toNearbyProfessional())
    }

    // A professional type this version does not know -- because a later
    // migration added one -- is a label, not a reason to take a verified
    // professional off the map of an older copy of the application.
    @Test
    fun anUnknownProfessionalTypeLeavesTheResultStandingWithoutItsLabel() {
        val mapped = row(professionalType = "MIDWIFE").toNearbyProfessional()

        assertNotNull(mapped)
        assertNull(mapped?.professionalType)
    }

    @Test
    fun aKnownProfessionalTypeIsRead() {
        assertEquals(ProfessionalType.NURSE, row().toNearbyProfessional()?.professionalType)
    }

    // Latitude and longitude are both negative here and each fits the other's
    // range, so swapping them produces a valid coordinate off the coast of
    // Africa instead of an error.
    @Test
    fun theBasePointKeepsLatitudeAndLongitudeInTheirPlaces() {
        val point = row().toNearbyProfessional()?.basePoint

        assertEquals(-17.368, point?.latitude)
        assertEquals(-66.174, point?.longitude)
    }

    // Without a usable point the professional has no pin, and that is all:
    // their card still carries the distance, the rate and the reputation, and
    // dropping the row would lose a valid result.
    @Test
    fun aRowWithoutAUsablePointKeepsEverythingElse() {
        val mapped = row(latitude = null, longitude = null).toNearbyProfessional()

        assertNotNull(mapped)
        assertNull(mapped?.basePoint)
        assertEquals(1100, mapped?.distanceM)
    }

    @Test
    fun aPointOutOfRangeIsNoPointAtAll() {
        assertNull(row(latitude = 95.0).toNearbyProfessional()?.basePoint)
    }

    @Test
    fun theDistanceIsReadAsWholeMetres() {
        assertEquals(1100, row(distanceM = 1100.0).toNearbyProfessional()?.distanceM)
        assertEquals(0, row(distanceM = 0.0).toNearbyProfessional()?.distanceM)
    }
}
