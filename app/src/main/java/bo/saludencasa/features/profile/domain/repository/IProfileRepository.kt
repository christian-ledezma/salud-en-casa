package bo.saludencasa.features.profile.domain.repository

import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.RoleResult

interface IProfileRepository {
    suspend fun getRole(): RoleResult

    suspend fun assignRole(role: AssignableRole): ChooseRoleResult

    suspend fun getProfile(): ProfileResult

    suspend fun saveProfile(update: ProfileUpdate): ProfileResult
}
