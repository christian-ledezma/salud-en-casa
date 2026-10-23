package bo.saludencasa.features.requests.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.location.address
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.serviceRequest
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalTime
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ImmediateCareScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The longest of the twelve catalog entries, and a short one to measure it by.
    private val venousLine =
        ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60)
    private val vaccination = ServiceType("t2", "Vacunación", null, BigDecimal("90.00"), 20)

    private val home = address(id = "a1", alias = "Mi casa", isPrimary = true)
    private val mother = address(id = "a2", alias = "Casa de mi mamá", isPrimary = false)

    private fun form(
        nearby: NearbyState = NearbyState.Found(emptyList(), more = false),
        selectedServiceTypeId: String? = venousLine.id,
        typeListExpanded: Boolean = false,
    ) = ImmediateCareUiState.Form(
        serviceTypes = listOf(venousLine, vaccination),
        addresses = listOf(home, mother),
        selectedServiceTypeId = selectedServiceTypeId,
        selectedAddressId = home.id,
        typeListExpanded = typeListExpanded,
        description = "",
        budget = "",
        nearby = nearby,
        selectedProfessionalId = null,
        submitting = false,
        submitError = null,
    )

    private fun show(uiState: ImmediateCareUiState) {
        composeRule.setContentAtDoubleFontScale {
            ImmediateCareContent(
                uiState = uiState,
                onBack = {},
                actions = ImmediateCareActions(),
                map = { _, modifier -> Box(modifier = modifier.testTag(MAP_TAG)) },
            )
        }
    }

    @Test
    fun theWayBackAndTheMapStayOnScreen() {
        show(form())

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithTag(MAP_TAG))
    }

    // The button is fixed to the foot, outside the content that scrolls: it has
    // to be there without scrolling, whatever the font did to the form above.
    @Test
    fun askingForCareIsAlwaysWithinReach() {
        show(form())

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.immediate_submit)),
        )
    }

    // HT-23: nobody nearby says so, and the button stays enabled.
    @Test
    fun nobodyNearbyIsSaidAndStillLetsThePatientAsk() {
        show(form(nearby = NearbyState.Found(emptyList(), more = false)))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.immediate_nearby_none)))
        composeRule.onNodeWithText(string(R.string.immediate_submit)).assertIsEnabled()
    }

    @Test
    fun everyAddressOptionStaysOnScreen() {
        show(form())

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(home.alias))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(mother.alias))
    }

    // Open, the longest name of the catalog wraps instead of hiding its tail:
    // its row is taller than the row of a short name (docs/decisions.md,
    // 2026-10-09, the relative measure).
    @Test
    fun theLongestServiceTypeWrapsWhenTheListIsOpen() {
        show(form(selectedServiceTypeId = null, typeListExpanded = true))

        val longRow = composeRule.onNodeWithText(venousLine.name)
        composeRule.assertFitsTheScreen(longRow)
        val shortBounds = composeRule.onNodeWithText(vaccination.name).getUnclippedBoundsInRoot()
        val longBounds = longRow.getUnclippedBoundsInRoot()

        assertTrue(
            "Both rows are the same height, so the long name did not wrap and its tail is hidden.",
            longBounds.bottom - longBounds.top > shortBounds.bottom - shortBounds.top,
        )
    }

    @Test
    fun theWaitKeepsItsDeadlineAndBothWaysOutOnScreen() {
        show(ImmediateCareUiState.Waiting(serviceRequest(serviceTypeName = venousLine.name), false, null))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.immediate_waiting_title)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(venousLine.name))
        composeRule.assertFitsTheScreen(
            // The fixture expires at 13:00 UTC, which is 09:00 in La Paz.
            composeRule.onNodeWithText(
                string(R.string.immediate_waiting_expires, formatTime(LocalTime.of(9, 0), Locale.getDefault())),
            ),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.request_list_cancel_action)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.immediate_waiting_my_requests)))
    }

    @Test
    fun aFailedCancellationSaysWhyNextToTheButton() {
        show(
            ImmediateCareUiState.Waiting(
                serviceRequest(),
                cancelling = false,
                cancelError = RequestError.NetworkUnavailable,
            ),
        )

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_network_unavailable)))
    }

    // The patient who arrives here without an address has to be able to leave
    // for the screen that registers one.
    @Test
    fun theNoAddressWayOutStaysOnScreen() {
        show(ImmediateCareUiState.NoAddress)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_create_no_address_action)),
        )
    }

    @Test
    fun aFailedLoadKeepsItsRetryOnScreen() {
        show(ImmediateCareUiState.Failed(RequestError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    private companion object {
        const val MAP_TAG = "immediate_map"
    }
}
