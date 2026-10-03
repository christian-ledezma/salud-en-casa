package bo.saludencasa.features.profile.domain.repository

import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserRole

interface IProfileRepository {
    suspend fun getRoles(): RoleResult

    suspend fun addRole(role: AssignableRole): AddRoleResult

    suspend fun setActiveRole(role: UserRole): SwitchRoleResult

    suspend fun getProfile(): ProfileResult

    suspend fun saveProfile(update: ProfileUpdate): ProfileResult

    suspend fun setAvailableNow(availableNow: Boolean): AvailabilityResult

    suspend fun getPublicProfile(professionalId: String): PublicProfileResult
}
