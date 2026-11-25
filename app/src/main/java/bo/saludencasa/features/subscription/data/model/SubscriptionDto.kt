package bo.saludencasa.features.subscription.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionDto(
    val required: Boolean = false,
    val status: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
)
