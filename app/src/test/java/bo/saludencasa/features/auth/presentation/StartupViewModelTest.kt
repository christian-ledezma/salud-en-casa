package bo.saludencasa.features.auth.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.auth.FakeAuthRepository
import bo.saludencasa.features.auth.authSession
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // The stored session is restored asynchronously, so a start up screen that
    // reads it once decides before the answer exists and sends a signed in user
    // back to the welcome screen on every launch. It is the defect HT-05 found
    // on a device and the reason the session is observed and not read.
    @Test
    fun `waits for the restored session instead of deciding before it arrives`() =
        runTest {
            val sessions = MutableStateFlow<SessionState>(SessionState.Loading)
            val viewModel = StartupViewModel(ObserveSessionUseCase(FakeAuthRepository(sessions = sessions)))

            viewModel.sessionState.test {
                assertEquals(SessionState.Loading, awaitItem())

                val restored = SessionState.SignedIn(authSession())
                sessions.value = restored

                assertEquals(restored, awaitItem())
            }
        }
}
