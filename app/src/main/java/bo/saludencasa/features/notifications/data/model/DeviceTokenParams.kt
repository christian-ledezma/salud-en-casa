package bo.saludencasa.features.notifications.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// No default value: Postgrest does not encode defaults, and a dropped field
// would reach the function as null.
@Serializable
data class DeviceTokenParams(
    @SerialName("p_token") val token: String,
)
