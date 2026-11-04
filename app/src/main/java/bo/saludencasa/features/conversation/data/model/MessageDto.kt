package bo.saludencasa.features.conversation.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

// No dates: stamp_message_created_at and stamp_message_read_at own them.
@Serializable
data class NewMessageRow(
    val id: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
)

@Serializable
data class ConversationPageParams(
    @SerialName("p_request_id") val requestId: String,
    @SerialName("p_limit") val limit: Int,
    @SerialName("p_before_created_at") val beforeCreatedAt: String? = null,
    @SerialName("p_before_id") val beforeId: String? = null,
    @SerialName("p_after_created_at") val afterCreatedAt: String? = null,
    @SerialName("p_after_id") val afterId: String? = null,
)

@Serializable
data class ConversationRequestIdParam(
    @SerialName("p_request_id") val requestId: String,
)
