package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.loadedRoles
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddRoleUseCaseTest {
    @Test
    fun `adds the role when the person does not hold it yet`() =
        runTest {
            val repository = FakeProfileRepository(roleResult = loadedRoles(held = emptySet(), active = null))

            val result = AddRoleUseCase(repository)(AssignableRole.PROFESSIONAL)

            assertEquals(AddRoleResult.Success(UserRole.PROFESSIONAL), result)
            assertEquals(AssignableRole.PROFESSIONAL, repository.lastAddedRole)
        }

    // The whole point of the Sprint 2.5 change: a patient adding the
    // professional role is the feature, not an error. What stays refused is the
    // same role twice, which the primary key of profile_roles would reject
    // anyway. Replaces assignsRoleOnlyOnceAndRejectsSecondAssignment, whose rule
    // was derogated (docs/decisions.md, 2026-10-01).
    @Test
    fun aPatientCanAddTheProfessionalRole() =
        runTest {
            val repository = FakeProfileRepository(roleResult = loadedRoles())

            val result = AddRoleUseCase(repository)(AssignableRole.PROFESSIONAL)

            assertEquals(AddRoleResult.Success(UserRole.PROFESSIONAL), result)
            assertEquals(1, repository.addAttempts)
        }

    @Test
    fun rejectsAddingARoleThePersonAlreadyHolds() =
        runTest {
            val repository = FakeProfileRepository(roleResult = loadedRoles())

            val result = AddRoleUseCase(repository)(AssignableRole.PATIENT)

            assertEquals(AddRoleResult.Failure(ProfileError.RoleAlreadyHeld), result)
            assertEquals(0, repository.addAttempts)
        }

    // A read that failed says nothing about which roles the person holds.
    // Writing anyway would add a role on the strength of a reply that never
    // arrived.
    @Test
    fun `does not write anything when the held roles cannot be read`() =
        runTest {
            val repository =
                FakeProfileRepository(roleResult = RoleResult.Failure(ProfileError.NetworkUnavailable))

            val result = AddRoleUseCase(repository)(AssignableRole.PATIENT)

            assertEquals(AddRoleResult.Failure(ProfileError.NetworkUnavailable), result)
            assertEquals(0, repository.addAttempts)
        }
}
