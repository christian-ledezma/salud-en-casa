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
                onSignedIn = { navController.replaceGraphWith(AccountRoute) },
                onSignedOut = { navController.replaceGraphWith(WelcomeRoute) },
            )
        }

        composable<WelcomeRoute> {
            WelcomeScreen(onSignedIn = { navController.replaceGraphWith(AccountRoute) })
        }

        composable<AccountRoute> {
            AccountScreen(onSignedOut = { navController.replaceGraphWith(WelcomeRoute) })
        }
    }
}

// Signing in and signing out both change who the application belongs to, so the
// previous destination must not survive the back button.
private fun NavHostController.replaceGraphWith(route: Any) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}
