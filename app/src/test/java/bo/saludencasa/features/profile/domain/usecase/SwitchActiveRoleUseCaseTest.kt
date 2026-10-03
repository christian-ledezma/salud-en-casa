package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.loadedRoles
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SwitchActiveRoleUseCaseTest {
    @Test
    fun `switches to the other role when the person holds both`() =
        runTest {
            val repository =
                FakeProfileRepository(
                    roleResult = loadedRoles(setOf(UserRole.PATIENT, UserRole.PROFESSIONAL), UserRole.PATIENT),
                )

            val result = SwitchActiveRoleUseCase(repository)(UserRole.PROFESSIONAL)

            assertEquals(SwitchRoleResult.Success(UserRole.PROFESSIONAL), result)
            assertEquals(UserRole.PROFESSIONAL, repository.lastActiveRole)
        }

    // The composite foreign key refuses the write too, but it arrives as a
    // constraint violation the client can only report as unexpected. Catching it
    // here is what lets the screen say which role is missing.
    @Test
    fun rejectsSwitchingToARoleThePersonDoesNotHold() =
        runTest {
            val repository = FakeProfileRepository(roleResult = loadedRoles())

            val result = SwitchActiveRoleUseCase(repository)(UserRole.PROFESSIONAL)

            assertEquals(SwitchRoleResult.Failure(ProfileError.RoleNotHeld), result)
            assertEquals(0, repository.switchAttempts)
        }

    // ADMIN is held by an administrator, so possession alone would let the
    // switch activate it. It is refused one level up, in ProfileRoles.switchable,
    // which is what the screen builds its options from.
    @Test
    fun `does not write anything when the held roles cannot be read`() =
        runTest {
            val repository =
                FakeProfileRepository(roleResult = RoleResult.Failure(ProfileError.NetworkUnavailable))

            val result = SwitchActiveRoleUseCase(repository)(UserRole.PROFESSIONAL)

            assertEquals(SwitchRoleResult.Failure(ProfileError.NetworkUnavailable), result)
            assertEquals(0, repository.switchAttempts)
        }
}
