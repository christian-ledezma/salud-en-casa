package bo.saludencasa.features.verification.presentation

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossier
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.PendingReviewQuery
import bo.saludencasa.features.verification.domain.model.ReviewQueueOrder
import bo.saludencasa.features.verification.domain.model.ReviewRoleFilter
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationChecklist
import bo.saludencasa.features.verification.everyRequiredNurseDocument
import bo.saludencasa.features.verification.pendingSubject
import bo.saludencasa.features.verification.reviewSubject
import bo.saludencasa.features.verification.verificationDocument
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.plural
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

    private fun showDetail(
        approved: Collection<DocumentType> = emptyList(),
        checked: Set<DocumentType> = emptySet(),
    ) {
        composeRule.setContentAtDoubleFontScale {
            DocumentReviewContent(
                uiState =
                    DocumentReviewUiState.Content(
                        dossier = dossier(approved),
                        selectedType = DocumentType.LICENSE,
                        checkedTypes = checked,
                        image = DocumentImageState.Missing,
                        busy = false,
                        expandedImage = false,
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

    private fun showQueue(query: PendingReviewQuery = PendingReviewQuery()) {
        composeRule.setContentAtDoubleFontScale {
            DocumentReviewQueueContent(
                uiState =
                    DocumentReviewQueueUiState(
                        query = query,
                        list =
                            QueueListState.Content(
                                subjects = listOf(pendingSubject(fullName = LONG_NAME, pendingCount = 5)),
                                loadingMore = false,
                                endReached = true,
                                notice = null,
                            ),
                    ),
                onBack = {},
                onRetry = {},
                onReachedEnd = {},
                onOpenReview = {},
                criteria = QueueCriteriaActions(),
            )
        }
    }

    // A LazyColumn composes nothing below the fold, so a card the header and the
    // criteria pushed out of sight is absent from the semantics tree and cannot
    // be scrolled to by its text. The list is moved to the card's index first:
    // header, criteria, then the only person.
    private fun scrollQueueToTheFirstCard() {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
    }

    @Test
    fun aLongNameInTheQueueKeepsItsPendingCountOnScreen() {
        showQueue()
        scrollQueueToTheFirstCard()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(LONG_NAME))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(plural(R.plurals.review_queue_pending_count, 5, 5)),
        )
    }

    // Five chips and a text field above the list: the row that wraps worst on a
    // narrow screen, and the one the administrator needs to narrow the pile.
    @Test
    fun everyQueueCriterionStaysOnScreen() {
        showQueue(PendingReviewQuery(search = "ana", role = ReviewRoleFilter.PROFESSIONAL))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.review_queue_search_label)),
        )
        ReviewRoleFilter.entries.forEach { role ->
            composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(role.labelRes())))
        }
        ReviewQueueOrder.entries.forEach { order ->
            composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(order.labelRes())))
        }
    }

    @Test
    fun theQueueKeepsItsWayBackOnScreen() {
        showQueue()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }

    @Test
    fun bothSelectionActionsStayOnScreen() {
        showDetail(checked = setOf(DocumentType.LICENSE))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.review_action_approve_selected)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.review_action_reject_selected)),
        )
    }

    @Test
    fun theSelectAllRowAndTheCountStayOnScreen() {
        showDetail(checked = setOf(DocumentType.LICENSE, DocumentType.DEGREE))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.review_select_all)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(plural(R.plurals.review_selection_count, 2, 2, 5)),
        )
    }

    // Reachable means reachable with a finger: the checkbox of the last document
    // is the control furthest down the list of five.
    @Test
    fun theCheckboxOfEveryDocumentStaysOnScreen() {
        showDetail()

        everyRequiredNurseDocument.forEach { type ->
            composeRule.assertFitsTheScreen(
                composeRule.onNodeWithContentDescription(
                    string(R.string.cd_select_document, string(type.titleRes())),
                ),
            )
        }
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
