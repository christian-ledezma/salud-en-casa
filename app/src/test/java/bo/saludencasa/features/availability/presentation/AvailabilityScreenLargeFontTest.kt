package bo.saludencasa.features.availability.presentation

import android.content.Context
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import bo.saludencasa.features.availability.domain.model.AvailabilitySlot
import bo.saludencasa.features.availability.domain.model.WeeklyAvailability
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class AvailabilityScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The locale the screen formats the hours with, so the expected labels
    // match whatever Robolectric configures.
    private val locale: Locale by lazy {
        ApplicationProvider
            .getApplicationContext<Context>()
            .resources.configuration.locales[0]
    }

    private val mondayMorning =
        AvailabilitySlot(
            id = "1",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
        )

    private fun show(uiState: AvailabilityUiState) {
        composeRule.setContentAtDoubleFontScale {
            AvailabilityContent(
                uiState = uiState,
                onBack = {},
                onRetry = {},
                onAddClick = {},
                onRemoveClick = {},
                onDraftStart = {},
                onDraftEnd = {},
                onConfirmAdd = {},
                onCancelDraft = {},
                onConfirmRemove = {},
                onDismissRemoval = {},
            )
        }
    }

    private fun editing(): AvailabilityUiState.Content =
        AvailabilityUiState.Content(
            availability = WeeklyAvailability.from(listOf(mondayMorning)),
            draft =
                SlotDraft(
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(10, 0),
                    error = AvailabilityError.OverlapsExistingSlot,
                ),
        )

    private fun scrollTo(index: Int) {
        composeRule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(index)
    }

    @Test
    fun theWayBackStaysOnScreen() {
        show(AvailabilityUiState.Content(WeeklyAvailability.Empty))

        // The header sits outside the LazyColumn, so performScrollTo cannot
        // climb to a scroll parent — the back button is already in the visible
        // region.
        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }

    @Test
    fun theEmptyHintTitleStaysOnScreen() {
        show(AvailabilityUiState.Content(WeeklyAvailability.Empty))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.availability_empty_title)),
        )
    }

    // The application is in Spanish whatever the language of the phone, and
    // Robolectric runs in English: a day name taken from the platform calendar
    // would read "Monday" here.
    @Test
    fun theDayNameIsTheSpanishOneFromTheResources() {
        show(AvailabilityUiState.Content(WeeklyAvailability.from(listOf(mondayMorning))))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.availability_day_monday)),
        )
    }

    // The slot row carries two formatted times side by side. At 200% the whole
    // row has to stay on screen without the two times overflowing their row.
    @Test
    fun aSlotRowStaysOnScreen() {
        show(AvailabilityUiState.Content(WeeklyAvailability.from(listOf(mondayMorning))))
        // [header Monday][slot][add Monday]: the empty hint is absent.
        scrollTo(1)

        composeRule.assertFitsTheScreen(
            composeRule.onAllNodesWithText(string(R.string.availability_add_slot))[0],
        )
    }

    @Test
    fun theTwoLabelsOfTheEditorStayOnScreen() {
        show(editing())
        scrollTo(EDITOR)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.availability_start_label)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.availability_end_label)),
        )
    }

    // The hour the editor opens on is the one it scrolled its row to, so it has
    // to be reachable and inside the screen at 200%.
    @Test
    fun theChosenHourAndItsPartOfTheDayStayOnScreen() {
        show(editing())
        scrollTo(EDITOR)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.common_part_morning)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onAllNodesWithText(formatTime(LocalTime.of(9, 0), locale))[0],
        )
    }

    @Test
    fun bothActionsOfTheEditorStayOnScreen() {
        show(editing())
        scrollTo(EDITOR)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.availability_add_confirm)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.common_cancel)),
        )
    }

    // The overlap the engine reports is the one error the professional sees
    // most, and it is the longest text in the editor.
    @Test
    fun theOverlapErrorStaysOnScreen() {
        show(editing())
        scrollTo(EDITOR)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.availability_error_overlap)),
        )
    }

    @Test
    fun theRemovalDialogStaysOnScreen() {
        show(
            AvailabilityUiState.Content(
                availability = WeeklyAvailability.from(listOf(mondayMorning)),
                removal = mondayMorning,
            ),
        )

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.availability_remove_action)),
        )
    }
}

// [header Monday][slot][editor]: the empty hint is absent while Monday has a slot.
private const val EDITOR = 2
