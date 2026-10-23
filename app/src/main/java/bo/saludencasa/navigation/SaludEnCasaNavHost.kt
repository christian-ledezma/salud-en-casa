package bo.saludencasa.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import bo.saludencasa.features.auth.presentation.AccountScreen
import bo.saludencasa.features.auth.presentation.StartupScreen
import bo.saludencasa.features.auth.presentation.WelcomeScreen
import bo.saludencasa.features.availability.presentation.AvailabilityScreen
import bo.saludencasa.features.catalog.presentation.MyServicesScreen
import bo.saludencasa.features.location.presentation.AddressListScreen
import bo.saludencasa.features.location.presentation.AddressScreen
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.presentation.OpenNotificationViewModel
import bo.saludencasa.features.profile.presentation.ProfileScreen
import bo.saludencasa.features.profile.presentation.PublicProfileScreen
import bo.saludencasa.features.profile.presentation.RoleSelectionScreen
import bo.saludencasa.features.requests.presentation.CreateRequestScreen
import bo.saludencasa.features.requests.presentation.InboxScreen
import bo.saludencasa.features.requests.presentation.MyRequestsScreen
import bo.saludencasa.features.requests.presentation.ScheduleAppointmentScreen
import bo.saludencasa.features.search.presentation.ProfessionalSearchScreen
import bo.saludencasa.features.verification.presentation.DocumentReviewQueueScreen
import bo.saludencasa.features.verification.presentation.DocumentReviewScreen
import bo.saludencasa.features.verification.presentation.VerificationScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SaludEnCasaNavHost(
    modifier: Modifier = Modifier,
    pendingNotification: PushPayload? = null,
    onNotificationHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    openNotification: OpenNotificationViewModel = koinViewModel(),
) {
    NotificationTapEffect(
        navController = navController,
        pending = pendingNotification,
        onHandled = onNotificationHandled,
        resolve = openNotification::resolve,
    )

    NavHost(
        navController = navController,
        startDestination = StartupRoute(),
        modifier = modifier,
    ) {
        // docs/decisions.md, 2026-10-14, the welcome screen.
        composable<StartupRoute>(
            exitTransition = {
                if (targetState.destination.hasRoute<WelcomeRoute>()) ExitTransition.None else null
            },
        ) { entry ->
            StartupScreen(
                playIntro = entry.toRoute<StartupRoute>().playIntro,
                onSignIn = { navController.replaceCurrentWith(WelcomeRoute) },
                onChooseRole = { navController.replaceCurrentWith(RoleSelectionRoute) },
                onHome = { navController.replaceCurrentWith(AccountRoute) },
            )
        }

        composable<WelcomeRoute>(
            enterTransition = {
                if (initialState.destination.hasRoute<StartupRoute>()) EnterTransition.None else null
            },
        ) {
            WelcomeScreen(onSignedIn = { navController.replaceCurrentWith(StartupRoute(playIntro = false)) })
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
                onOpenCreateRequest = { navController.navigate(CreateRequestRoute) },
                onOpenMyRequests = { navController.navigate(MyRequestsRoute()) },
                onOpenInbox = { navController.navigate(InboxRoute()) },
                onOpenAvailability = { navController.navigate(AvailabilityRoute) },
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

        composable<CreateRequestRoute> {
            CreateRequestScreen(
                onBack = { navController.popBackStack() },
                // The published request lands on the list that shows it, and the
                // create screen is torn off the back stack so returning does not
                // reopen a spent form.
                onCreated = {
                    navController.navigate(MyRequestsRoute(justCreated = true)) {
                        popUpTo(CreateRequestRoute) { inclusive = true }
                    }
                },
                onRegisterAddress = { navController.navigate(AddressRoute()) },
            )
        }

        composable<MyRequestsRoute> { entry ->
            MyRequestsScreen(
                justCreated = entry.toRoute<MyRequestsRoute>().justCreated,
                onBack = { navController.popBackStack() },
                onCreateRequest = { navController.navigate(CreateRequestRoute) },
            )
        }

        composable<InboxRoute> { entry ->
            InboxScreen(
                highlightRequestId = entry.toRoute<InboxRoute>().highlightRequestId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<AvailabilityRoute> {
            AvailabilityScreen(onBack = { navController.popBackStack() })
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
            val professionalId = entry.toRoute<PublicProfileRoute>().professionalId
            PublicProfileScreen(
                professionalId = professionalId,
                onBack = { navController.popBackStack() },
                onSchedule = { navController.navigate(ScheduleAppointmentRoute(professionalId)) },
            )
        }

        composable<ScheduleAppointmentRoute> { entry ->
            ScheduleAppointmentScreen(
                professionalId = entry.toRoute<ScheduleAppointmentRoute>().professionalId,
                onBack = { navController.popBackStack() },
                onCreated = {
                    navController.navigate(MyRequestsRoute(justCreated = true)) {
                        popUpTo<ScheduleAppointmentRoute> { inclusive = true }
                    }
                },
                onRegisterAddress = { navController.navigate(AddressRoute()) },
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
