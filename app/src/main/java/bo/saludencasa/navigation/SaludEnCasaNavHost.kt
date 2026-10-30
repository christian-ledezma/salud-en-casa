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
import bo.saludencasa.features.auth.presentation.StartupScreen
import bo.saludencasa.features.auth.presentation.WelcomeScreen
import bo.saludencasa.features.availability.presentation.AvailabilityScreen
import bo.saludencasa.features.catalog.presentation.MyServicesScreen
import bo.saludencasa.features.home.presentation.HomeNavigation
import bo.saludencasa.features.home.presentation.HomeShell
import bo.saludencasa.features.location.presentation.AddressListScreen
import bo.saludencasa.features.location.presentation.AddressScreen
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.presentation.OpenNotificationViewModel
import bo.saludencasa.features.profile.presentation.ProfileScreen
import bo.saludencasa.features.profile.presentation.PublicProfileScreen
import bo.saludencasa.features.profile.presentation.RoleSelectionScreen
import bo.saludencasa.features.requests.presentation.ImmediateCareScreen
import bo.saludencasa.features.requests.presentation.InboxScreen
import bo.saludencasa.features.requests.presentation.MyRequestsScreen
import bo.saludencasa.features.requests.presentation.NegotiationEntry
import bo.saludencasa.features.requests.presentation.OfferThreadScreen
import bo.saludencasa.features.requests.presentation.ScheduleAppointmentScreen
import bo.saludencasa.features.search.presentation.ProfessionalSearchScreen
import bo.saludencasa.features.services.presentation.AttentionScreen
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
                onHome = { navController.replaceCurrentWith(HomeRoute) },
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
                onRoleAssigned = { navController.replaceCurrentWith(HomeRoute) },
            )
        }

        composable<HomeRoute> {
            HomeShell(
                onSignedOut = { navController.replaceCurrentWith(WelcomeRoute) },
                navigation =
                    HomeNavigation(
                        onOpenImmediateCare = { navController.navigate(ImmediateCareRoute) },
                        onOpenScheduling = { navController.navigate(ProfessionalSearchRoute) },
                        onOpenNegotiation = navController::openNegotiation,
                        onOpenInboxRequest = { requestId -> navController.navigate(OfferThreadRoute(requestId)) },
                        onOpenProfile = { navController.navigate(ProfileRoute) },
                        onOpenMyServices = { navController.navigate(MyServicesRoute) },
                        onOpenAvailability = { navController.navigate(AvailabilityRoute) },
                        onOpenAddresses = { navController.navigate(AddressListRoute) },
                        onOpenVerification = { navController.navigate(VerificationRoute) },
                        onOpenDocumentReview = { navController.navigate(DocumentReviewQueueRoute) },
                    ),
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
                onSchedule = { professionalId, serviceTypeId ->
                    navController.navigate(ScheduleAppointmentRoute(professionalId, serviceTypeId))
                },
                onRegisterAddress = { navController.navigate(AddressRoute()) },
            )
        }

        // docs/decisions.md, 2026-10-23: publishing stays on this screen, which
        // turns into the wait for offers.
        composable<ImmediateCareRoute> {
            ImmediateCareScreen(
                onBack = { navController.popBackStack() },
                onOpenProfessional = { professionalId -> navController.navigate(PublicProfileRoute(professionalId)) },
                onOpenMyRequests = { navController.navigate(MyRequestsRoute()) },
                onRegisterAddress = { navController.navigate(AddressRoute()) },
                onOpenThread = { requestId, professionalId ->
                    navController.navigate(OfferThreadRoute(requestId, professionalId))
                },
            )
        }

        composable<MyRequestsRoute> { entry ->
            MyRequestsScreen(
                justCreated = entry.toRoute<MyRequestsRoute>().justCreated,
                onBack = { navController.popBackStack() },
                onCreateRequest = { navController.navigate(ImmediateCareRoute) },
                onOpenRequest = navController::openNegotiation,
            )
        }

        composable<InboxRoute> {
            InboxScreen(
                onBack = { navController.popBackStack() },
                onOpenRequest = { requestId -> navController.navigate(OfferThreadRoute(requestId)) },
            )
        }

        // HU-18 and HT-24: the one screen of a negotiation, for both sides.
        composable<OfferThreadRoute> { entry ->
            val route = entry.toRoute<OfferThreadRoute>()
            OfferThreadScreen(
                requestId = route.requestId,
                professionalId = route.professionalId,
                onBack = { navController.popBackStack() },
                // HU-19: the thread is done; replace it with the attention so a
                // back press does not return to the accepted negotiation.
                onAccepted = {
                    navController.navigate(AttentionRoute(route.requestId)) {
                        popUpTo<OfferThreadRoute> { inclusive = true }
                    }
                },
            )
        }

        // HU-19: the sealed agreement, for both parties. The patient opens the
        // professional's existing public profile from here.
        composable<AttentionRoute> { entry ->
            AttentionScreen(
                requestId = entry.toRoute<AttentionRoute>().requestId,
                onBack = { navController.popBackStack() },
                onOpenProfessionalProfile = { professionalId ->
                    navController.navigate(PublicProfileRoute(professionalId))
                },
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
                    if (navController.previousBackStackEntry?.destination?.hasRoute<AddressListRoute>() == true) {
                        navController.navigate(AddressListRoute) {
                            popUpTo(AddressListRoute) { inclusive = true }
                        }
                    } else {
                        // Opened from a form, «Atención ahora» or «Agendar»: back
                        // to it, which reads its addresses again on resume and
                        // chooses the one just added (docs/decisions.md,
                        // 2026-10-27, HT-24).
                        navController.popBackStack()
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
            val route = entry.toRoute<ScheduleAppointmentRoute>()
            ScheduleAppointmentScreen(
                professionalId = route.professionalId,
                serviceTypeId = route.serviceTypeId,
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

// docs/decisions.md, 2026-10-27, HT-24: no list between the card and the
// negotiation. Shared by "Mis solicitudes" on its own and as a tab of the home screen.
private fun NavHostController.openNegotiation(negotiation: NegotiationEntry) {
    when (negotiation) {
        // Back to the wait already on the stack, if the list was opened from it,
        // instead of a second one on top.
        NegotiationEntry.ImmediateCare -> {
            navigate(ImmediateCareRoute) {
                popUpTo<ImmediateCareRoute>()
                launchSingleTop = true
            }
        }

        is NegotiationEntry.Thread -> {
            navigate(OfferThreadRoute(negotiation.requestId, negotiation.professionalId))
        }

        is NegotiationEntry.Attention -> {
            navigate(AttentionRoute(negotiation.requestId))
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
