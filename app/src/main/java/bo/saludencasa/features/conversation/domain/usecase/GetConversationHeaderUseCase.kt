package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.domain.model.ConversationHeader
import bo.saludencasa.features.conversation.domain.model.ConversationHeaderResult
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase

class GetConversationHeaderUseCase(
    private val getAttention: GetAttentionUseCase,
) {
    suspend operator fun invoke(requestId: String): ConversationHeaderResult =
        when (val result = getAttention(requestId)) {
            is AttentionResult.Loaded -> {
                val attention = result.attention
                ConversationHeaderResult.Loaded(
                    ConversationHeader(
                        counterpartName = attention.counterpart.name,
                        counterpartPhotoUrl = attention.counterpart.photoUrl,
                        serviceTypeName = attention.serviceTypeName,
                        status = attention.status,
                        // The insert policy says the same (docs/decisions.md,
                        // 2026-11-03, HU-21); this only spares a refused send.
                        canWrite = attention.status != ServiceStatus.CANCELLED,
                    ),
                )
            }

            AttentionResult.NotFound -> {
                ConversationHeaderResult.NotFound
            }

            is AttentionResult.Failure -> {
                ConversationHeaderResult.Failure(
                    when (result.error) {
                        ServiceError.NotSignedIn -> MessageError.NotSignedIn
                        ServiceError.NetworkUnavailable -> MessageError.NetworkUnavailable
                        ServiceError.Unexpected -> MessageError.Unexpected
                    },
                )
            }
        }
}
