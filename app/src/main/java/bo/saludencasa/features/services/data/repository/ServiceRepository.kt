package bo.saludencasa.features.services.data.repository

import bo.saludencasa.features.services.data.datasource.SupabaseServiceDataSource
import bo.saludencasa.features.services.data.mapper.toAttention
import bo.saludencasa.features.services.data.mapper.toServiceError
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import kotlinx.coroutines.CancellationException

class ServiceRepository(
    private val dataSource: SupabaseServiceDataSource,
) : IServiceRepository {
    override suspend fun getAttention(requestId: String): AttentionResult {
        if (dataSource.currentUserId() == null) {
            return AttentionResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            val dto = dataSource.fetchAttention(requestId) ?: return AttentionResult.NotFound
            dto.toAttention()?.let(AttentionResult::Loaded)
                ?: AttentionResult.Failure(ServiceError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AttentionResult.Failure(failure.toServiceError())
        }
    }
}
