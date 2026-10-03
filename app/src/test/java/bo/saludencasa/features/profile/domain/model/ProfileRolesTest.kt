package bo.saludencasa.features.profile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRolesTest {
    // The composite foreign key from profiles to profile_roles makes this
    // impossible to store. Modelling it here too means a reply that carries it
    // becomes a failure the startup screen retries, instead of putting the
    // person in a mode whose record does not exist.
    @Test
    fun activeRoleMustBeOneOfTheHeldRoles() {
        val result = ProfileRoles.create(held = setOf(UserRole.PATIENT), active = UserRole.PROFESSIONAL)

        assertTrue(result.isFailure)
    }

    // The other direction of the same invariant. add_my_role always leaves the
    // new role active, so holding roles with none active means the row was
    // written by something that does not follow the rule.
    @Test
    fun heldRolesWithoutAnActiveOneAreRefused() {
        val result = ProfileRoles.create(held = setOf(UserRole.PATIENT), active = null)

        assertTrue(result.isFailure)
    }

    @Test
    fun aPersonWithNoRolesHasNoActiveRole() {
        val roles = ProfileRoles.create(held = emptySet(), active = null).getOrThrow()

        assertTrue(roles.hasNoRole)
        assertNull(roles.active)
    }

    @Test
    fun bothRolesHeldAtOnceIsAValidState() {
        val roles =
            ProfileRoles
                .create(
                    held = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL),
                    active = UserRole.PROFESSIONAL,
                ).getOrThrow()

        assertTrue(roles.has(UserRole.PATIENT))
        assertTrue(roles.has(UserRole.PROFESSIONAL))
        assertEquals(UserRole.PROFESSIONAL, roles.active)
    }

    // A control that offers nowhere to go is worse than no control: it tells the
    // person a capability exists and then does nothing when they use it.
    @Test
    fun switchingNeedsMoreThanOneRoleToSwitchInto() {
        val single = ProfileRoles.create(setOf(UserRole.PATIENT), UserRole.PATIENT).getOrThrow()
        val both =
            ProfileRoles
                .create(setOf(UserRole.PATIENT, UserRole.PROFESSIONAL), UserRole.PATIENT)
                .getOrThrow()

        assertFalse(single.canSwitch)
        assertTrue(both.canSwitch)
    }

    // ADMIN is granted manually and never takes part in the alternation, so an
    // administrator who is also a patient still has nothing to switch into.
    @Test
    fun theAdminRoleIsNeverSomethingToSwitchInto() {
        val roles =
            ProfileRoles
                .create(setOf(UserRole.PATIENT, UserRole.ADMIN), UserRole.PATIENT)
                .getOrThrow()

        assertEquals(listOf(UserRole.PATIENT), roles.switchable)
        assertFalse(roles.canSwitch)
    }

    @Test
    fun theRoleAlreadyHeldIsNotOfferedToAdd() {
        val roles = ProfileRoles.create(setOf(UserRole.PATIENT), UserRole.PATIENT).getOrThrow()

        assertEquals(listOf(AssignableRole.PROFESSIONAL), roles.addable)
    }

    @Test
    fun someoneWithBothRolesHasNothingLeftToAdd() {
        val roles =
            ProfileRoles
                .create(setOf(UserRole.PATIENT, UserRole.PROFESSIONAL), UserRole.PATIENT)
                .getOrThrow()

        assertTrue(roles.addable.isEmpty())
    }
}
