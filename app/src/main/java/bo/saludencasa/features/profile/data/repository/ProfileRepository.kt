package bo.saludencasa.features.profile.data.repository

import bo.saludencasa.features.profile.data.datasource.SupabaseProfileDataSource
import bo.saludencasa.features.profile.data.mapper.toPatientUpdateDto
import bo.saludencasa.features.profile.data.mapper.toProfileError
import bo.saludencasa.features.profile.data.mapper.toProfileUpdateDto
import bo.saludencasa.features.profile.data.mapper.toRoleResult
import bo.saludencasa.features.profile.data.mapper.toUserProfile
import bo.saludencasa.features.profile.data.mapper.toUserRole
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.repository.IProfileRepository
import kotlinx.coroutines.CancellationException

class ProfileRepository(
    private val dataSource: SupabaseProfileDataSource,
) : IProfileRepository {
    override suspend fun getRole(): RoleResult {
        val userId = dataSource.currentUserId() ?: return RoleResult.Failure(ProfileError.NotSignedIn)

        return try {
            dataSource.findRole(userId)?.toRoleResult() ?: RoleResult.Failure(ProfileError.ProfileNotFound)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            RoleResult.Failure(failure.toProfileError())
        }
    }

    override suspend fun assignRole(role: AssignableRole): ChooseRoleResult =
        try {
            dataSource
                .assignRole(role.role.name)
                .toUserRole()
                ?.let(ChooseRoleResult::Success)
                ?: ChooseRoleResult.Failure(ProfileError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ChooseRoleResult.Failure(failure.toProfileError())
        }

    override suspend fun getProfile(): ProfileResult {
        val userId = dataSource.currentUserId() ?: return ProfileResult.Failure(ProfileError.NotSignedIn)

        return try {
            val profile = dataSource.findProfile(userId) ?: return ProfileResult.Failure(ProfileError.ProfileNotFound)
            val patient = if (profile.role == UserRole.PATIENT.name) dataSource.findPatient(userId) else null
            ProfileResult.Success(profile.toUserProfile(patient))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ProfileResult.Failure(failure.toProfileError())
        }
    }

    override suspend fun saveProfile(update: ProfileUpdate): ProfileResult {
        val userId = dataSource.currentUserId() ?: return ProfileResult.Failure(ProfileError.NotSignedIn)

        return try {
            dataSource.updateProfile(userId, update.toProfileUpdateDto())
            update.patient?.let { dataSource.updatePatient(userId, it.toPatientUpdateDto()) }
            getProfile()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ProfileResult.Failure(failure.toProfileError())
        }
    }
}
