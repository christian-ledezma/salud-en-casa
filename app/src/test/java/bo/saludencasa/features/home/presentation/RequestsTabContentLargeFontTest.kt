package bo.saludencasa.features.home.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestsTabContentLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var view by mutableStateOf(RequestsView.REQUESTS)

    private fun show(requestsLabel: Int = R.string.home_requests_view_available) {
        composeRule.setContentAtDoubleFontScale {
            RequestsTabContent(
                view = view,
                onViewChange = { view = it },
                requestsLabel = string(requestsLabel),
                requests = { Text("bandeja") },
                myAttentions = { Text("atenciones") },
            )
        }
    }

    private fun layoutOf(label: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(label, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)
        return layouts.single()
    }

    // The first layout defect of this project was a label of this control cut
    // inside its own segment, which no bounds check can see (docs/decisions.md,
    // 2026-10-02): only the laid-out text says so.
    @Test
    fun neitherLabelOfTheProfessionalIsCutAtDoubleFontSize() {
        show()

        assertNotCut(R.string.home_requests_view_available)
        assertNotCut(R.string.home_requests_view_my_attentions)
    }

    // «Mis solicitudes» beside «Mis atenciones» is the longest pair of the control.
    @Test
    fun neitherLabelOfThePatientIsCutAtDoubleFontSize() {
        show(requestsLabel = R.string.home_requests_view_my_requests)

        assertNotCut(R.string.home_requests_view_my_requests)
        assertNotCut(R.string.home_requests_view_my_attentions)
    }

    private fun assertNotCut(label: Int) {
        val layout = layoutOf(string(label))
        assertEquals("${string(label)} wrapped.", 1, layout.lineCount)
        assertFalse("${string(label)} ends in an ellipsis.", layout.isLineEllipsized(0))
    }

    // The view a professional opens from «Ver todas» of Inicio is the inbox.
    @Test
    fun theControlMovesBetweenTheInboxAndTheAttentions() {
        show()
        composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText("bandeja"))

        composeRule.onNodeWithText(string(R.string.home_requests_view_my_attentions)).performClick()

        composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText("atenciones"))
        composeRule.onNodeWithText("bandeja").assertDoesNotExist()
    }
}
