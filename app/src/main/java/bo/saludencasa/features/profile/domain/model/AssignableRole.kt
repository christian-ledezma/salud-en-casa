package bo.saludencasa.features.profile.domain.model

// docs/decisions.md, 2026-09-13: el rol que una persona puede elegir es un tipo
// aparte del rol que un perfil puede tener.
enum class AssignableRole(
    val role: UserRole,
) {
    PATIENT(UserRole.PATIENT),
    PROFESSIONAL(UserRole.PROFESSIONAL),
}
