package bo.saludencasa.features.profile.data.repository

import bo.saludencasa.features.profile.data.datasource.SupabaseProfileDataSource
import bo.saludencasa.features.profile.data.mapper.toProfileError
import bo.saludencasa.features.profile.data.mapper.toPublicProfile
import bo.saludencasa.features.profile.data.mapper.toRoleResult
import bo.saludencasa.features.profile.data.mapper.toSaveProfileParams
import bo.saludencasa.features.profile.data.mapper.toUserProfile
import bo.saludencasa.features.profile.data.mapper.toUserRole
import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.repository.IProfileRepository
import kotlinx.coroutines.CancellationException

class ProfileRepository(
    private val dataSource: SupabaseProfileDataSource,
) : IProfileRepository {
    override suspend fun getRoles(): RoleResult {
        val userId = dataSource.currentUserId() ?: return RoleResult.Failure(ProfileError.NotSignedIn)

        return try {
            dataSource.findRoles(userId)?.toRoleResult() ?: RoleResult.Failure(ProfileError.ProfileNotFound)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            RoleResult.Failure(failure.toProfileError())
        }
    }

    override suspend fun addRole(role: AssignableRole): AddRoleResult =
        try {
            dataSource
                .addRole(role.role.name)
                .toUserRole()
                ?.let(AddRoleResult::Success)
                ?: AddRoleResult.Failure(ProfileError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AddRoleResult.Failure(failure.toProfileError())
        }

    override suspend fun setActiveRole(role: UserRole): SwitchRoleResult {
        val userId = dataSource.currentUserId() ?: return SwitchRoleResult.Failure(ProfileError.NotSignedIn)

        return try {
            dataSource.setActiveRole(userId, role.name)
            SwitchRoleResult.Success(role)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            SwitchRoleResult.Failure(failure.toProfileError())
        }
    }

    // Both role rows are read unconditionally, because someone holding both
    // roles has both and the active role does not say which exist. A row that is
    // not there answers with null through its own select policy, so asking for
    // it is cheaper than asking first which roles to ask for.
    override suspend fun getProfile(): ProfileResult {
        val userId = dataSource.currentUserId() ?: return ProfileResult.Failure(ProfileError.NotSignedIn)

        return try {
            val profile = dataSource.findProfile(userId) ?: return ProfileResult.Failure(ProfileError.ProfileNotFound)
            val patient = dataSource.findPatient(userId)
            val professional = dataSource.findProfessional(userId)
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
