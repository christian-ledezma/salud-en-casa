package bo.saludencasa.features.auth.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.auth.FakeAuthRepository
import bo.saludencasa.features.auth.authSession
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import bo.saludencasa.features.auth.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a sign out that fails is shown and does not pass for a closed session`() =
        runTest {
            val repository = signedInRepository(SignOutResult.Failure(AuthError.Unexpected))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(AccountUiState.Loading, awaitItem())
                assertEquals(AccountUiState.Content(authSession()), awaitItem())

                viewModel.signOut()

                assertEquals(AccountUiState.Error(AuthError.Unexpected), awaitItem())
            }
        }

    // Fixes the contract the «Reintentar» button of the failure state leans on:
    // asking again after a failure has to reach the repository a second time and
    // clear the failure only when it succeeds. This does not catch the wiring of
    // the button itself, which is what the review of pull request #2 found
    // broken; only a Compose test over AccountContent would. Noted as debt in
    // plan.md, HU-01.
    @Test
    fun `retrying after a failed sign out runs the sign out again`() =
        runTest {
            val repository = signedInRepository(SignOutResult.Failure(AuthError.Unexpected))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(AccountUiState.Loading, awaitItem())
                assertEquals(AccountUiState.Content(authSession()), awaitItem())

                viewModel.signOut()
                assertEquals(AccountUiState.Error(AuthError.Unexpected), awaitItem())

                repository.signOutResult = SignOutResult.Success
                viewModel.signOut()
                assertEquals(AccountUiState.Content(authSession()), awaitItem())
            }

            assertEquals(2, repository.signOutAttempts)
        }

    // The session keeps emitting while the failure is on screen. If the session
    // took priority in the combine, the failure would vanish on the next
    // emission and the person would never learn the sign out did not happen.
    @Test
    fun `a later session emission does not hide a pending failure`() =
        runTest {
            val sessions = MutableStateFlow<SessionState>(SessionState.SignedIn(authSession()))
            val repository =
                FakeAuthRepository(
                    sessions = sessions,
                    signOutResult = SignOutResult.Failure(AuthError.Unexpected),
                )
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(AccountUiState.Loading, awaitItem())
                assertEquals(AccountUiState.Content(authSession()), awaitItem())

                viewModel.signOut()
                assertEquals(AccountUiState.Error(AuthError.Unexpected), awaitItem())

                sessions.value = SessionState.SignedIn(authSession(fullName = "Ana Maria Quispe"))

                expectNoEvents()
            }
        }
}

private fun signedInRepository(signOutResult: SignOutResult): FakeAuthRepository =
    FakeAuthRepository(
        sessions = MutableStateFlow(SessionState.SignedIn(authSession())),
        signOutResult = signOutResult,
    )

private fun viewModel(repository: FakeAuthRepository): AccountViewModel =
    AccountViewModel(
        observeSession = ObserveSessionUseCase(repository),
        signOut = SignOutUseCase(repository),
    )
