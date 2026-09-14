package bo.saludencasa.features.profile

import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

class FakeProfileRepository(
    var roleResult: RoleResult = RoleResult.Unassigned,
    var assignResult: ChooseRoleResult? = null,
) : IProfileRepository {
    var roleReads: Int = 0
        private set
    var assignAttempts: Int = 0
        private set
    var lastAssignedRole: AssignableRole? = null
        private set

    override suspend fun getRole(): RoleResult {
        roleReads++
        return roleResult
    }

    override suspend fun assignRole(role: AssignableRole): ChooseRoleResult {
        assignAttempts++
        lastAssignedRole = role
        return assignResult ?: ChooseRoleResult.Success(role.role)
    }
}
