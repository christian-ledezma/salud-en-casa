package bo.saludencasa.features.verification.data.repository

import bo.saludencasa.features.verification.data.datasource.SupabaseVerificationDataSource
import bo.saludencasa.features.verification.data.mapper.toDocument
import bo.saludencasa.features.verification.data.mapper.toVerificationError
import bo.saludencasa.features.verification.data.model.VerificationDocumentRow
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.UploadDocumentResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.repository.IVerificationRepository
import bo.saludencasa.features.verification.domain.vo.DocumentCaption
import bo.saludencasa.features.verification.domain.vo.DocumentImage
import kotlinx.coroutines.CancellationException

class VerificationRepository(
    private val dataSource: SupabaseVerificationDataSource,
) : IVerificationRepository {
    override suspend fun getMyDocuments(): MyDocumentsResult {
        val userId = dataSource.currentUserId() ?: return MyDocumentsResult.Failure(VerificationError.NotSignedIn)

        return try {
            MyDocumentsResult.Loaded(dataSource.findMyDocuments(userId).mapNotNull { it.toDocument() })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            MyDocumentsResult.Failure(failure.toVerificationError())
        }
    }

    override suspend fun uploadDocument(
        type: DocumentType,
        image: DocumentImage,
        caption: DocumentCaption?,
    ): UploadDocumentResult {
        val userId = dataSource.currentUserId() ?: return UploadDocumentResult.Failure(VerificationError.NotSignedIn)

        return try {
            // Row first, object second. Reversed would leave the bucket with
            // an orphan if the row write failed, and the storage policies
            // (update_own and delete_own require a PENDING row that points at
            // the object) would then refuse any later retry on that file. The
            // row is written as an upsert on (profile_id, document_type), so
            // a repeated attempt is a no-op; the object upload is also
            // upsert = true.
            val path = "$userId/${type.name}.jpg"
            dataSource.upsertDocumentRow(
                VerificationDocumentRow(
                    profileId = userId,
                    documentType = type.name,
                    storagePath = path,
                    caption = caption?.value,
                ),
            )
            dataSource.uploadToBucket(path, image.bytes)
            UploadDocumentResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            UploadDocumentResult.Failure(failure.toVerificationError())
        }
    }
}
