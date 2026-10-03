package bo.saludencasa.features.profile.domain.model

// The database guarantees the same rule with a composite foreign key from
// profiles to profile_roles (docs/decisions.md, 2026-10-01). Modelling it here
// too keeps an impossible state from being representable in memory, so a reply
// the server should never send becomes a failure the startup screen can retry
// instead of a role the application silently believes in.
@ConsistentCopyVisibility
data class ProfileRoles private constructor(
    val held: Set<UserRole>,
    val active: UserRole?,
) {
    val hasNoRole: Boolean get() = held.isEmpty()

    fun has(role: UserRole): Boolean = role in held

    // ADMIN is granted manually and never takes part in the alternation, so it
    // is not something to switch into even for someone who holds it.
    val switchable: List<UserRole> get() = held.filterNot { it == UserRole.ADMIN }.sorted()

    val canSwitch: Boolean get() = switchable.size > 1

    // AssignableRole stays the only place that lists what a person may grant
    // themselves, which is what keeps adminRoleIsNeverSelfAssignable meaningful.
    val addable: List<AssignableRole> get() = AssignableRole.entries.filterNot { has(it.role) }

    companion object {
        fun create(
            held: Set<UserRole>,
            active: UserRole?,
        ): Result<ProfileRoles> =
            when {
                active != null && active !in held -> failure("active_role_must_be_held")
                active == null && held.isNotEmpty() -> failure("held_roles_require_an_active_role")
                else -> Result.success(ProfileRoles(held, active))
            }

        val none: ProfileRoles = ProfileRoles(emptySet(), null)

        private fun failure(key: String): Result<ProfileRoles> = Result.failure(IllegalArgumentException(key))
    }
}
