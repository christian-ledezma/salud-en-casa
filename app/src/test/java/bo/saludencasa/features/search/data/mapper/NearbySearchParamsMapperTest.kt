package bo.saludencasa.features.search.data.mapper

import bo.saludencasa.features.location.coordinate
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.domain.model.SearchRadius
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbySearchParamsMapperTest {
    private val origin = coordinate(latitude = -17.3835, longitude = -66.1568)

    private fun params(criteria: SearchCriteria) = criteria.toParams(origin, offset = 0, limit = 20)

    // The engine reads this parameter in three states: null does not filter,
    // true keeps the available ones, and false keeps the UNAVAILABLE ones. An
    // off filter travelling as false is the one mistake here that returns a
    // full screen of results, all of them wrong.
    @Test
    fun anAvailabilityFilterThatIsOffTravelsAsNothingAndNotAsFalse() {
        assertNull(params(SearchCriteria(availableNowOnly = false)).availableNow)
    }

    @Test
    fun anAvailabilityFilterThatIsOnAsksForTheAvailableOnes() {
        assertTrue(params(SearchCriteria(availableNowOnly = true)).availableNow == true)
    }

    // Both of this country's coordinates are negative and both fit inside the
    // other one's range, so swapping them raises nothing and simply searches
    // somewhere else. The asymmetric pair is what makes the swap visible.
    @Test
    fun latitudeTravelsAsLatitudeAndLongitudeAsLongitude() {
        val sent = params(SearchCriteria())

        assertEquals(-17.3835, sent.latitude, 0.0)
        assertEquals(-66.1568, sent.longitude, 0.0)
    }

    @Test
    fun noServiceTypeChosenMeansNoTypeFilterAtAllRatherThanAnEmptyOne() {
        assertNull(params(SearchCriteria(serviceTypeId = null)).serviceTypeId)
    }

    @Test
    fun theRadiusTravelsInKilometres() {
        assertEquals(10.0, params(SearchCriteria(radius = SearchRadius.KM_10)).radiusKm, 0.0)
    }
}
