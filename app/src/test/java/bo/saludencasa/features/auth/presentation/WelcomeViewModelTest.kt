@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.auth.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.auth.FakeAuthRepository
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthResult
import bo.saludencasa.features.auth.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WelcomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // RF-01: dismissing the account chooser is a decision, not a failure. The
    // screen has to come back untouched, ready for another attempt.
    @Test
    fun `dismissing the account chooser leaves no error on screen`() =
        runTest {
            val viewModel = viewModel()

            viewModel.signIn { GoogleCredentialResult.Failure(AuthError.Cancelled) }
            advanceUntilIdle()

            assertEquals(WelcomeUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `reports a device with no google account as its own error`() =
        runTest {
            val viewModel = viewModel()

            viewModel.signIn { GoogleCredentialResult.Failure(AuthError.NoGoogleAccount) }
            advanceUntilIdle()

            assertEquals(WelcomeUiState.Error(AuthError.NoGoogleAccount), viewModel.uiState.value)
        }

    @Test
    fun `shows progress while the account chooser is still open`() =
        runTest {
            val viewModel = viewModel()

            viewModel.signIn { GoogleCredentialResult.Failure(AuthError.Cancelled) }

            assertEquals(WelcomeUiState.SigningIn, viewModel.uiState.value)
        }

    // A credential the provider did hand over is worthless if the exchange with
    // Supabase fails, so the screen must not advance on the first half alone.
    @Test
    fun `stays on the screen when the token is rejected after a valid credential`() =
        runTest {
            val viewModel = viewModel(AuthResult.Failure(AuthError.TokenRejected))

            viewModel.signIn { GoogleCredentialResult.Success(idToken = "token", rawNonce = "nonce") }
            advanceUntilIdle()

            assertEquals(WelcomeUiState.Error(AuthError.TokenRejected), viewModel.uiState.value)
        }

    @Test
    fun `advances only once the session exists`() =
        runTest {
            val viewModel = viewModel()

            viewModel.signIn { GoogleCredentialResult.Success(idToken = "token", rawNonce = "nonce") }
            advanceUntilIdle()

            assertEquals(WelcomeUiState.SignedIn, viewModel.uiState.value)
        }
}

private fun viewModel(signInResult: AuthResult? = null): WelcomeViewModel {
    val repository =
        signInResult
            ?.let { FakeAuthRepository(signInResult = it) }
            ?: FakeAuthRepository()
    return WelcomeViewModel(SignInWithGoogleUseCase(repository))
}
