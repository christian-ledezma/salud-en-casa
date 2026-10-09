@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.verification.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.verification.FakeDocumentReviewRepository
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.ApproveDocumentsUseCase
import bo.saludencasa.features.verification.domain.usecase.ApproveProfessionalVerificationUseCase
import bo.saludencasa.features.verification.domain.usecase.GetDocumentReviewDossierUseCase
import bo.saludencasa.features.verification.domain.usecase.GetSignedDocumentUrlUseCase
import bo.saludencasa.features.verification.domain.usecase.RejectDocumentsUseCase
import bo.saludencasa.features.verification.everyRequiredNurseDocument
import bo.saludencasa.features.verification.verificationDocument
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DocumentReviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDocumentReviewRepository()

    // Everything approved but the license, which is the one under review.
    private fun onlyTheLicenseIsLeft() {
        repository.documents =
            everyRequiredNurseDocument.map {
                val status = if (it == DocumentType.LICENSE) ReviewStatus.PENDING else ReviewStatus.APPROVED
                verificationDocument(type = it, status = status)
            }
    }

    private fun TestScope.opened(): DocumentReviewViewModel {
        val viewModel =
            DocumentReviewViewModel(
                profileId = "subject-id",
                getDossier = GetDocumentReviewDossierUseCase(repository),
                getSignedUrl = GetSignedDocumentUrlUseCase(repository),
                approveDocuments = ApproveDocumentsUseCase(repository),
                rejectDocuments = RejectDocumentsUseCase(repository),
                approveProfessional = ApproveProfessionalVerificationUseCase(repository),
            )
        advanceUntilIdle()
        return viewModel
    }

    private fun DocumentReviewViewModel.content() = uiState.value as DocumentReviewUiState.Content

    private fun DocumentReviewViewModel.approve(vararg types: DocumentType) {
        types.forEach { onToggleChecked(it) }
        onApproveSelected()
    }

    // The queue card names a person, so the screen has to pick the document
    // itself, and the one worth opening is the first one nobody answered yet.
    @Test
    fun theViewerOpensOnTheFirstDocumentStillPending() =
        runTest {
            repository.documents =
                everyRequiredNurseDocument.map {
                    val approved = it == DocumentType.ID_FRONT
                    verificationDocument(
                        type = it,
                        status = if (approved) ReviewStatus.APPROVED else ReviewStatus.PENDING,
                    )
                }

            val viewModel = opened()

            assertEquals(DocumentType.ID_BACK, viewModel.content().selectedType)
            assertEquals(listOf("x/ID_BACK.jpg"), repository.urlRequests)
        }

    @Test
    fun approvingTheLastRequiredDocumentOffersTheProfessionalApproval() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()
            assertFalse(viewModel.content().dossier.canApproveProfessional)

            viewModel.approve(DocumentType.LICENSE)
            advanceUntilIdle()

            assertTrue(viewModel.content().dossier.canApproveProfessional)
        }

    // One statement for the whole pile, because the engine is what makes the
    // verdict atomic; a loop of single writes can stop halfway.
    @Test
    fun theWholeSelectionTravelsInOneVerdict() =
        runTest {
            repository.documents =
                everyRequiredNurseDocument.map { verificationDocument(type = it, status = ReviewStatus.PENDING) }
            val viewModel = opened()

            viewModel.approve(DocumentType.ID_FRONT, DocumentType.ID_BACK, DocumentType.SELFIE)
            advanceUntilIdle()

            assertEquals(1, repository.verdicts.size)
            assertEquals(
                setOf(DocumentType.ID_FRONT, DocumentType.ID_BACK, DocumentType.SELFIE),
                repository.verdicts.single().types,
            )
        }

    // A slot the person never filled has no row to update. Sending it anyway
    // would make the engine write fewer documents than were asked for, which the
    // repository reports as a refusal, so a good verdict would look denied.
    @Test
    fun markingAllSelectsOnlyTheDocumentsThatExist() =
        runTest {
            repository.documents =
                listOf(
                    verificationDocument(type = DocumentType.ID_FRONT, status = ReviewStatus.PENDING),
                    verificationDocument(type = DocumentType.DEGREE, status = ReviewStatus.PENDING),
                )
            val viewModel = opened()

            viewModel.onToggleAllChecked()

            assertEquals(setOf(DocumentType.ID_FRONT, DocumentType.DEGREE), viewModel.content().checkedTypes)
            assertTrue(viewModel.content().allChecked)
        }

    @Test
    fun aSecondTapOnSelectAllClearsTheSelection() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()

            viewModel.onToggleAllChecked()
            viewModel.onToggleAllChecked()

            assertTrue(viewModel.content().checkedTypes.isEmpty())
        }

    // The marks name documents that have just been decided. Keeping them would
    // let the next tap send a verdict on paperwork already settled.
    @Test
    fun aSuccessfulVerdictClearsTheMarks() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()

            viewModel.approve(DocumentType.LICENSE)
            advanceUntilIdle()

            assertTrue(viewModel.content().checkedTypes.isEmpty())
        }

    // Nothing was decided, so the selection is still what the admin meant to
    // decide: clearing it would make them mark everything again to retry.
    @Test
    fun aFailedVerdictKeepsTheMarks() =
        runTest {
            onlyTheLicenseIsLeft()
            repository.actionResult = ReviewActionResult.Failure(VerificationError.NetworkUnavailable)
            val viewModel = opened()

            viewModel.approve(DocumentType.LICENSE)
            advanceUntilIdle()

            assertEquals(setOf(DocumentType.LICENSE), viewModel.content().checkedTypes)
        }

    @Test
    fun nothingIsWrittenWithoutASelection() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()

            viewModel.onApproveSelected()
            viewModel.onRejectClick()
            advanceUntilIdle()

            assertTrue(repository.verdicts.isEmpty())
            assertNull(viewModel.content().rejectionDraft)
        }

    // The screen shows what the server reports after the write. Here the write
    // is accepted but nothing changes in storage, so a screen that flipped the
    // status itself would show APPROVED over a document that still is not.
    @Test
    fun aVerdictIsShownAsTheServerReportsItAndNotAsTheClientAssumed() =
        runTest {
            onlyTheLicenseIsLeft()
            repository.applyWrites = false
            val viewModel = opened()

            viewModel.approve(DocumentType.LICENSE)
            advanceUntilIdle()

            val license =
                viewModel
                    .content()
                    .dossier.checklist
                    .documentFor(DocumentType.LICENSE)
            assertEquals(ReviewStatus.PENDING, license?.status)
        }

    @Test
    fun aFailedVerdictLeavesTheDocumentInItsPreviousStatus() =
        runTest {
            onlyTheLicenseIsLeft()
            repository.actionResult = ReviewActionResult.Failure(VerificationError.NotAuthorized)
            val viewModel = opened()

            viewModel.approve(DocumentType.LICENSE)
            advanceUntilIdle()

            val content = viewModel.content()
            assertEquals(
                ReviewStatus.PENDING,
                content.dossier.checklist
                    .documentFor(DocumentType.LICENSE)
                    ?.status,
            )
            assertEquals(VerificationError.NotAuthorized, content.notice)
            assertFalse(content.busy)
        }

    @Test
    fun aFailedPromotionLeavesTheProfessionalUnverified() =
        runTest {
            repository.documents =
                everyRequiredNurseDocument.map { verificationDocument(type = it, status = ReviewStatus.APPROVED) }
            repository.actionResult = ReviewActionResult.Failure(VerificationError.RequiredDocumentsNotApproved)
            val viewModel = opened()
            assertTrue(viewModel.content().dossier.canApproveProfessional)

            viewModel.onApproveProfessionalClick()
            viewModel.onApproveProfessionalConfirm()
            advanceUntilIdle()

            val content = viewModel.content()
            assertEquals(ReviewStatus.PENDING, content.dossier.subject.professionalStatus)
            assertEquals(VerificationError.RequiredDocumentsNotApproved, content.notice)
            assertFalse(content.confirmingProfessionalApproval)
        }

    @Test
    fun aSuccessfulPromotionRetiresTheButton() =
        runTest {
            repository.documents =
                everyRequiredNurseDocument.map { verificationDocument(type = it, status = ReviewStatus.APPROVED) }
            val viewModel = opened()

            viewModel.onApproveProfessionalClick()
            viewModel.onApproveProfessionalConfirm()
            advanceUntilIdle()

            val content = viewModel.content()
            assertEquals(ReviewStatus.APPROVED, content.dossier.subject.professionalStatus)
            assertFalse(content.dossier.canApproveProfessional)
        }

    @Test
    fun aFailedSignedUrlStillShowsTheSubjectData() =
        runTest {
            onlyTheLicenseIsLeft()
            repository.urlResult = DocumentUrlResult.Failure(VerificationError.NetworkUnavailable)

            val viewModel = opened()

            val content = viewModel.content()
            assertEquals("Ernesto Arancibia", content.dossier.subject.fullName)
            assertEquals(DocumentImageState.Failed(VerificationError.NetworkUnavailable), content.image)
        }

    @Test
    fun aDocumentNeverUploadedHasNoImageToRequest() =
        runTest {
            repository.documents = emptyList()

            val viewModel = opened()

            assertEquals(DocumentImageState.Missing, viewModel.content().image)
            assertTrue(repository.urlRequests.isEmpty())
        }

    @Test
    fun selectingAnotherDocumentRequestsItsOwnSignedUrl() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()

            viewModel.onSelect(DocumentType.SELFIE)
            advanceUntilIdle()

            assertEquals(DocumentType.SELFIE, viewModel.content().selectedType)
            assertEquals(listOf("x/LICENSE.jpg", "x/SELFIE.jpg"), repository.urlRequests)
        }

    @Test
    fun aRejectionWithABlankReasonKeepsTheDialogOpenAndWritesNothing() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()

            viewModel.onToggleChecked(DocumentType.LICENSE)
            viewModel.onRejectClick()
            viewModel.onRejectReasonChanged("   ")
            viewModel.onRejectConfirm()
            advanceUntilIdle()

            val content = viewModel.content()
            assertNotNull(content.rejectionDraft)
            assertEquals(VerificationError.InvalidRejectionReason, content.notice)
            assertTrue(repository.verdicts.isEmpty())
        }

    // docs/decisions.md, 2026-10-08, one reason for the whole selection.
    @Test
    fun aRejectionRecordsTheSameReasonOnEveryMarkedDocument() =
        runTest {
            repository.documents =
                everyRequiredNurseDocument.map { verificationDocument(type = it, status = ReviewStatus.PENDING) }
            val viewModel = opened()

            viewModel.onToggleChecked(DocumentType.LICENSE)
            viewModel.onToggleChecked(DocumentType.DEGREE)
            viewModel.onRejectClick()
            viewModel.onRejectReasonChanged("Foto borrosa")
            viewModel.onRejectConfirm()
            advanceUntilIdle()

            val checklist = viewModel.content().dossier.checklist
            listOf(DocumentType.LICENSE, DocumentType.DEGREE).forEach { type ->
                val document = checklist.documentFor(type)
                assertEquals("Foto borrosa", document?.rejectionReason)
                assertEquals(ReviewStatus.REJECTED, document?.status)
            }
            assertEquals(ReviewStatus.PENDING, checklist.documentFor(DocumentType.SELFIE)?.status)
        }

    @Test
    fun aRejectionClosesTheDialog() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()

            viewModel.onToggleChecked(DocumentType.LICENSE)
            viewModel.onRejectClick()
            viewModel.onRejectReasonChanged("Foto borrosa")
            viewModel.onRejectConfirm()
            advanceUntilIdle()

            assertNull(viewModel.content().rejectionDraft)
        }

    // The write succeeded and only the reread failed: the dialog must not stay
    // open over a rejection that already happened, or the admin sends it again.
    @Test
    fun aRejectionWhoseRereadFailsStillClosesTheDialog() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()
            viewModel.onToggleChecked(DocumentType.LICENSE)
            viewModel.onRejectClick()
            viewModel.onRejectReasonChanged("Foto borrosa")
            repository.documentsFailure = VerificationError.NetworkUnavailable

            viewModel.onRejectConfirm()
            advanceUntilIdle()

            val content = viewModel.content()
            assertNull(content.rejectionDraft)
            assertEquals(VerificationError.NetworkUnavailable, content.notice)
        }
}
