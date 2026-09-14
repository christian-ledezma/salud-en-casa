package bo.saludencasa.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import bo.saludencasa.features.auth.presentation.AccountScreen
import bo.saludencasa.features.auth.presentation.StartupScreen
import bo.saludencasa.features.auth.presentation.WelcomeScreen
import bo.saludencasa.features.profile.presentation.ProfileScreen
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
            )
        }

        composable<ProfileRoute> {
            ProfileScreen()
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
