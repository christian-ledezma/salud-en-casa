package bo.saludencasa.features.availability.presentation

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
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

@RunWith(AndroidJUnit4::class)
class AvailabilityScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(uiState: AvailabilityUiState) {
        composeRule.setContentAtDoubleFontScale {
            AvailabilityContent(
                uiState = uiState,
                onBack = {},
                onRetry = {},
                onAddClick = {},
                onRemoveClick = {},
                onDraftDay = {},
                onDraftStart = {},
                onDraftEnd = {},
                onConfirmAdd = {},
                onConfirmRemove = {},
                onDismissDialog = {},
            )
        }
    }

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

    // The slot row carries two formatted times side by side. At 200% the whole
    // row has to stay on screen without the two times overflowing their row.
    @Test
    fun aSlotRowStaysOnScreen() {
        show(
            AvailabilityUiState.Content(
                WeeklyAvailability.from(
                    listOf(
                        AvailabilitySlot(
                            id = "1",
                            dayOfWeek = DayOfWeek.MONDAY,
                            startTime = LocalTime.of(9, 0),
                            endTime = LocalTime.of(10, 30),
                        ),
                    ),
                ),
            ),
        )
        // The row is at index 2: the empty-hint is absent, [header Monday][slot][add Monday].
        scrollTo(1)

        composeRule.assertFitsTheScreen(
            composeRule.onAllNodesWithText(string(R.string.availability_add_slot))[0],
        )
    }
}
