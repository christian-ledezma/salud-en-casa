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
import bo.saludencasa.features.catalog.presentation.MyServicesScreen
import bo.saludencasa.features.location.presentation.AddressListScreen
import bo.saludencasa.features.location.presentation.AddressScreen
import bo.saludencasa.features.profile.presentation.ProfileScreen
import bo.saludencasa.features.profile.presentation.PublicProfileScreen
import bo.saludencasa.features.profile.presentation.RoleSelectionScreen
import bo.saludencasa.features.search.presentation.ProfessionalSearchScreen
import bo.saludencasa.features.verification.presentation.DocumentReviewQueueScreen
import bo.saludencasa.features.verification.presentation.DocumentReviewScreen
import bo.saludencasa.features.verification.presentation.VerificationScreen

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
                onOpenSearch = { navController.navigate(ProfessionalSearchRoute) },
                onOpenAddress = { navController.navigate(AddressListRoute) },
                onOpenVerification = { navController.navigate(VerificationRoute) },
                onOpenDocumentReview = { navController.navigate(DocumentReviewQueueRoute) },
            )
        }

        composable<ProfileRoute> {
            ProfileScreen(
                onOpenPublicProfile = { professionalId -> navController.navigate(PublicProfileRoute(professionalId)) },
                onOpenMyServices = { navController.navigate(MyServicesRoute) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<ProfessionalSearchRoute> {
            ProfessionalSearchScreen(
                onBack = { navController.popBackStack() },
                onOpenProfessional = { professionalId ->
                    navController.navigate(PublicProfileRoute(professionalId))
                },
                onRegisterAddress = { navController.navigate(AddressRoute()) },
            )
        }

        composable<MyServicesRoute> {
            MyServicesScreen(onBack = { navController.popBackStack() })
        }

        composable<AddressListRoute> {
            AddressListScreen(
                onAddClick = { navController.navigate(AddressRoute()) },
                onEditClick = { addressId -> navController.navigate(AddressRoute(addressId)) },
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
            )
        }

        composable<PublicProfileRoute> { entry ->
            PublicProfileScreen(
                professionalId = entry.toRoute<PublicProfileRoute>().professionalId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<VerificationRoute> {
            VerificationScreen(onBack = { navController.popBackStack() })
        }

        composable<DocumentReviewQueueRoute> {
            DocumentReviewQueueScreen(
                onBack = { navController.popBackStack() },
                onOpenReview = { profileId -> navController.navigate(DocumentReviewRoute(profileId)) },
            )
        }

        composable<DocumentReviewRoute> { entry ->
            DocumentReviewScreen(
                profileId = entry.toRoute<DocumentReviewRoute>().profileId,
                onBack = { navController.popBackStack() },
            )
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
