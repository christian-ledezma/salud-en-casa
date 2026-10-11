package bo.saludencasa.features.verification.presentation

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationChecklist
import bo.saludencasa.features.verification.verificationDocument
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VerificationScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun showPendingIdFront() {
        val document = verificationDocument(type = DocumentType.ID_FRONT, status = ReviewStatus.PENDING)
        val checklist = VerificationChecklist.create(setOf(UserRole.PATIENT), null, listOf(document))
        composeRule.setContentAtDoubleFontScale {
            VerificationContent(
                uiState =
                    VerificationUiState.Content(
                        checklist = checklist,
                        otherCaption = "",
                        pendingType = null,
                        stagedBytes = emptyMap(),
                        notice = null,
                    ),
                onBack = {},
                onRetryLoad = {},
                onCaptionChanged = {},
                onPickType = {},
                onDismissPicker = {},
                onCameraRequested = {},
                onGalleryRequested = {},
                onRetry = {},
                picking = null,
            )
        }
    }

    // The defect found on a device on 2026-10-05: beside the longest document title
    // the status chip was squeezed to one letter per line. A chip that reads on one
    // line is wider than it is tall; a stacked one is far taller than wide.
    @Test
    fun theStatusChipReadsOnOneLine() {
        showPendingIdFront()

        val bounds =
            composeRule
                .onNodeWithText(string(R.string.verification_status_pending))
                .getUnclippedBoundsInRoot()
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top

        assertTrue("The status chip is $width wide and $height tall, so it is stacked.", width > height)
    }

    // Reintroducing the Row that HU-07 replaced with a FlowRow makes this fail:
    // at 320 dp and a 200 % font scale the replace action leaves the screen. It is
    // the shape of the two Sprint 2.5 defects and of the HU-07 one.
    @Test
    fun theDocumentTitleAndItsActionStayOnScreen() {
        showPendingIdFront()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.verification_document_id_front_title)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.verification_action_replace)),
        )
    }

    // The checklist screen once had no way back but the system gesture.
    @Test
    fun theBackArrowStaysOnScreenBesideTheTitle() {
        showPendingIdFront()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }
}
