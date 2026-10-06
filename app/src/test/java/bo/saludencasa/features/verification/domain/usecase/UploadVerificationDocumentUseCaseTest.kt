package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.verification.FakeVerificationRepository
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.UploadDocumentResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.vo.DocumentImage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadVerificationDocumentUseCaseTest {
    private val repository = FakeVerificationRepository()
    private val useCase = UploadVerificationDocumentUseCase(repository)

    // Catches "the compressor returned zero bytes and we still called the
    // server": a defect that would waste a round trip to be told the row is
    // invalid, and that the schema's trigger would never see.
    @Test
    fun `rejects an empty image and never calls the repository`() =
        runTest {
            val result = useCase(DocumentType.ID_FRONT, ByteArray(0), rawCaption = null)

            assertEquals(UploadDocumentResult.Failure(VerificationError.EmptyImage), result)
            assertTrue(repository.uploadAttempts.isEmpty())
        }

    // The storage bucket caps the payload too (RF-04.3 and the file_size_limit
    // in the migration). Refusing on this side saves the upload and tells the
    // user with a readable error instead of a 413.
    @Test
    fun `rejects an image larger than 2 MiB and never calls the repository`() =
        runTest {
            val oversized = ByteArray(DocumentImage.MAX_BYTES + 1) { 1 }

            val result = useCase(DocumentType.SELFIE, oversized, rawCaption = null)

            assertEquals(UploadDocumentResult.Failure(VerificationError.ImageTooLarge), result)
            assertTrue(repository.uploadAttempts.isEmpty())
        }

    // The schema's biconditional check mirrors this: an OTHER row without a
    // caption would fail the constraint. Refusing before the write names the
    // field for the user and keeps the schema's refusal out of the UI.
    @Test
    fun `rejects an OTHER document without a caption and never calls the repository`() =
        runTest {
            val result = useCase(DocumentType.OTHER, nonEmpty(), rawCaption = "   ")

            assertEquals(UploadDocumentResult.Failure(VerificationError.CaptionRequired), result)
            assertTrue(repository.uploadAttempts.isEmpty())
        }

    @Test
    fun `rejects a caption on a fixed document type and never calls the repository`() =
        runTest {
            val result = useCase(DocumentType.ID_FRONT, nonEmpty(), rawCaption = "sticker")

            assertEquals(UploadDocumentResult.Failure(VerificationError.CaptionNotAllowed), result)
            assertTrue(repository.uploadAttempts.isEmpty())
        }

    @Test
    fun `trims whitespace from an OTHER caption before persisting it`() =
        runTest {
            useCase(DocumentType.OTHER, nonEmpty(), rawCaption = "  Carnet del colegio  ")

            assertEquals("Carnet del colegio", repository.uploadAttempts.single().caption)
        }

    @Test
    fun `an OTHER caption longer than 120 characters is refused as invalid`() =
        runTest {
            val tooLong = "a".repeat(121)

            val result = useCase(DocumentType.OTHER, nonEmpty(), rawCaption = tooLong)

            assertEquals(UploadDocumentResult.Failure(VerificationError.InvalidCaption), result)
            assertTrue(repository.uploadAttempts.isEmpty())
        }

    @Test
    fun `a valid fixed document reaches the repository with a null caption`() =
        runTest {
            useCase(DocumentType.SELFIE, nonEmpty(), rawCaption = null)

            val attempt = repository.uploadAttempts.single()
            assertEquals(DocumentType.SELFIE, attempt.type)
            assertEquals(null, attempt.caption)
        }

    private fun nonEmpty(): ByteArray = ByteArray(16) { 1 }
}
