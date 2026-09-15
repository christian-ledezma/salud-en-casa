package bo.saludencasa.features.profile.data.repository

import bo.saludencasa.features.profile.data.datasource.SupabaseProfileDataSource
import bo.saludencasa.features.profile.data.mapper.toProfileError
import bo.saludencasa.features.profile.data.mapper.toPublicProfile
import bo.saludencasa.features.profile.data.mapper.toRoleResult
import bo.saludencasa.features.profile.data.mapper.toSaveProfileParams
import bo.saludencasa.features.profile.data.mapper.toUserProfile
import bo.saludencasa.features.profile.data.mapper.toUserRole
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
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
            val professional =
                if (profile.role == UserRole.PROFESSIONAL.name) dataSource.findProfessional(userId) else null
            ProfileResult.Success(profile.toUserProfile(patient, professional))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ProfileResult.Failure(failure.toProfileError())
        }
    }

    override suspend fun saveProfile(update: ProfileUpdate): ProfileResult {
        if (dataSource.currentUserId() == null) return ProfileResult.Failure(ProfileError.NotSignedIn)

        return try {
            dataSource.saveProfile(update.toSaveProfileParams())
            getProfile()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ProfileResult.Failure(failure.toProfileError())
        }
    }

    override suspend fun setAvailableNow(availableNow: Boolean): AvailabilityResult {
        val userId = dataSource.currentUserId() ?: return AvailabilityResult.Failure(ProfileError.NotSignedIn)

        return try {
            dataSource.setAvailableNow(userId, availableNow)
            AvailabilityResult.Success(availableNow)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AvailabilityResult.Failure(failure.toProfileError())
        }
    }

    override suspend fun getPublicProfile(professionalId: String): PublicProfileResult =
        try {
            dataSource
                .findPublicProfile(professionalId)
                ?.toPublicProfile()
                ?.let(PublicProfileResult::Success)
                ?: PublicProfileResult.NotPublished
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            PublicProfileResult.Failure(failure.toProfileError())
        }
}
