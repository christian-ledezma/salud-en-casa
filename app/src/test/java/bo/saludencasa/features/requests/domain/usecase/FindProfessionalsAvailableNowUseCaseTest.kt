package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.catalog.venousLine
import bo.saludencasa.features.location.coordinate
import bo.saludencasa.features.search.FakeProfessionalSearchRepository
import bo.saludencasa.features.search.domain.model.SearchRadius
import bo.saludencasa.features.search.domain.usecase.SearchNearbyProfessionalsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindProfessionalsAvailableNowUseCaseTest {
    private val search = FakeProfessionalSearchRepository()
    private val find = FindProfessionalsAvailableNowUseCase(SearchNearbyProfessionalsUseCase(search))

    // docs/decisions.md, 2026-10-23: the map of «Atención ahora» shows who could
    // come now. Dropping the availability filter would fill it with people who
    // are off, and a narrower radius would hide some of those the request
    // reaches, since the coverage of each professional decides that.
    @Test
    fun asksOnlyForThoseAvailableNowWithinTheWidestRadiusAroundTheGivenPoint() =
        runTest {
            val around = coordinate(-17.3835, -66.1568)

            find(around, venousLine.id)

            val asked = search.requests.single()
            assertEquals(around, asked.origin)
            assertTrue(asked.criteria.availableNowOnly)
            assertEquals(SearchRadius.entries.maxBy { it.km }, asked.criteria.radius)
            assertEquals(venousLine.id, asked.criteria.serviceTypeId)
            assertEquals(0, asked.offset)
        }
}
