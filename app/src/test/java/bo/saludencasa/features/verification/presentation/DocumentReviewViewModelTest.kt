@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.verification.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.verification.FakeDocumentReviewRepository
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.ApproveDocumentUseCase
import bo.saludencasa.features.verification.domain.usecase.ApproveProfessionalVerificationUseCase
import bo.saludencasa.features.verification.domain.usecase.GetDocumentReviewDossierUseCase
import bo.saludencasa.features.verification.domain.usecase.GetSignedDocumentUrlUseCase
import bo.saludencasa.features.verification.domain.usecase.RejectDocumentUseCase
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

    private fun TestScope.opened(type: DocumentType = DocumentType.LICENSE): DocumentReviewViewModel {
        val viewModel =
            DocumentReviewViewModel(
                profileId = "subject-id",
                initialType = type,
                getDossier = GetDocumentReviewDossierUseCase(repository),
                getSignedUrl = GetSignedDocumentUrlUseCase(repository),
                approveDocument = ApproveDocumentUseCase(repository),
                rejectDocument = RejectDocumentUseCase(repository),
                approveProfessional = ApproveProfessionalVerificationUseCase(repository),
            )
        advanceUntilIdle()
        return viewModel
    }

    private fun DocumentReviewViewModel.content() = uiState.value as DocumentReviewUiState.Content

    @Test
    fun approvingTheLastRequiredDocumentOffersTheProfessionalApproval() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()
            assertFalse(viewModel.content().dossier.canApproveProfessional)

            viewModel.onApprove()
            advanceUntilIdle()

            assertTrue(viewModel.content().dossier.canApproveProfessional)
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

            viewModel.onApprove()
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

            viewModel.onApprove()
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
            repository.documents = listOf(verificationDocument(type = DocumentType.ID_FRONT))

            val viewModel = opened(type = DocumentType.DEGREE)

            assertEquals(DocumentImageState.Missing, viewModel.content().image)
            assertTrue(repository.urlRequests.isEmpty())
        }

    @Test
    fun selectingAnotherDocumentRequestsItsOwnSignedUrl() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened(type = DocumentType.LICENSE)

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

            viewModel.onRejectClick()
            viewModel.onRejectReasonChanged("   ")
            viewModel.onRejectConfirm()
            advanceUntilIdle()

            val content = viewModel.content()
            assertNotNull(content.rejectionDraft)
            assertEquals(VerificationError.InvalidRejectionReason, content.notice)
            assertTrue(repository.verdicts.isEmpty())
        }

    @Test
    fun aRejectionRecordsItsReasonOnTheDocument() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()

            viewModel.onRejectClick()
            viewModel.onRejectReasonChanged("Foto borrosa")
            viewModel.onRejectConfirm()
            advanceUntilIdle()

            val license =
                viewModel
                    .content()
                    .dossier.checklist
                    .documentFor(DocumentType.LICENSE)
            assertEquals("Foto borrosa", license?.rejectionReason)
            assertEquals(ReviewStatus.REJECTED, license?.status)
        }

    @Test
    fun aRejectionClosesTheDialog() =
        runTest {
            onlyTheLicenseIsLeft()
            val viewModel = opened()

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
