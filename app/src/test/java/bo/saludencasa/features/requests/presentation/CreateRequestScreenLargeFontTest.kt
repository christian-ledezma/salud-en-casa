package bo.saludencasa.features.requests.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class CreateRequestScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The longest of the twelve catalog entries: if any name decides whether a
    // row fits a 320 dp screen at a 200 % font scale, it is this one.
    private val venousLine =
        ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60)

    private val home =
        Address(
            id = "a1",
            alias = "Mi casa",
            addressText = "Avenida América 1234, Cochabamba",
            reference = null,
            city = "Cochabamba",
            coordinate = Coordinate.create(-17.3835, -66.1568).getOrThrow(),
            isPrimary = true,
            isProfessionalBase = false,
        )

    private fun showForm() {
        composeRule.setContentAtDoubleFontScale {
            CreateRequestContent(
                uiState =
                    CreateRequestUiState.Content(
                        serviceTypes = listOf(venousLine),
                        addresses = listOf(home),
                        selectedServiceTypeId = venousLine.id,
                        selectedAddressId = home.id,
                        description = "",
                        budget = "",
                        submitting = false,
                        submitError = null,
                    ),
                onBack = {},
                actions = CreateRequestActions(),
            )
        }
    }

    @Test
    fun theWayBackStaysOnScreen() {
        showForm()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }

    @Test
    fun theLongestServiceTypeOptionStaysOnScreen() {
        showForm()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(venousLine.name))
    }

    @Test
    fun theAddressOptionStaysOnScreen() {
        showForm()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(home.alias))
    }

    @Test
    fun theSubmitButtonStaysOnScreen() {
        showForm()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_create_submit)),
        )
    }

    // The patient who arrives here without an address has to be able to leave
    // for the screen that registers one.
    @Test
    fun theNoAddressWayOutStaysOnScreen() {
        composeRule.setContentAtDoubleFontScale {
            CreateRequestContent(
                uiState = CreateRequestUiState.NoAddress,
                onBack = {},
                actions = CreateRequestActions(),
            )
        }

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.request_create_no_address_action)),
        )
    }

    @Test
    fun aFailedLoadKeepsItsRetryOnScreen() {
        composeRule.setContentAtDoubleFontScale {
            CreateRequestContent(
                uiState = CreateRequestUiState.Failed(RequestError.NetworkUnavailable),
                onBack = {},
                actions = CreateRequestActions(),
            )
        }

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }
}
