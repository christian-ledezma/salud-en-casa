package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attentionSummary
import bo.saludencasa.features.services.domain.model.MyAttentionsPageResult
import bo.saludencasa.features.services.domain.model.ServiceError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMyAttentionsUseCaseTest {
    private val repository = FakeServiceRepository()
    private val getMyAttentions = GetMyAttentionsUseCase(repository)

    private fun stored(count: Int) = List(count) { attentionSummary(requestId = "r$it") }

    // RNF-08: a full page may have more behind it, so the list has to offer them.
    @Test
    fun aFullPageIsNotTheEnd() =
        runTest {
            repository.attentions = stored(GetMyAttentionsUseCase.PAGE_SIZE + 1)

            val page = getMyAttentions(offset = 0) as MyAttentionsPageResult.Loaded

            assertEquals(GetMyAttentionsUseCase.PAGE_SIZE, page.attentions.size)
            assertFalse(page.endReached)
        }

    @Test
    fun aShortPageIsTheEnd() =
        runTest {
            repository.attentions = stored(GetMyAttentionsUseCase.PAGE_SIZE + 1)

            val page = getMyAttentions(offset = GetMyAttentionsUseCase.PAGE_SIZE) as MyAttentionsPageResult.Loaded

            assertEquals(1, page.attentions.size)
            assertTrue(page.endReached)
        }

    @Test
    fun aFailureCarriesItsError() =
        runTest {
            repository.attentionsError = ServiceError.NetworkUnavailable

            assertEquals(
                MyAttentionsPageResult.Failure(ServiceError.NetworkUnavailable),
                getMyAttentions(offset = 0),
            )
        }
}
