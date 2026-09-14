package bo.saludencasa.features.profile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignableRoleTest {
    // RF-01.5. The screen builds its options from AssignableRole.entries, so
    // the only way ADMIN could ever reach the selector is by being added here.
    // The database refuses it as well, in assign_my_role and in the trigger
    // profiles_guard_role: this keeps it from being offered in the first place.
    @Test
    fun adminRoleIsNeverSelfAssignable() {
        val offered = AssignableRole.entries.map { it.role }

        assertTrue("ADMIN reached the roles a person can choose.", UserRole.ADMIN !in offered)
        assertEquals(listOf(UserRole.PATIENT, UserRole.PROFESSIONAL), offered)
    }
}
