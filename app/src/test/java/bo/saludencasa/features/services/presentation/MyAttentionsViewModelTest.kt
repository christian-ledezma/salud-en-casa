package bo.saludencasa.features.services.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.core.util.ListRefresh
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attentionSummary
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.GetMyAttentionsUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class MyAttentionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeServiceRepository()

    private fun TestScope.viewModel(side: AttentionSide = AttentionSide.PROFESSIONAL): MyAttentionsViewModel {
        val viewModel = MyAttentionsViewModel(side, GetMyAttentionsUseCase(repository))
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

    // Someone with both roles reads one side at a time (INV-17), on every read.
    @Test
    fun everyReadAsksForTheSideOfTheList() =
        runTest {
            repository.attentions = stored(GetMyAttentionsUseCase.PAGE_SIZE + 1)
            val viewModel = viewModel(AttentionSide.PATIENT)

            viewModel.onLoadMore()
            advanceUntilIdle()
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onPullToRefresh()
            advanceUntilIdle()

            assertEquals(setOf(AttentionSide.PATIENT), repository.sidesRead.toSet())
        }

    // HT-27, what the author asked for: pulling rereads every datum of the card, not
    // only its status.
    @Test
    fun pullingRereadsEveryDatumOfTheCard() =
        runTest {
            repository.attentions = listOf(attentionSummary(requestId = "a"))
            val viewModel = viewModel()

            val start = Instant.parse("2026-11-04T14:02:00Z")
            val changed =
                attentionSummary(requestId = "a", status = ServiceStatus.IN_PROGRESS, startedAt = start)
                    .copy(serviceTypeName = "Otro servicio", counterpartName = "Otra persona")
            repository.attentions = listOf(changed)
            viewModel.onPullToRefresh()
            advanceUntilIdle()

            assertEquals(listOf(changed), viewModel.content().attentions)
            assertEquals(ListRefresh(), viewModel.listRefresh.value)
        }

    @Test
    fun aFailedPullKeepsTheListAndSaysSo() =
        runTest {
            repository.attentions = stored(2)
            val viewModel = viewModel()

            repository.attentionsError = ServiceError.NetworkUnavailable
            viewModel.onPullToRefresh()
            advanceUntilIdle()

            assertEquals(2, viewModel.content().attentions.size)
            assertEquals(ListRefresh(failed = true), viewModel.listRefresh.value)
        }

    // Nobody asked for the read on returning to the screen, so its failure says
    // nothing, and its success retires a notice that is no longer true.
    @Test
    fun theReadOnReturningIsSilentAndClearsAnOldFailure() =
        runTest {
            repository.attentions = stored(2)
            val viewModel = viewModel()

            repository.attentionsError = ServiceError.NetworkUnavailable
            viewModel.refresh()
            advanceUntilIdle()
            assertFalse(viewModel.listRefresh.value.failed)

            viewModel.onPullToRefresh()
            advanceUntilIdle()
            assertTrue(viewModel.listRefresh.value.failed)

            repository.attentionsError = null
            viewModel.refresh()
            advanceUntilIdle()
            assertFalse(viewModel.listRefresh.value.failed)
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
