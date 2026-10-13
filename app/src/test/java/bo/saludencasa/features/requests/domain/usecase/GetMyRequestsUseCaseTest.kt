package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.MyRequestsPageResult
import bo.saludencasa.features.requests.domain.model.MyRequestsResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.serviceRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMyRequestsUseCaseTest {
    private val repository = FakeRequestRepository()
    private val useCase = GetMyRequestsUseCase(repository)

    // A full page means there may be more: endReached must stay false, or the
    // list never asks for the next page.
    @Test
    fun aFullPageIsNotTheEnd() =
        runTest {
            repository.requestsResult =
                MyRequestsResult.Loaded(List(GetMyRequestsUseCase.PAGE_SIZE) { serviceRequest(id = "r$it") })

            val result = useCase(offset = 0)

            assertTrue(result is MyRequestsPageResult.Loaded)
            assertFalse((result as MyRequestsPageResult.Loaded).endReached)
        }

    @Test
    fun aShortPageIsTheEnd() =
        runTest {
            repository.requestsResult = MyRequestsResult.Loaded(listOf(serviceRequest()))

            val result = useCase(offset = 0)

            assertTrue((result as MyRequestsPageResult.Loaded).endReached)
        }

    @Test
    fun forwardsTheFailure() =
        runTest {
            repository.requestsResult = MyRequestsResult.Failure(RequestError.NetworkUnavailable)

            val result = useCase(offset = 0)

            assertEquals(MyRequestsPageResult.Failure(RequestError.NetworkUnavailable), result)
        }
}
