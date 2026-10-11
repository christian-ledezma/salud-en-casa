package bo.saludencasa.features.catalog.presentation

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.catalog.declaredServices
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.catalog.professionalService
import bo.saludencasa.features.catalog.venousLine
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyServicesScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // "Colocación y control de vía venosa" is the longest name of the twelve
    // catalog entries, so it is the one that squeezes the actions beside it.
    private val declaredVenousLine = professionalService(type = venousLine, price = "140.00")

    // Each fixture leaves exactly one card of each kind in the tree, because
    // "Declarar" and "Quitar" repeat per card and onNodeWithText refuses to
    // choose between two matches.
    private fun show(
        services: List<ProfessionalService> = listOf(declaredVenousLine),
        catalog: List<ServiceType> = listOf(venousLine, physiotherapy),
        editor: ServiceEditor? = null,
    ) {
        composeRule.setContentAtDoubleFontScale {
            MyServicesContent(
                uiState =
                    MyServicesUiState.Content(
                        declaredServices = declaredServices(services = services, catalog = catalog),
                        editor = editor,
                        busy = false,
                        notice = null,
                    ),
                onBack = {},
                onRetryClick = {},
                onDeclareClick = {},
                onRepriceClick = {},
                onRemoveClick = {},
                onPriceChanged = {},
                onConfirmPrice = {},
                onConfirmRemoval = {},
                onDismissEditor = {},
            )
        }
    }

    // A LazyColumn composes nothing below the fold, so a card the header pushed
    // out of sight is absent from the semantics tree and cannot be reached by
    // its text. The list is moved to the card's index first.
    private fun scrollTo(index: Int) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
    }

    // Header, section title, then the only declared service.
    @Test
    fun bothActionsOfADeclaredServiceStayOnScreen() {
        show()
        scrollTo(2)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(venousLine.name))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.catalog_reprice_action)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.catalog_remove_action)),
        )
    }

    // Header, declared title, the declared card, the second section title, then
    // the one entry left to declare.
    @Test
    fun theCatalogEntryLeftToDeclareKeepsItsActionOnScreen() {
        show()
        scrollTo(4)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(physiotherapy.name))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.catalog_declare_action)),
        )
    }

    // With nothing declared the empty block takes the card's place, and the
    // catalog has to stay reachable: it is the only way out of that state.
    @Test
    fun withNothingDeclaredTheEmptyBlockAndTheCatalogAreBothReachable() {
        show(services = emptyList(), catalog = listOf(venousLine))
        scrollTo(2)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.catalog_declared_empty_title)),
        )

        scrollTo(4)
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.catalog_declare_action)),
        )
    }

    @Test
    fun theWayBackStaysOnScreen() {
        show()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }

    // The dialog is where the price is fixed, and an AlertDialog bounds its text
    // slot and clips the overflow instead of scrolling it. Nothing inside it
    // scrolls, so a slot that outgrows the dialog takes its refusal out of sight
    // and assertIsDisplayed is what notices.
    @Test
    fun thePriceDialogKeepsItsFieldItsRefusalAndBothActionsOnScreen() {
        show(
            editor =
                ServiceEditor.Declaring(
                    venousLine,
                    price = "0",
                    error = CatalogError.InvalidPrice,
                ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.catalog_price_label)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.error_catalog_invalid_price)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.catalog_price_confirm)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.common_cancel)),
        )
    }

    // The list is left empty on purpose: the confirmation button carries the
    // same label as the card action, and with a card in the tree there would be
    // two matches for it.
    @Test
    fun theRemovalConfirmationKeepsItsQuestionAndBothActionsOnScreen() {
        show(services = emptyList(), editor = ServiceEditor.Removing(declaredVenousLine))

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.catalog_remove_dialog_title)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(
                string(R.string.catalog_remove_dialog_message, venousLine.name),
            ),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.catalog_remove_action)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.common_cancel)),
        )
    }
}
