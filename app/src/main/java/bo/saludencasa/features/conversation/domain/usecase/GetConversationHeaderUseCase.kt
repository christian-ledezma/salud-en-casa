package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.domain.model.ConversationHeader
import bo.saludencasa.features.conversation.domain.model.ConversationHeaderResult
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase

class GetConversationHeaderUseCase(
    private val getAttention: GetAttentionUseCase,
    private val getProfile: GetProfileUseCase,
) {
    suspend operator fun invoke(requestId: String): ConversationHeaderResult =
        when (val result = getAttention(requestId)) {
            is AttentionResult.Loaded -> {
                val attention = result.attention
                val canRecord = attention.canRecord()
                ConversationHeaderResult.Loaded(
                    ConversationHeader(
                        counterpartName = attention.counterpart.name,
                        counterpartPhotoUrl = attention.counterpart.photoUrl,
                        serviceTypeName = attention.serviceTypeName,
                        status = attention.status,
                        // The insert policy says the same (docs/decisions.md,
                        // 2026-11-03, HU-21); this only spares a refused send.
                        canWrite = attention.status != ServiceStatus.CANCELLED,
                        canRecord = canRecord,
                        showsStudentNotice = canRecord && mayBeAStudent(),
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
                        ServiceError.AttentionChanged, ServiceError.Unexpected -> MessageError.Unexpected
                    },
                )
            }
        }

    // The insert policy says the same (docs/decisions.md, 2026-11-03, HU-22).
    private fun Attention.canRecord(): Boolean =
        !iAmThePatient && (status == ServiceStatus.IN_PROGRESS || status == ServiceStatus.COMPLETED)

    // docs/decisions.md, 2026-11-03, HU-22: with the type unknown, the notice shows.
    private suspend fun mayBeAStudent(): Boolean =
        when (val result = getProfile()) {
            is ProfileResult.Success -> result.profile.professional?.professionalType == ProfessionalType.STUDENT
            is ProfileResult.Failure -> true
        }
}
