package bo.saludencasa.navigation

import kotlinx.serialization.Serializable

// Typed routes, never strings assembled by hand: a renamed destination then
// fails to compile instead of failing at run time on a device.
@Serializable
data object StartupRoute

@Serializable
data object WelcomeRoute

@Serializable
data object AccountRoute
