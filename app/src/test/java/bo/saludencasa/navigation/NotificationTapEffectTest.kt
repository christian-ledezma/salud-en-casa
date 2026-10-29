package bo.saludencasa.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.features.notifications.REQUEST_ID
import bo.saludencasa.features.notifications.domain.model.NotificationDestination
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.nearbyRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationTapEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController
    private var pending by mutableStateOf<PushPayload?>(null)
    private var resolves = 0

    private fun show(destination: NotificationDestination?) {
        composeRule.setContent {
            navController = rememberNavController()
            NotificationTapEffect(
                navController = navController,
                pending = pending,
                onHandled = { pending = null },
                resolve = {
                    resolves++
                    destination
                },
            )
            NavHost(navController = navController, startDestination = StartupRoute()) {
                composable<StartupRoute> { Text("startup") }
                composable<AccountRoute> { Text("account") }
                composable<InboxRoute> { Text("inbox") }
                composable<MyRequestsRoute> { Text("requests") }
                composable<OfferThreadRoute> { Text("thread") }
            }
        }
    }

    private fun finishStartup() {
        composeRule.runOnIdle {
            navController.navigate(AccountRoute) { popUpTo<StartupRoute> { inclusive = true } }
        }
        composeRule.waitForIdle()
    }

    // A cold start from a notification: the startup screen is still deciding
    // whether the person is signed in, and opening the request under it would be
    // undone the moment it finishes. HT-24: a request for the professional opens
    // that request, with no professionalId, as its professional.
    @Test
    fun aTapWaitsWhileTheStartupScreenIsShowing() {
        pending = nearbyRequest()
        show(NotificationDestination.OfferThread(REQUEST_ID, professionalId = null))
        composeRule.waitForIdle()

        assertEquals(0, resolves)
        assertTrue(navController.currentDestination!!.hasRoute<StartupRoute>())

        finishStartup()

        val route = navController.currentBackStackEntry!!.toRoute<OfferThreadRoute>()
        assertEquals(REQUEST_ID, route.requestId)
        assertNull(route.professionalId)
        assertTrue(navController.previousBackStackEntry!!.destination.hasRoute<AccountRoute>())
    }

    // Recomposition must not open the inbox a second time, nor ask the server
    // to switch the role twice.
    @Test
    fun aTapIsHandledOnlyOnce() {
        pending = nearbyRequest()
        show(NotificationDestination.Inbox)
        finishStartup()

        composeRule.runOnIdle { navController.popBackStack() }
        composeRule.waitForIdle()

        assertEquals(1, resolves)
        assertNull(pending)
        assertTrue(navController.currentDestination!!.hasRoute<AccountRoute>())
    }

    // HU-18: a counter-offer opens the thread it answers, with the account
    // rebuilt underneath it, so going back lands somewhere that exists.
    @Test
    fun aCounterOfferTapOpensItsThread() {
        pending = nearbyRequest()
        show(NotificationDestination.OfferThread(REQUEST_ID, "professional-1"))
        finishStartup()

        val route = navController.currentBackStackEntry!!.toRoute<OfferThreadRoute>()
        assertEquals(REQUEST_ID, route.requestId)
        assertEquals("professional-1", route.professionalId)
        assertTrue(navController.previousBackStackEntry!!.destination.hasRoute<AccountRoute>())
    }

    // Signed out, or meant for another account: the tap is spent and the
    // person stays where the app put them.
    @Test
    fun aTapThatCannotBeHonouredOpensNothing() {
        pending = nearbyRequest()
        show(destination = null)
        finishStartup()

        assertNull(pending)
        assertTrue(navController.currentDestination!!.hasRoute<AccountRoute>())
    }
}
