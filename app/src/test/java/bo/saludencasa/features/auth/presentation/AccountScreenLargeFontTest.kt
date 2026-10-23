package bo.saludencasa.features.auth.presentation

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        held: Set<UserRole>,
        active: UserRole,
    ) {
        val session =
            AuthSession(
                userId = "00000000-0000-0000-0000-000000000000",
                email = Email.create("ana.quispe@example.com").getOrNull(),
                fullName = PersonName.create("Ana Quispe").getOrNull(),
            )
        val roles = RoleSection(roles = ProfileRoles.create(held, active).getOrThrow())
        composeRule.setContentAtDoubleFontScale {
            AccountContent(
                uiState = AccountUiState.Content(session, roles),
                onSignOutClick = {},
                onDismissError = {},
                onOpenProfile = {},
                onOpenImmediateCare = {},
                onOpenScheduling = {},
                onOpenMyRequests = {},
                onOpenInbox = {},
                onOpenAvailability = {},
                onOpenAddress = {},
                onOpenVerification = {},
                onOpenDocumentReview = {},
                onSwitchTo = {},
                onActivate = {},
            )
        }
    }

    // The Sprint 2.5 defect: the segmented control overflowed its width at 200 %,
    // which is why its labels were shortened. Both options have to stay readable.
    @Test
    fun bothRolesOfTheSwitchStayOnScreen() {
        show(setOf(UserRole.PATIENT, UserRole.PROFESSIONAL), UserRole.PATIENT)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.auth_account_role_short_patient)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.auth_account_role_short_professional)),
        )
    }

    // HT-23: the two ways to get care are the first thing a patient finds, above
    // the list of what they already asked for and above the account.
    @Test
    fun aPatientFindsBothWaysToGetCareFirst() {
        show(setOf(UserRole.PATIENT), UserRole.PATIENT)

        val immediate = composeRule.onNodeWithText(string(R.string.auth_account_immediate_care_title))
        val scheduling = composeRule.onNodeWithText(string(R.string.auth_account_schedule_title))
        composeRule.assertFitsTheScreen(immediate)
        composeRule.assertFitsTheScreen(scheduling)

        val myRequests = composeRule.onNodeWithText(string(R.string.auth_account_open_my_requests))
        composeRule.assertFitsTheScreen(myRequests)
        val schedulingTop = scheduling.getUnclippedBoundsInRoot().top
        assertTrue(immediate.getUnclippedBoundsInRoot().top < schedulingTop)
        assertTrue(schedulingTop < myRequests.getUnclippedBoundsInRoot().top)
    }

    @Test
    fun aProfessionalIsNotOfferedThePatientsWaysToGetCare() {
        show(setOf(UserRole.PATIENT, UserRole.PROFESSIONAL), UserRole.PROFESSIONAL)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.auth_account_open_inbox)))
        composeRule.onNodeWithText(string(R.string.auth_account_immediate_care_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.auth_account_schedule_title)).assertDoesNotExist()
    }

    @Test
    fun anAdministratorSeesTheReviewEntryAndNothingElse() {
        show(setOf(UserRole.ADMIN), UserRole.ADMIN)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.auth_account_open_document_review)),
        )
        composeRule.onNodeWithText(string(R.string.auth_account_open_profile)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.auth_account_open_verification)).assertDoesNotExist()
    }
}
