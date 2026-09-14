package bo.saludencasa.features.profile.domain.model

// docs/decisions.md, 2026-09-13: the role a person may choose is a type apart
// from the role a profile may hold.
enum class AssignableRole(
    val role: UserRole,
) {
    PATIENT(UserRole.PATIENT),
    PROFESSIONAL(UserRole.PROFESSIONAL),
}
