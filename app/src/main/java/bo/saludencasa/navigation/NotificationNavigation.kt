package bo.saludencasa.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import bo.saludencasa.features.notifications.domain.model.NotificationDestination
import bo.saludencasa.features.notifications.domain.model.PushPayload

@Composable
internal fun NotificationTapEffect(
    navController: NavHostController,
    pending: PushPayload?,
    onHandled: () -> Unit,
    resolve: suspend (PushPayload) -> NotificationDestination?,
) {
    val entry by navController.currentBackStackEntryAsState()
    val starting = entry?.destination?.hasRoute<StartupRoute>() != false

    LaunchedEffect(pending, starting) {
        val payload = pending ?: return@LaunchedEffect
        if (starting) return@LaunchedEffect

        // Navigating before clearing the tap: clearing it restarts this effect,
        // which would cancel anything still running in it.
        resolve(payload)?.let(navController::openFromNotification)
        onHandled()
    }
}

// docs/decisions.md, 2026-10-16, "el teléfono se identifica por su instalación", 8.
private fun NavHostController.openFromNotification(destination: NotificationDestination) {
    navigate(AccountRoute) {
        popUpTo<AccountRoute> { inclusive = true }
    }
    navigate(
        when (destination) {
            NotificationDestination.Inbox -> {
                InboxRoute
            }

            NotificationDestination.MyRequests -> {
                MyRequestsRoute()
            }

            is NotificationDestination.OfferThread -> {
                OfferThreadRoute(destination.requestId, destination.professionalId)
            }
        },
    )
}
