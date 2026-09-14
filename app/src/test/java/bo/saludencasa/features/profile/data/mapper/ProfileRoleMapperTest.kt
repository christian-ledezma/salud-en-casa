package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.features.profile.data.model.ProfileRoleDto
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRoleMapperTest {
    // The trigger handle_new_user leaves role null on the first sign in. Reading
    // that absence as anything other than «not chosen yet» would either skip the
    // question RF-01.4 requires or repeat it forever.
    @Test
    fun `a profile without a role is unassigned, not a failure`() {
        assertEquals(RoleResult.Unassigned, ProfileRoleDto(role = null).toRoleResult())
    }

    @Test
    fun `each role the database stores maps to its own domain value`() {
        assertEquals(RoleResult.Assigned(UserRole.PATIENT), ProfileRoleDto(role = "PATIENT").toRoleResult())
        assertEquals(RoleResult.Assigned(UserRole.PROFESSIONAL), ProfileRoleDto(role = "PROFESSIONAL").toRoleResult())
        assertEquals(RoleResult.Assigned(UserRole.ADMIN), ProfileRoleDto(role = "ADMIN").toRoleResult())
    }

    // A value the enum user_role gains later arrives here as a string this
    // build has never seen. Falling back to «unassigned» would ask an assigned
    // person to choose again and the database would refuse the answer.
    @Test
    fun `a role this build does not know is a failure, never an absent role`() {
        assertEquals(RoleResult.Failure(ProfileError.Unexpected), ProfileRoleDto(role = "AUDITOR").toRoleResult())
        assertNull("AUDITOR".toUserRole())
    }
}
