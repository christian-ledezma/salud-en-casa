package bo.saludencasa.features.profile.data.repository

import bo.saludencasa.features.profile.data.datasource.SupabaseProfileDataSource
import bo.saludencasa.features.profile.data.mapper.toProfileError
import bo.saludencasa.features.profile.data.mapper.toRoleResult
import bo.saludencasa.features.profile.data.mapper.toUserRole
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
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
}
