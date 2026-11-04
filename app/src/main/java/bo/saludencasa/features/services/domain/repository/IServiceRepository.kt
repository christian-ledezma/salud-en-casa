package bo.saludencasa.features.services.domain.repository

import bo.saludencasa.features.services.domain.model.ActiveAttentionResult
import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.MyAttentionsResult
import kotlinx.coroutines.flow.Flow

interface IServiceRepository {
    // The sealed agreement of a request, read by whichever of its two parties
    // opens it (RF-08.5, RF-08.6).
    suspend fun getAttention(requestId: String): AttentionResult

    suspend fun getMyAttentions(
        side: AttentionSide,
        offset: Int,
        limit: Int,
    ): MyAttentionsResult

    suspend fun getActiveAttention(side: AttentionSide): ActiveAttentionResult

    suspend fun advance(
        requestId: String,
        step: AttentionStep,
    ): AdvanceAttentionResult

    fun attentionSignals(requestId: String): Flow<Unit>
}
