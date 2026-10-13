@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.InboxPageResult
import bo.saludencasa.features.requests.domain.model.InboxResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.inboxRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetInboxUseCaseTest {
    private val repository = FakeRequestRepository()
    private val useCase = GetInboxUseCase(repository)

    // A full page cannot be the end: there may be another behind it, so the
    // screen must keep the "load more" affordance.
    @Test
    fun aFullPageDoesNotEndTheList() =
        runTest {
            repository.inboxResult =
                InboxResult.Loaded(List(GetInboxUseCase.PAGE_SIZE) { inboxRequest(id = "r$it") })

            val result = useCase(offset = 0) as InboxPageResult.Loaded

            assertEquals(GetInboxUseCase.PAGE_SIZE, result.requests.size)
            assertFalse(result.endReached)
        }

    // A short page is the last one, and an empty short page is still the end,
    // not an error.
    @Test
    fun aShortPageEndsTheList() =
        runTest {
            repository.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "only")))

            val result = useCase(offset = 0) as InboxPageResult.Loaded

            assertTrue(result.endReached)
        }

    @Test
    fun aFailureIsCarriedThrough() =
        runTest {
            repository.inboxResult = InboxResult.Failure(RequestError.NetworkUnavailable)

            val result = useCase(offset = 0)

            assertEquals(InboxPageResult.Failure(RequestError.NetworkUnavailable), result)
        }

    @Test
    fun theOffsetReachesTheRepository() =
        runTest {
            useCase(offset = 40)

            assertEquals(40 to GetInboxUseCase.PAGE_SIZE, repository.inboxPageRequests.single())
        }
}
