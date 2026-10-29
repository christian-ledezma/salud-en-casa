@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.services.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AttentionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeServiceRepository()

    private fun viewModel() = AttentionViewModel("r1", GetAttentionUseCase(repository))

    @Test
    fun showsTheSealedAgreementWithTheCounterpart() =
        runTest {
            repository.result = AttentionResult.Loaded(attention(counterpartId = "pro-1"))

            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value as AttentionUiState.Content
            assertEquals("pro-1", state.attention.counterpart.id)
            assertEquals("María Quispe", state.attention.counterpart.name)
            assertEquals(true, state.attention.iAmThePatient)
        }

    @Test
    fun showsNotFoundWhenThereIsNoServiceForTheCaller() =
        runTest {
            repository.result = AttentionResult.NotFound

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(AttentionUiState.NotFound, viewModel.uiState.value)
        }

    @Test
    fun reportsAFailure() =
        runTest {
            repository.result = AttentionResult.Failure(ServiceError.NetworkUnavailable)

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(AttentionUiState.Failed(ServiceError.NetworkUnavailable), viewModel.uiState.value)
        }
}
