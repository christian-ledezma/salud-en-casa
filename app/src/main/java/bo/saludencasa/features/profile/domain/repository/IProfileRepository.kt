package bo.saludencasa.features.profile.domain.repository

import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.RoleResult

interface IProfileRepository {
    suspend fun getRole(): RoleResult

    suspend fun assignRole(role: AssignableRole): ChooseRoleResult
}
