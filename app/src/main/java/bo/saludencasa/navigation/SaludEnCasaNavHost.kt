package bo.saludencasa.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import bo.saludencasa.features.auth.presentation.AccountScreen
import bo.saludencasa.features.auth.presentation.StartupScreen
import bo.saludencasa.features.auth.presentation.WelcomeScreen
import bo.saludencasa.features.location.presentation.AddressListScreen
import bo.saludencasa.features.location.presentation.AddressScreen
import bo.saludencasa.features.profile.presentation.ProfileScreen
import bo.saludencasa.features.profile.presentation.PublicProfileScreen
import bo.saludencasa.features.profile.presentation.RoleSelectionScreen

@Composable
fun SaludEnCasaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = StartupRoute,
        modifier = modifier,
    ) {
        composable<StartupRoute> {
            StartupScreen(
                onSignIn = { navController.replaceCurrentWith(WelcomeRoute) },
                onChooseRole = { navController.replaceCurrentWith(RoleSelectionRoute) },
                onHome = { navController.replaceCurrentWith(AccountRoute) },
            )
        }

        composable<WelcomeRoute> {
            WelcomeScreen(onSignedIn = { navController.replaceCurrentWith(StartupRoute) })
        }

        composable<RoleSelectionRoute> {
            RoleSelectionScreen(
                onRoleAssigned = { navController.replaceCurrentWith(AccountRoute) },
            )
        }

        composable<AccountRoute> {
            AccountScreen(
                onSignedOut = { navController.replaceCurrentWith(WelcomeRoute) },
                onOpenProfile = { navController.navigate(ProfileRoute) },
                onOpenAddress = { navController.navigate(AddressListRoute) },
            )
        }

        composable<ProfileRoute> {
            ProfileScreen(
                onOpenPublicProfile = { professionalId -> navController.navigate(PublicProfileRoute(professionalId)) },
            )
        }

        composable<AddressListRoute> {
            AddressListScreen(
                onAddClick = { navController.navigate(AddressRoute()) },
                onEditClick = { addressId -> navController.navigate(AddressRoute(addressId)) },
            )
        }

        composable<AddressRoute> { entry ->
            AddressScreen(
                addressId = entry.toRoute<AddressRoute>().addressId,
                // A plain popBackStack() would return to the same
                // AddressListRoute entry, whose AddressListViewModel already
                // loaded once and never reloads on its own: the address just
                // saved would be missing until the person left the screen and
                // came back. Popping the list route itself and navigating to
                // it again tears down that stale instance and starts a fresh
                // one, which loads the list the address was just added to.
                onSaved = {
                    navController.navigate(AddressListRoute) {
                        popUpTo(AddressListRoute) { inclusive = true }
                    }
                },
            )
        }

        composable<PublicProfileRoute> { entry ->
            PublicProfileScreen(professionalId = entry.toRoute<PublicProfileRoute>().professionalId)
        }
    }
}

private fun NavHostController.replaceCurrentWith(route: Any) {
    val leaving = currentDestination?.id ?: graph.startDestinationId
    navigate(route) {
        popUpTo(leaving) { inclusive = true }
        launchSingleTop = true
    }
}
