package bo.saludencasa.features.notifications.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.domain.usecase.AnswerNotificationPromptUseCase
import bo.saludencasa.features.notifications.domain.usecase.ObserveNotificationPromptAnsweredUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationPermissionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repository: FakeNotificationRepository) =
        NotificationPermissionViewModel(
            ObserveNotificationPromptAnsweredUseCase(repository),
            AnswerNotificationPromptUseCase(repository),
        )

    // Starting as "not answered" would flash the card for a frame on every
    // visit of someone who already declined, before the stored answer arrives.
    @Test
    fun doesNotShowTheCardBeforeTheStoredAnswerIsRead() =
        runTest {
            viewModel(FakeNotificationRepository()).answered.test {
                assertTrue(awaitItem())
                assertEquals(false, awaitItem())
            }
        }

    // RF-09.13: one explanation, then the decision is respected.
    @Test
    fun neverAsksAgainOnceAnswered() =
        runTest {
            val repository = FakeNotificationRepository()
            val viewModel = viewModel(repository)

            viewModel.answered.test {
                assertTrue(awaitItem())
                assertEquals(false, awaitItem())

                viewModel.onAnswered()
                advanceUntilIdle()

                assertTrue(awaitItem())
            }
            assertEquals(listOf("markPromptAnswered"), repository.calls)
        }
}
