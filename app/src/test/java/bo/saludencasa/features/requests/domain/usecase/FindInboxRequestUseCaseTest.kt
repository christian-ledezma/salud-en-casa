package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.InboxResult
import bo.saludencasa.features.requests.domain.model.InboxSearchResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.inboxRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindInboxRequestUseCaseTest {
    private val repository = FakeRequestRepository()
    private val find = FindInboxRequestUseCase(GetInboxUseCase(repository))

    private fun fullPage(offset: Int) =
        InboxResult.Loaded(List(GetInboxUseCase.PAGE_SIZE) { inboxRequest(id = "r${offset + it}") })

    @Test
    fun aRequestAlreadyLoadedIsFoundWithoutReading() =
        runTest {
            var reads = 0
            repository.inboxResponder = { offset, _ ->
                reads++
                fullPage(offset)
            }

            val result = find("b", listOf(inboxRequest(id = "a"), inboxRequest(id = "b")), endReached = false)

            assertTrue(result is InboxSearchResult.Found)
            assertEquals(0, reads)
        }

    @Test
    fun keepsReadingPagesUntilItFindsTheRequest() =
        runTest {
            repository.inboxResponder = { offset, _ -> fullPage(offset) }

            val result = find("r45", List(GetInboxUseCase.PAGE_SIZE) { inboxRequest(id = "r$it") }, endReached = false)

            assertTrue(result is InboxSearchResult.Found)
            assertEquals(3 * GetInboxUseCase.PAGE_SIZE, result.requests.size)
        }

    // RNF-08: an inbox that never ends is not read to its end.
    @Test
    fun stopsAtTheSearchLimit() =
        runTest {
            repository.inboxResponder = { offset, _ -> fullPage(offset) }

            val result = find("never", emptyList(), endReached = false)

            assertTrue(result is InboxSearchResult.NotInInbox)
            assertEquals(5 * GetInboxUseCase.PAGE_SIZE, result.requests.size)
        }

    @Test
    fun aFailedPageIsNotARequestThatLeft() =
        runTest {
            repository.inboxResult = InboxResult.Failure(RequestError.NetworkUnavailable)

            val result = find("b", listOf(inboxRequest(id = "a")), endReached = false)

            assertEquals(RequestError.NetworkUnavailable, (result as InboxSearchResult.Failure).error)
        }
}
