package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChooseRoleUseCaseTest {
    @Test
    fun `assigns the chosen role when the profile does not have one yet`() =
        runTest {
            val repository = FakeProfileRepository(roleResult = RoleResult.Unassigned)

            val result = ChooseRoleUseCase(repository)(AssignableRole.PROFESSIONAL)

            assertEquals(ChooseRoleResult.Success(UserRole.PROFESSIONAL), result)
            assertEquals(AssignableRole.PROFESSIONAL, repository.lastAssignedRole)
        }

    // RF-01.4: the role is chosen once. The trigger profiles_guard_role refuses
    // a second one too, but by then the request has already left the device and
    // the person sees a failure instead of the screen they already earned.
    @Test
    fun assignsRoleOnlyOnceAndRejectsSecondAssignment() =
        runTest {
            val repository = FakeProfileRepository(roleResult = RoleResult.Assigned(UserRole.PATIENT))

            val result = ChooseRoleUseCase(repository)(AssignableRole.PROFESSIONAL)

            assertEquals(ChooseRoleResult.Failure(ProfileError.RoleAlreadyAssigned), result)
            assertEquals(0, repository.assignAttempts)
        }

    // Reading the current role can fail on its own. Writing anyway would turn a
    // lost connection into a role the person never confirmed.
    @Test
    fun `does not write anything when the current role cannot be read`() =
        runTest {
            val repository = FakeProfileRepository(roleResult = RoleResult.Failure(ProfileError.NetworkUnavailable))

            val result = ChooseRoleUseCase(repository)(AssignableRole.PATIENT)

            assertEquals(ChooseRoleResult.Failure(ProfileError.NetworkUnavailable), result)
            assertEquals(0, repository.assignAttempts)
        }
}
