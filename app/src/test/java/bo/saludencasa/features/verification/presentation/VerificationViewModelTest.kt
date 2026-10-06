@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.verification.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
import bo.saludencasa.features.profile.loadedRoles
import bo.saludencasa.features.verification.FakeVerificationRepository
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.UploadDocumentResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.GetMyVerificationChecklistUseCase
import bo.saludencasa.features.verification.domain.usecase.UploadVerificationDocumentUseCase
import bo.saludencasa.features.verification.verificationDocument
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VerificationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a patient with no documents sees the three required slots empty`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertEquals(VerificationUiState.Loading, awaitItem())
                val content = awaitItem() as VerificationUiState.Content
                assertEquals(
                    listOf(DocumentType.ID_FRONT, DocumentType.ID_BACK, DocumentType.SELFIE),
                    content.checklist.required,
                )
                assertEquals(null, content.pendingType)
                assertTrue(content.stagedBytes.isEmpty())
            }
        }

    // The retry criterion of HU-07 depends on the bytes staying available
    // after a failed upload: without them, the person has to open the camera
    // again just to send the same picture.
    @Test
    fun `a failed upload keeps the bytes staged for retry`() =
        runTest {
            val verification =
                FakeVerificationRepository(
                    uploadResult = UploadDocumentResult.Failure(VerificationError.NetworkUnavailable),
                )
            val viewModel = viewModel(verification)
            val bytes = ByteArray(32) { 1 }

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onImageChosen(DocumentType.ID_FRONT, bytes)

                val uploading = awaitItem() as VerificationUiState.Content
                assertEquals(DocumentType.ID_FRONT, uploading.pendingType)
                assertEquals(bytes, uploading.stagedBytes[DocumentType.ID_FRONT])

                val failed = awaitItem() as VerificationUiState.Content
                assertEquals(null, failed.pendingType)
                assertEquals(VerificationError.NetworkUnavailable, failed.notice)
                assertEquals(bytes, failed.stagedBytes[DocumentType.ID_FRONT])
            }
        }

    @Test
    fun `retrying uses the staged bytes and never asks for them again`() =
        runTest {
            val verification =
                FakeVerificationRepository(
                    uploadResult = UploadDocumentResult.Failure(VerificationError.NetworkUnavailable),
                )
            val viewModel = viewModel(verification)
            val bytes = ByteArray(32) { 1 }

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onImageChosen(DocumentType.ID_FRONT, bytes)
                awaitItem()
                awaitItem()

                // The next upload passes.
                verification.uploadResult = UploadDocumentResult.Success
                verification.documentsResult =
                    MyDocumentsResult.Loaded(listOf(verificationDocument(type = DocumentType.ID_FRONT)))

                viewModel.onRetry(DocumentType.ID_FRONT)

                val retrying = awaitItem() as VerificationUiState.Content
                assertEquals(DocumentType.ID_FRONT, retrying.pendingType)
                assertEquals(VerificationUiState.Loading, awaitItem())
                val reloaded = awaitItem() as VerificationUiState.Content
                assertEquals(
                    ReviewStatus.PENDING,
                    reloaded.checklist.documentFor(DocumentType.ID_FRONT)?.status,
                )
            }

            // Two attempts for the same slot, no second compression requested.
            assertEquals(2, verification.uploadAttempts.size)
            assertEquals(DocumentType.ID_FRONT, verification.uploadAttempts.first().type)
            assertEquals(DocumentType.ID_FRONT, verification.uploadAttempts.last().type)
        }

    @Test
    fun `a successful upload reloads from the server rather than patching locally`() =
        runTest {
            val verification = FakeVerificationRepository()
            val viewModel = viewModel(verification)
            val bytes = ByteArray(32) { 1 }

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                verification.documentsResult =
                    MyDocumentsResult.Loaded(listOf(verificationDocument(type = DocumentType.SELFIE)))
                viewModel.onImageChosen(DocumentType.SELFIE, bytes)

                awaitItem()
                assertEquals(VerificationUiState.Loading, awaitItem())
                val reloaded = awaitItem() as VerificationUiState.Content
                assertEquals(
                    ReviewStatus.PENDING,
                    reloaded.checklist.documentFor(DocumentType.SELFIE)?.status,
                )
            }

            // The documents endpoint is read once on open, once after upload.
            assertEquals(2, verification.documentReads)
        }

    @Test
    fun `a compression failure surfaces as a notice without writing anything`() =
        runTest {
            val verification = FakeVerificationRepository()
            val viewModel = viewModel(verification)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onCompressionFailed(VerificationError.UnreadableImage)

                val content = awaitItem() as VerificationUiState.Content
                assertEquals(VerificationError.UnreadableImage, content.notice)
            }

            assertTrue(verification.uploadAttempts.isEmpty())
        }

    @Test
    fun `the OTHER caption travels with the upload`() =
        runTest {
            val verification = FakeVerificationRepository()
            val viewModel = viewModel(verification)
            val bytes = ByteArray(32) { 2 }

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onCaptionChanged("Carnet del colegio")
                awaitItem()

                viewModel.onImageChosen(DocumentType.OTHER, bytes)
                awaitItem()
                awaitItem()
                awaitItem()
            }

            val attempt = verification.uploadAttempts.single()
            assertEquals(DocumentType.OTHER, attempt.type)
            assertEquals("Carnet del colegio", attempt.caption)
        }
}

private fun viewModel(
    verification: FakeVerificationRepository = FakeVerificationRepository(),
    profiles: FakeProfileRepository =
        FakeProfileRepository(roleResult = loadedRoles(setOf(UserRole.PATIENT), UserRole.PATIENT)),
): VerificationViewModel =
    VerificationViewModel(
        getChecklist =
            GetMyVerificationChecklistUseCase(
                repository = verification,
                getRoles = GetRolesUseCase(profiles),
                getProfile = GetProfileUseCase(profiles),
            ),
        uploadDocument = UploadVerificationDocumentUseCase(verification),
    )
