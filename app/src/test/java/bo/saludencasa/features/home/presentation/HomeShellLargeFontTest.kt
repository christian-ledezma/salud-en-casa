package bo.saludencasa.features.home.presentation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.auth.presentation.AccountUiState
import bo.saludencasa.features.auth.presentation.RoleSection
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class HomeShellLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bothRoles = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL)

    private fun show(
        held: Set<UserRole>,
        active: UserRole,
        tab: HomeTab = HomeTab.HOME,
        account: AccountUiState = signedIn(held, active),
        professionalHome: ProfessionalHomeUiState = professionalContent(verified = true),
        actions: HomeActions = HomeActions(),
    ) {
        composeRule.setContentAtDoubleFontScale {
            HomeShellContent(
                uiState = account,
                tab = tab,
                onTabSelected = {},
                patientHome = PatientHomeUiState.NothingInProgress,
                professionalHome = professionalHome,
                navigation = HomeNavigation(),
                actions = actions,
            )
        }
    }

    // HT-23, kept by HT-25: the two ways to get care are the first thing a
    // patient finds, the immediate one above.
    @Test
    fun aPatientFindsBothWaysToGetCareFirst() {
        show(bothRoles, UserRole.PATIENT)

        val immediate = composeRule.onNodeWithText(string(R.string.home_immediate_care_title))
        val scheduling = composeRule.onNodeWithText(string(R.string.home_schedule_title))
        composeRule.assertFitsTheScreen(immediate)
        composeRule.assertFitsTheScreen(scheduling)
        assertTrue(immediate.getUnclippedBoundsInRoot().top < scheduling.getUnclippedBoundsInRoot().top)
    }

    // The role is on the line of the greeting, under the avatar that changes it.
    // A pill that wrapped under the name would still be on screen, so the test
    // compares the two rows instead of asking whether the pill is displayed.
    @Test
    fun theRoleSitsOnTheLineOfTheGreeting() {
        show(bothRoles, UserRole.PATIENT)

        val greeting = composeRule.onNodeWithText(string(R.string.home_greeting)).getUnclippedBoundsInRoot()
        val role = composeRule.onNodeWithText(string(R.string.auth_account_role_short_patient))
        composeRule.assertFitsTheScreen(role)
        val pill = role.getUnclippedBoundsInRoot()
        assertTrue(
            "The role pill spans ${pill.top}..${pill.bottom}, off the greeting at ${greeting.top}..${greeting.bottom}.",
            pill.top < greeting.bottom && pill.bottom > greeting.top,
        )
    }

    @Test
    fun everyDestinationOfTheBarStaysOnScreen() {
        show(bothRoles, UserRole.PROFESSIONAL)

        listOf(
            R.string.common_nav_home,
            R.string.common_nav_settings,
            R.string.common_nav_requests,
            R.string.common_nav_account,
        ).forEach { label ->
            composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText(string(label)))
        }
    }

    // The defect no bounds assertion sees: the label measured to its column and
    // cut inside it. "Solicitudes" is eleven characters in a column of about
    // 75 dp, so without the shrink-to-fit it ends in an ellipsis at 200 %.
    @Test
    fun theLongestLabelOfTheBarIsNotCutAtDoubleFontSize() {
        show(bothRoles, UserRole.PATIENT)

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(string(R.string.common_nav_requests), useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)

        assertFalse("The bar label ends in an ellipsis.", layouts.single().isLineEllipsized(0))
    }

    @Test
    fun aProfessionalSeesTheirAvailabilityAndTheRequestsNearby() {
        show(bothRoles, UserRole.PROFESSIONAL)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.home_available_now)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(LONG_SERVICE_TYPE))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.home_nearby_requests_see_all)))
    }

    @Test
    fun anUnverifiedProfessionalIsToldWhyTheyAreNotInTheSearch() {
        show(bothRoles, UserRole.PROFESSIONAL, professionalHome = professionalContent(verified = false))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.home_verification_pending_title)),
        )
    }

    // The menu is where the switch lives now (HT-25). The role labels repeat the
    // pill, so the rows are found by the line each one adds.
    @Test
    fun theAccountMenuOffersBothRolesAndSigningOut() {
        show(bothRoles, UserRole.PATIENT)

        composeRule.onNodeWithContentDescription(string(R.string.cd_account_menu)).performClick()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.home_menu_hint_patient)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.home_menu_hint_professional)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.auth_account_sign_out)))
    }

    // A choice that does nothing tells the person a capability exists and then
    // fails them; with one role there is nowhere to switch into.
    @Test
    fun someoneWithOneRoleIsNotOfferedASwitch() {
        show(setOf(UserRole.PATIENT), UserRole.PATIENT)

        composeRule.onNodeWithContentDescription(string(R.string.cd_account_menu)).performClick()

        composeRule.onNodeWithText(string(R.string.home_menu_use_as)).assertDoesNotExist()
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.auth_account_sign_out)))
    }

    // HT-25 moved four buttons of the old account screen into this tab; each one
    // still has to be reachable.
    @Test
    fun theSettingsOfAProfessionalReachEveryScreenTheyHadBefore() {
        show(bothRoles, UserRole.PROFESSIONAL, tab = HomeTab.SETTINGS)

        listOf(
            R.string.home_settings_verification,
            R.string.home_settings_services,
            R.string.home_settings_availability,
            R.string.home_settings_addresses,
        ).forEach { title ->
            composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(title)))
        }
    }

    @Test
    fun theAccountTabOffersToActivateTheMissingRole() {
        show(setOf(UserRole.PATIENT), UserRole.PATIENT, tab = HomeTab.ACCOUNT)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.auth_account_activate_professional)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.auth_account_sign_out)))
    }

    @Test
    fun anAdministratorReachesTheReviewAndHasNoBar() {
        show(setOf(UserRole.ADMIN), UserRole.ADMIN)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.home_review_documents_title)))
        composeRule.onNodeWithText(string(R.string.common_nav_settings)).assertDoesNotExist()
    }

    // The wiring the review of pull request #2 found broken, and that
    // AccountViewModelTest could not catch: the button has to ask again.
    @Test
    fun retryingAFailedSignOutAsksForItAgain() {
        var signOutRequests = 0
        show(
            bothRoles,
            UserRole.PATIENT,
            account = AccountUiState.Error(AuthError.NetworkUnavailable),
            actions = HomeActions(onSignOut = { signOutRequests++ }),
        )

        val retry = composeRule.onNodeWithText(string(R.string.common_retry))
        composeRule.assertFitsTheScreen(retry)
        retry.performClick()

        assertEquals(1, signOutRequests)
    }

    private fun signedIn(
        held: Set<UserRole>,
        active: UserRole,
    ): AccountUiState =
        AccountUiState.Content(
            session =
                AuthSession(
                    userId = "00000000-0000-0000-0000-000000000000",
                    email = Email.create("ana.quispe@example.com").getOrNull(),
                    fullName = PersonName.create("Ana Quispe").getOrNull(),
                ),
            roleSection = RoleSection(roles = ProfileRoles.create(held, active).getOrThrow()),
        )

    private fun professionalContent(verified: Boolean): ProfessionalHomeUiState =
        ProfessionalHomeUiState.Content(
            availableNow = true,
            availabilityError = null,
            verified = verified,
            nearby = NearbyRequests.Some(listOf(nearby("n1"), nearby("n2"))),
        )

    private fun nearby(id: String): InboxRequest =
        InboxRequest(
            id = id,
            serviceTypeName = if (id == "n1") LONG_SERVICE_TYPE else "Vacunación",
            city = "Cochabamba",
            description = null,
            suggestedBudgetBob = BigDecimal("150.00"),
            distanceM = 1200,
            status = RequestStatus.PUBLISHED,
            createdAt = Instant.parse("2026-10-30T12:00:00Z"),
            modality = RequestModality.IMMEDIATE,
            scheduledAt = null,
        )

    private companion object {
        // The longest name in the catalog, the one that clipped the HU-11 chip.
        const val LONG_SERVICE_TYPE = "Colocación y control de vía venosa"
    }
}
