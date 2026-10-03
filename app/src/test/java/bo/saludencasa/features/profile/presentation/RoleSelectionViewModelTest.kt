package bo.saludencasa.features.profile.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.AddRoleUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RoleSelectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // RF-01.4 asks for a deliberate choice. Confirming with nothing selected
    // would have to pick a default, and a default role is one the person never
    // chose, and nothing on the screen says which one.
    @Test
    fun `confirming without a choice writes nothing`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = RoleSelectionViewModel(AddRoleUseCase(repository))

            viewModel.confirm()

            assertEquals(0, repository.addAttempts)
            assertEquals(RoleSelectionUiState.Choosing(selected = null), viewModel.uiState.value)
        }

    @Test
    fun `the chosen role reaches the repository and the screen learns it was assigned`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = RoleSelectionViewModel(AddRoleUseCase(repository))

            viewModel.uiState.test {
                assertEquals(RoleSelectionUiState.Choosing(selected = null), awaitItem())

                viewModel.select(AssignableRole.PATIENT)
                assertEquals(RoleSelectionUiState.Choosing(AssignableRole.PATIENT), awaitItem())

                viewModel.confirm()
                assertEquals(RoleSelectionUiState.Saving(AssignableRole.PATIENT), awaitItem())
                assertEquals(RoleSelectionUiState.Assigned(UserRole.PATIENT), awaitItem())
            }

            assertEquals(AssignableRole.PATIENT, repository.lastAddedRole)
        }

    // A failed choice has to keep the selection. Dropping it would send the
    // person back to an empty screen and make «Reintentar» impossible to honour.
    @Test
    fun `a failed choice keeps the selection so retrying writes the same role`() =
        runTest {
            val repository =
                FakeProfileRepository(
                    addResult = AddRoleResult.Failure(ProfileError.NetworkUnavailable),
                )
            val viewModel = RoleSelectionViewModel(AddRoleUseCase(repository))

            viewModel.uiState.test {
                assertEquals(RoleSelectionUiState.Choosing(selected = null), awaitItem())

                viewModel.select(AssignableRole.PROFESSIONAL)
                awaitItem()

                viewModel.confirm()
                awaitItem()
                assertEquals(
                    RoleSelectionUiState.Error(AssignableRole.PROFESSIONAL, ProfileError.NetworkUnavailable),
                    awaitItem(),
                )

                repository.addResult = null
                viewModel.confirm()
                assertEquals(RoleSelectionUiState.Saving(AssignableRole.PROFESSIONAL), awaitItem())
                assertEquals(RoleSelectionUiState.Assigned(UserRole.PROFESSIONAL), awaitItem())
            }

            assertEquals(2, repository.addAttempts)
        }
}
