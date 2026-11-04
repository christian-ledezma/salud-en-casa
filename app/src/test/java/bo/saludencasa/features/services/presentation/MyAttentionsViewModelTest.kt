package bo.saludencasa.features.services.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attentionSummary
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.GetMyAttentionsUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyAttentionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeServiceRepository()

    private fun TestScope.viewModel(): MyAttentionsViewModel {
        val viewModel = MyAttentionsViewModel(GetMyAttentionsUseCase(repository))
        advanceUntilIdle()
        return viewModel
    }

    private fun MyAttentionsViewModel.content() = uiState.value as MyAttentionsUiState.Content

    private fun stored(count: Int) = List(count) { attentionSummary(requestId = "r$it") }

    @Test
    fun withNoAttentionsTheListIsEmpty() =
        runTest {
            assertEquals(MyAttentionsUiState.Empty, viewModel().uiState.value)
        }

    // docs/decisions.md, 2026-11-03, HT-26: a finished attention still has a
    // conversation to reach, so nothing is filtered out by its status.
    @Test
    fun cancelledAndCompletedAttentionsStayInTheList() =
        runTest {
            repository.attentions =
                listOf(
                    attentionSummary(requestId = "a", status = ServiceStatus.COMPLETED),
                    attentionSummary(requestId = "b", status = ServiceStatus.CANCELLED),
                )

            assertEquals(listOf("a", "b"), viewModel().content().attentions.map { it.requestId })
        }

    // Offsets move when a new attention arrives, which brings a row back; a
    // repeated key would throw the LazyColumn.
    @Test
    fun aNewAttentionWhilePagingDoesNotRepeatARow() =
        runTest {
            repository.attentions = stored(GetMyAttentionsUseCase.PAGE_SIZE + 1)
            val viewModel = viewModel()

            repository.attentions = listOf(attentionSummary(requestId = "new")) + repository.attentions
            viewModel.onLoadMore()
            advanceUntilIdle()

            val ids = viewModel.content().attentions.map { it.requestId }
            assertEquals(ids.distinct(), ids)
            assertTrue(viewModel.content().endReached)
        }

    // Coming back from an attention must not lose the list to a dropped read.
    @Test
    fun aFailedRefreshKeepsTheListOnScreen() =
        runTest {
            repository.attentions = stored(2)
            val viewModel = viewModel()

            repository.attentionsError = ServiceError.NetworkUnavailable
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(2, viewModel.content().attentions.size)
        }

    @Test
    fun aRefreshShowsAnAttentionAgreedMeanwhile() =
        runTest {
            val viewModel = viewModel()

            repository.attentions = stored(1)
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(1, viewModel.content().attentions.size)
        }

    @Test
    fun aFailedNextPageKeepsTheListAndOffersItAgain() =
        runTest {
            repository.attentions = stored(GetMyAttentionsUseCase.PAGE_SIZE + 1)
            val viewModel = viewModel()

            repository.attentionsError = ServiceError.NetworkUnavailable
            viewModel.onLoadMore()
            advanceUntilIdle()

            assertEquals(ServiceError.NetworkUnavailable, viewModel.content().notice)
            assertEquals(GetMyAttentionsUseCase.PAGE_SIZE, viewModel.content().attentions.size)
        }

    @Test
    fun aFirstReadThatFailsCanBeRetried() =
        runTest {
            repository.attentionsError = ServiceError.NetworkUnavailable
            val viewModel = viewModel()
            assertEquals(MyAttentionsUiState.Failed(ServiceError.NetworkUnavailable), viewModel.uiState.value)

            repository.attentionsError = null
            repository.attentions = stored(1)
            viewModel.load()
            advanceUntilIdle()

            assertEquals(1, viewModel.content().attentions.size)
        }
}
