package bo.saludencasa.features.auth.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.auth.FakeAuthRepository
import bo.saludencasa.features.auth.authSession
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetRoleUseCase
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
            val viewModel = viewModel(sessions, FakeProfileRepository())

            viewModel.destination.test {
                assertEquals(StartupDestination.Loading, awaitItem())

                sessions.value = SessionState.SignedIn(authSession())

                assertEquals(StartupDestination.ChooseRole, awaitItem())
            }
        }

    // RF-01.4: nothing else is reachable while the role is missing. Routing a
    // signed in person to the home because the session exists would skip the
    // question and leave patients and professionals without their record.
    @Test
    fun `a signed in person without a role is sent to choose one`() =
        runTest {
            val viewModel = viewModel(signedIn(), FakeProfileRepository(roleResult = RoleResult.Unassigned))

            viewModel.destination.test {
                assertEquals(StartupDestination.Loading, awaitItem())
                assertEquals(StartupDestination.ChooseRole, awaitItem())
            }
        }

    // RF-01.4, the other half: somebody who already answered never sees the
    // question again, on this launch or any later one.
    @Test
    fun `a signed in person with a role goes straight to the home of that role`() =
        runTest {
            val repository = FakeProfileRepository(roleResult = RoleResult.Assigned(UserRole.PROFESSIONAL))
            val viewModel = viewModel(signedIn(), repository)

            viewModel.destination.test {
                assertEquals(StartupDestination.Loading, awaitItem())
                assertEquals(StartupDestination.Home, awaitItem())
            }
        }

    @Test
    fun `a signed out person is sent to sign in without the role ever being read`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(MutableStateFlow(SessionState.SignedOut), repository)

            viewModel.destination.test {
                assertEquals(StartupDestination.Loading, awaitItem())
                assertEquals(StartupDestination.SignIn, awaitItem())
            }

            assertEquals(0, repository.roleReads)
        }

    // A role that cannot be read is not a role that is missing. Treating the
    // failure as «no role yet» would put somebody who already chose in front of
    // the question again, and the database would refuse their answer.
    @Test
    fun `a role that cannot be read stops on an error the person can retry`() =
        runTest {
            val repository = FakeProfileRepository(roleResult = RoleResult.Failure(ProfileError.NetworkUnavailable))
            val viewModel = viewModel(signedIn(), repository)

            viewModel.destination.test {
                assertEquals(StartupDestination.Loading, awaitItem())
                assertEquals(StartupDestination.Error(ProfileError.NetworkUnavailable), awaitItem())

                repository.roleResult = RoleResult.Assigned(UserRole.PATIENT)
                viewModel.retry()

                assertEquals(StartupDestination.Home, awaitItem())
            }

            assertEquals(2, repository.roleReads)
        }
}

private fun signedIn(): MutableStateFlow<SessionState> = MutableStateFlow(SessionState.SignedIn(authSession()))

private fun viewModel(
    sessions: MutableStateFlow<SessionState>,
    profiles: FakeProfileRepository,
): StartupViewModel =
    StartupViewModel(
        observeSession = ObserveSessionUseCase(FakeAuthRepository(sessions = sessions)),
        getRole = GetRoleUseCase(profiles),
    )
