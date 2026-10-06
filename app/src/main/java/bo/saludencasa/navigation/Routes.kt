package bo.saludencasa.navigation

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
data object AccountRoute

@Serializable
data class PublicProfileRoute(
    val professionalId: String,
)

@Serializable
data object AddressListRoute

@Serializable
data class AddressRoute(
    val addressId: String? = null,
)

@Serializable
data object VerificationRoute
