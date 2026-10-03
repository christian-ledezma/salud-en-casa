package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.features.profile.data.model.ProfileRolesDto
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRolesMapperTest {
    @Test
    fun `a profile with no roles yet reads as an empty set, not a failure`() {
        val result = ProfileRolesDto(activeRole = null, heldRoles = emptyList()).toRoleResult()

        val roles = (result as RoleResult.Loaded).roles
        assertTrue(roles.hasNoRole)
        assertNull(roles.active)
    }

    @Test
    fun `both roles arrive as a set with the active one named apart`() {
        val result =
            ProfileRolesDto(
                activeRole = "PROFESSIONAL",
                heldRoles = listOf("PATIENT", "PROFESSIONAL"),
            ).toRoleResult()

        val roles = (result as RoleResult.Loaded).roles
        assertEquals(setOf(UserRole.PATIENT, UserRole.PROFESSIONAL), roles.held)
        assertEquals(UserRole.PROFESSIONAL, roles.active)
    }

    // Dropping the unknown value would silently shrink the set. For someone who
    // holds both roles that reads as having lost one, and the screen would offer
    // to add a role they already have.
    @Test
    fun `a held role this build does not know is a failure, never a smaller set`() {
        val result =
            ProfileRolesDto(activeRole = "PATIENT", heldRoles = listOf("PATIENT", "AUDITOR")).toRoleResult()

        assertEquals(RoleResult.Failure(ProfileError.Unexpected), result)
    }

    @Test
    fun `an active role this build does not know is a failure, never an absent role`() {
        val result =
            ProfileRolesDto(activeRole = "AUDITOR", heldRoles = listOf("PATIENT")).toRoleResult()

        assertEquals(RoleResult.Failure(ProfileError.Unexpected), result)
    }

    // The database cannot store this, so reading it back means the reply is not
    // a role state this build can trust. Believing it would put the person in a
    // mode with no record behind it.
    @Test
    fun `an active role outside the held ones is a failure`() {
        val result =
            ProfileRolesDto(activeRole = "PROFESSIONAL", heldRoles = listOf("PATIENT")).toRoleResult()

        assertEquals(RoleResult.Failure(ProfileError.Unexpected), result)
    }
}
