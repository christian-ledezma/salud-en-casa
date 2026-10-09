package bo.saludencasa.features.verification.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossier
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationChecklist
import bo.saludencasa.features.verification.everyRequiredNurseDocument
import bo.saludencasa.features.verification.pendingReview
import bo.saludencasa.features.verification.reviewSubject
import bo.saludencasa.features.verification.verificationDocument
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val LONG_NAME = "Maria del Carmen Condori de la Torre"

@RunWith(AndroidJUnit4::class)
class DocumentReviewScreensLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun dossier(approved: Collection<DocumentType>): DocumentReviewDossier {
        val subject = reviewSubject()
        val documents =
            everyRequiredNurseDocument.map { type ->
                val status = if (type in approved) ReviewStatus.APPROVED else ReviewStatus.PENDING
                verificationDocument(type = type, status = status)
            }
        return DocumentReviewDossier(
            subject = subject.copy(fullName = LONG_NAME),
            checklist = VerificationChecklist.create(subject.heldRoles, subject.professionalType, documents),
        )
    }

    private fun showDetail(approved: Collection<DocumentType> = emptyList()) {
        composeRule.setContentAtDoubleFontScale {
            DocumentReviewContent(
                uiState =
                    DocumentReviewUiState.Content(
                        dossier = dossier(approved),
                        selectedType = DocumentType.LICENSE,
                        image = DocumentImageState.Missing,
                        busy = false,
                        rejectionDraft = null,
                        confirmingProfessionalApproval = false,
                        notice = null,
                    ),
                onBack = {},
                onRetryLoad = {},
                actions = ReviewActions(),
            )
        }
    }

    @Test
    fun aLongNameInTheQueueKeepsItsDocumentTitleOnScreen() {
        composeRule.setContentAtDoubleFontScale {
            DocumentReviewQueueContent(
                uiState =
                    DocumentReviewQueueUiState.Content(
                        reviews = listOf(pendingReview(type = DocumentType.STUDENT_CARD, fullName = LONG_NAME)),
                        loadingMore = false,
                        endReached = true,
                        notice = null,
                    ),
                onBack = {},
                onRetry = {},
                onReachedEnd = {},
                onOpenReview = {},
            )
        }

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(LONG_NAME))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.verification_document_student_card_title)),
        )
    }

    @Test
    fun bothVerdictActionsStayOnScreen() {
        showDetail()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.review_action_approve)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.review_action_reject)),
        )
    }

    // The action that closes the story: it only appears once every required
    // document is approved, and it has the longest label of the screen.
    @Test
    fun theProfessionalApprovalStaysOnScreenOnceItIsOffered() {
        showDetail(approved = everyRequiredNurseDocument)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.review_action_approve_professional)),
        )
    }
}
