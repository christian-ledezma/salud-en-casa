package bo.saludencasa.features.services.domain.repository

import bo.saludencasa.features.services.domain.model.AttentionResult

interface IServiceRepository {
    // The sealed agreement of a request, read by whichever of its two parties
    // opens it (RF-08.5, RF-08.6).
    suspend fun getAttention(requestId: String): AttentionResult
}
