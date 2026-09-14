package bo.saludencasa.navigation

import bo.saludencasa.features.profile.domain.model.UserRole
import kotlinx.serialization.Serializable

@Serializable
data object StartupRoute

@Serializable
data object WelcomeRoute

@Serializable
data object RoleSelectionRoute

@Serializable
data object ProfileRoute

@Serializable
data class AccountRoute(
    val role: UserRole,
)
