@file:OptIn(ExperimentalCoroutinesApi::class)

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
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.AddRoleUseCase
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
import bo.saludencasa.features.profile.domain.usecase.SwitchActiveRoleUseCase
import bo.saludencasa.features.profile.loadedRoles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
            advanceUntilIdle()

            viewModel.uiState.test {
                assertEquals(AccountUiState.Loading, awaitItem())
                assertTrue(awaitItem() is AccountUiState.Content)

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
            advanceUntilIdle()

            viewModel.uiState.test {
                assertEquals(AccountUiState.Loading, awaitItem())
                assertTrue(awaitItem() is AccountUiState.Content)

                viewModel.signOut()
                assertEquals(AccountUiState.Error(AuthError.Unexpected), awaitItem())

                repository.signOutResult = SignOutResult.Success
                viewModel.signOut()
                assertTrue(awaitItem() is AccountUiState.Content)
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
            advanceUntilIdle()

            viewModel.uiState.test {
                assertEquals(AccountUiState.Loading, awaitItem())
                assertTrue(awaitItem() is AccountUiState.Content)

                viewModel.signOut()
                assertEquals(AccountUiState.Error(AuthError.Unexpected), awaitItem())

                sessions.value = SessionState.SignedIn(authSession(fullName = "Ana Maria Quispe"))

                expectNoEvents()
            }
        }

    @Test
    fun `the screen opens on the role the person left active`() =
        runTest {
            val profiles = FakeProfileRepository(roleResult = loadedRoles(bothRoles, UserRole.PROFESSIONAL))
            val viewModel = viewModel(signedInRepository(SignOutResult.Success), profiles)
            advanceUntilIdle()

            viewModel.uiState.test {
                advanceUntilIdle()
                val content = expectMostRecentItem() as AccountUiState.Content

                assertEquals(UserRole.PROFESSIONAL, content.roleSection.roles.active)
                assertTrue(content.roleSection.roles.canSwitch)
            }
        }

    @Test
    fun `switching to the other role leaves it active`() =
        runTest {
            val profiles = FakeProfileRepository(roleResult = loadedRoles(bothRoles, UserRole.PATIENT))
            val viewModel = viewModel(signedInRepository(SignOutResult.Success), profiles)
            advanceUntilIdle()

            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()

                viewModel.switchTo(UserRole.PROFESSIONAL)
                advanceUntilIdle()

                val content = expectMostRecentItem() as AccountUiState.Content
                assertEquals(UserRole.PROFESSIONAL, content.roleSection.roles.active)
                assertNull(content.roleSection.error)
            }

            assertEquals(UserRole.PROFESSIONAL, profiles.lastActiveRole)
        }

    // The optimistic update AddressListViewModelTest already caught once on
    // another screen: a control that moves to the role the finger asked for and
    // stays there although the server refused leaves the person believing they
    // are working as a professional while every policy still treats them as a
    // patient.
    @Test
    fun switchFailureKeepsThePreviousActiveRole() =
        runTest {
            val profiles =
                FakeProfileRepository(
                    roleResult = loadedRoles(bothRoles, UserRole.PATIENT),
                    switchResult = SwitchRoleResult.Failure(ProfileError.NetworkUnavailable),
                )
            val viewModel = viewModel(signedInRepository(SignOutResult.Success), profiles)
            advanceUntilIdle()

            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()

                viewModel.switchTo(UserRole.PROFESSIONAL)
                advanceUntilIdle()

                val content = expectMostRecentItem() as AccountUiState.Content
                assertEquals(UserRole.PATIENT, content.roleSection.roles.active)
                assertEquals(ProfileError.NetworkUnavailable, content.roleSection.error)
                assertFalse(content.roleSection.busy)
            }
        }

    // A control that offers nowhere to go tells the person a capability exists
    // and then does nothing when they use it.
    @Test
    fun theSwitchIsHiddenForSomeoneWithASingleRole() =
        runTest {
            val profiles = FakeProfileRepository(roleResult = loadedRoles())
            val viewModel = viewModel(signedInRepository(SignOutResult.Success), profiles)
            advanceUntilIdle()

            viewModel.uiState.test {
                advanceUntilIdle()
                val content = expectMostRecentItem() as AccountUiState.Content

                assertFalse(content.roleSection.roles.canSwitch)
                assertEquals(listOf(AssignableRole.PROFESSIONAL), content.roleSection.roles.addable)
            }
        }

    @Test
    fun `activating the other role reaches the repository and the screen reads it back`() =
        runTest {
            val profiles = FakeProfileRepository(roleResult = loadedRoles())
            val viewModel = viewModel(signedInRepository(SignOutResult.Success), profiles)
            advanceUntilIdle()

            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()

                viewModel.activate(AssignableRole.PROFESSIONAL)
                advanceUntilIdle()

                val content = expectMostRecentItem() as AccountUiState.Content
                assertEquals(UserRole.PROFESSIONAL, content.roleSection.roles.active)
                assertTrue(content.roleSection.roles.canSwitch)
            }

            assertEquals(AssignableRole.PROFESSIONAL, profiles.lastAddedRole)
        }

    @Test
    fun `a refused activation reports the error and leaves the roles untouched`() =
        runTest {
            val profiles =
                FakeProfileRepository(
                    roleResult = loadedRoles(),
                    addResult = AddRoleResult.Failure(ProfileError.NetworkUnavailable),
                )
            val viewModel = viewModel(signedInRepository(SignOutResult.Success), profiles)
            advanceUntilIdle()

            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()

                viewModel.activate(AssignableRole.PROFESSIONAL)
                advanceUntilIdle()

                val content = expectMostRecentItem() as AccountUiState.Content
                assertEquals(setOf(UserRole.PATIENT), content.roleSection.roles.held)
                assertEquals(ProfileError.NetworkUnavailable, content.roleSection.error)
            }
        }

    // Switching to the role already active would spend a request to end up where
    // the person already was.
    @Test
    fun `switching to the role already active writes nothing`() =
        runTest {
            val profiles = FakeProfileRepository(roleResult = loadedRoles(bothRoles, UserRole.PATIENT))
            val viewModel = viewModel(signedInRepository(SignOutResult.Success), profiles)
            advanceUntilIdle()

            viewModel.uiState.test {
                advanceUntilIdle()
                expectMostRecentItem()

                viewModel.switchTo(UserRole.PATIENT)
                advanceUntilIdle()
            }

            assertEquals(0, profiles.switchAttempts)
        }
}

private val bothRoles = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL)

private fun signedInRepository(signOutResult: SignOutResult): FakeAuthRepository =
    FakeAuthRepository(
        sessions = MutableStateFlow(SessionState.SignedIn(authSession())),
        signOutResult = signOutResult,
    )

private fun viewModel(
    repository: FakeAuthRepository,
    profiles: FakeProfileRepository = FakeProfileRepository(roleResult = loadedRoles()),
): AccountViewModel =
    AccountViewModel(
        observeSession = ObserveSessionUseCase(repository),
        signOut = SignOutUseCase(repository),
        getRoles = GetRolesUseCase(profiles),
        switchActiveRole = SwitchActiveRoleUseCase(profiles),
        addRole = AddRoleUseCase(profiles),
    )
