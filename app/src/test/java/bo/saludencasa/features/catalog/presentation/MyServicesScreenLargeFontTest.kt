package bo.saludencasa.features.catalog.presentation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.catalog.declaredServices
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.catalog.professionalService
import bo.saludencasa.features.catalog.serviceType
import bo.saludencasa.features.catalog.venousLine
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.plural
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyServicesScreenLargeFontTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // "Colocación y control de vía venosa" is the longest name of the twelve
    // catalog entries, so it is the one that squeezes the price beside it.
    private val declaredVenousLine = professionalService(type = venousLine, price = "140.00")

    private fun priceOf(service: ProfessionalService): String =
        formatBob(
            service.referencePriceBob,
            ApplicationProvider
                .getApplicationContext<Context>()
                .resources.configuration.locales[0],
        )

    private fun content(
        services: List<ProfessionalService> = listOf(declaredVenousLine),
        catalog: List<ServiceType> = listOf(venousLine, physiotherapy),
        editor: ServiceEditor? = null,
        picker: ServicePicker? = null,
        query: String = "",
        newTypeIds: Set<String> = emptySet(),
        declarationNotice: Int? = null,
    ): MyServicesUiState.Content =
        MyServicesUiState.Content(
            declaredServices = declaredServices(services = services, catalog = catalog),
            editor = editor,
            picker = picker,
            query = query,
            newTypeIds = newTypeIds,
            declarationNotice = declarationNotice,
        )

    private fun show(
        uiState: MyServicesUiState = content(),
        actions: MyServicesActions = MyServicesActions(),
    ) {
        composeRule.setContentAtDoubleFontScale {
            MyServicesContent(uiState = uiState, actions = actions)
        }
    }

    // A LazyColumn composes nothing below the fold, so a row the header pushed
    // out of sight is absent from the semantics tree and cannot be reached by
    // its text. The list is moved to the row's index first.
    private fun scrollTo(index: Int) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
    }

    // ---- the list -----------------------------------------------------------

    // Header, summary, then the only declared service.
    @Test
    fun theNameTheDurationAndThePriceOfADeclaredServiceStayOnScreen() {
        show()
        scrollTo(2)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(venousLine.name))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.catalog_duration_hours, 1)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(
                string(R.string.cd_catalog_change_price, venousLine.name, priceOf(declaredVenousLine)),
            ),
        )
    }

    // The add button is outside the list on purpose: with fifty rows it must
    // not sit under the last one.
    @Test
    fun theAddButtonStaysOnScreenWithoutScrolling() {
        show()

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.catalog_add_services_action)),
        )
    }

    @Test
    fun anOpenRowKeepsItsDescriptionAndItsRemoveActionOnScreen() {
        val service = professionalService(type = venousLine.copy(description = "Canalización de vía periférica."))
        show(content(services = listOf(service)))
        scrollTo(2)

        composeRule.onNodeWithText(venousLine.name).performClick()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Canalización de vía periférica."))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.catalog_remove_service_action)))
    }

    // The mark sits in the line with the duration, which at this font and
    // width is also where the chevron goes: none of the three may push out
    // another.
    @Test
    fun theNewMarkStaysOnScreenBesideTheDuration() {
        show(content(newTypeIds = setOf(venousLine.id)))
        scrollTo(2)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.catalog_new_badge)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.catalog_duration_hours, 1)))
    }

    // The notice that asks the professional to review the prices has to reach
    // the screen and then be consumed, or it would come back every time the
    // list is composed again.
    @Test
    fun theNoticeOfWhatWasDeclaredShowsAndIsReportedAsShown() {
        var shown = 0
        show(
            uiState = content(newTypeIds = setOf(venousLine.id), declarationNotice = 2),
            actions = MyServicesActions(onDeclarationNoticeShown = { shown++ }),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(plural(R.plurals.catalog_declared_notice, 2, 2)),
        )
        composeRule.waitUntil(timeoutMillis = 10_000) { shown == 1 }
    }

    // With nothing declared the empty block takes the rows' place and the way
    // out of that state, the add button, stays within reach.
    @Test
    fun withNothingDeclaredTheEmptyBlockAndTheAddButtonAreBothReachable() {
        show(content(services = emptyList(), catalog = listOf(venousLine)))
        scrollTo(2)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.catalog_declared_empty_title)))
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.catalog_add_services_action)),
        )
    }

    // Past eight declared services the list grows its own search. Header,
    // summary, then the field.
    @Test
    fun withMoreThanEightDeclaredTheSearchFieldIsReachable() {
        val types = (1..9).map { serviceType(id = "type-$it", name = "Servicio $it") }
        show(
            content(
                services = types.mapIndexed { index, type -> professionalService(id = "declared-$index", type = type) },
                catalog = types,
            ),
        )
        scrollTo(2)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.catalog_search_declared_placeholder)),
        )
    }

    @Test
    fun theWayBackStaysOnScreen() {
        show()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)))
    }

    // ---- the selector -------------------------------------------------------

    private val twoOffered = listOf(generalConsultation, physiotherapy)

    private fun pickerContent(picker: ServicePicker = ServicePicker()): MyServicesUiState.Content =
        content(services = emptyList(), catalog = twoOffered, picker = picker)

    // Header, the search field that stays at the top, the summary, then rows.
    @Test
    fun everyRowOfTheSelectorAndItsSearchStayOnScreen() {
        show(pickerContent())
        scrollTo(1)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.catalog_search_catalog_placeholder)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_catalog_picker_close)),
        )

        scrollTo(3)
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(generalConsultation.name))
        scrollTo(4)
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(physiotherapy.name))
    }

    @Test
    fun theSelectorTellsWhatIsLeftAndHowTheyAreDeclared() {
        show(pickerContent())
        scrollTo(2)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(plural(R.plurals.catalog_picker_count, 2, 2)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.catalog_picker_price_note)),
        )
    }

    // The confirm button is outside the list, with the choice counted in its
    // own label: it must be within reach whatever the list holds.
    @Test
    fun theConfirmButtonStaysOnScreenAndIsDisabledUntilSomethingIsChosen() {
        show(pickerContent())

        val none = composeRule.onNodeWithText(string(R.string.catalog_picker_confirm_none))
        composeRule.assertFitsTheScreenWithoutScrolling(none)
        none.assertIsNotEnabled()
    }

    @Test
    fun theConfirmButtonCountsTheChosenServicesAndIsEnabled() {
        show(pickerContent(ServicePicker(selected = setOf(generalConsultation.id, physiotherapy.id))))

        val confirm = composeRule.onNodeWithText(plural(R.plurals.catalog_picker_confirm, 2, 2))
        composeRule.assertFitsTheScreenWithoutScrolling(confirm)
        confirm.assertIsEnabled()
    }

    // The refusal sits in the bar with the button, so it arrives in sight
    // without a gesture: under the rows it would be below the fold at this font.
    @Test
    fun aRefusalOfTheSelectorStaysOnScreenAboveTheConfirmButton() {
        show(
            pickerContent(
                ServicePicker(selected = setOf(physiotherapy.id), error = CatalogError.ServiceAlreadyDeclared),
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.error_catalog_service_already_declared)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(plural(R.plurals.catalog_picker_confirm, 1, 1)),
        )
    }

    @Test
    fun aSearchWithoutResultsSaysSoInsteadOfLeavingTheListEmpty() {
        show(pickerContent(ServicePicker(query = "dentista")))
        scrollTo(3)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.catalog_picker_no_results, "dentista")),
        )
    }

    // The system back gesture closes what is on top. With the selector open
    // that is the selector: leaving «Mis servicios» would throw away what the
    // person had chosen.
    @Test
    fun theBackGestureClosesTheSelectorAndDoesNotLeaveTheScreen() {
        var leftTheScreen = 0
        var closedTheSelector = 0
        show(
            uiState = pickerContent(),
            actions = MyServicesActions(onBack = { leftTheScreen++ }, onPickerDismiss = { closedTheSelector++ }),
        )

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(1, closedTheSelector)
        assertEquals(0, leftTheScreen)
    }

    // ---- the dialogs --------------------------------------------------------

    // The dialog is where the price is fixed, and an AlertDialog bounds its text
    // slot and clips the overflow instead of scrolling it. So the slot scrolls, and
    // a refusal has to arrive in sight without a gesture.
    @Test
    fun thePriceDialogKeepsItsFieldItsRefusalAndBothActionsOnScreen() {
        show(
            content(
                editor =
                    ServiceEditor.Repricing(
                        declaredVenousLine,
                        price = "0",
                        error = CatalogError.InvalidPrice,
                    ),
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.error_catalog_invalid_price)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.catalog_price_label)))
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.catalog_price_confirm)),
        )
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.common_cancel)),
        )
    }

    // The list is left empty on purpose: with a row in the tree the remove
    // button of the row and the one of the dialog would be easy to confuse.
    @Test
    fun theRemovalConfirmationKeepsItsQuestionAndBothActionsOnScreen() {
        show(content(services = emptyList(), editor = ServiceEditor.Removing(declaredVenousLine)))

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
