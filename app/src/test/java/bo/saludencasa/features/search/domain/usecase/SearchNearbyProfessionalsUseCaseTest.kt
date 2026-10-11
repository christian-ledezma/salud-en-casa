package bo.saludencasa.features.search.domain.usecase

import bo.saludencasa.features.location.coordinate
import bo.saludencasa.features.search.FakeProfessionalSearchRepository
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsPageResult
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.nearbyProfessional
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchNearbyProfessionalsUseCaseTest {
    private val repository = FakeProfessionalSearchRepository()
    private val useCase = SearchNearbyProfessionalsUseCase(repository)

    private fun page(count: Int) = List(count) { index -> nearbyProfessional(id = "p$index", distanceM = 100 * index) }

    // A full page is not the end: a <= here would stop the pagination at
    // exactly twenty results and hide the twenty first.
    @Test
    fun aFullPageIsNotTheEndOfTheResults() =
        runTest {
            repository.professionals = page(SearchNearbyProfessionalsUseCase.PAGE_SIZE)

            val result = useCase(coordinate(), SearchCriteria(), offset = 0)

            assertFalse((result as NearbyProfessionalsPageResult.Loaded).endReached)
        }

    @Test
    fun aShortPageEndsThePagination() =
        runTest {
            repository.professionals = page(3)

            val result = useCase(coordinate(), SearchCriteria(), offset = 0)

            assertTrue((result as NearbyProfessionalsPageResult.Loaded).endReached)
        }

    @Test
    fun asksTheEngineForOnePageAtTheOffsetItWasGiven() =
        runTest {
            repository.professionals = page(25)

            useCase(coordinate(), SearchCriteria(), offset = 20)

            val request = repository.requests.single()
            assertEquals(20, request.offset)
            assertEquals(SearchNearbyProfessionalsUseCase.PAGE_SIZE, request.limit)
        }
}
