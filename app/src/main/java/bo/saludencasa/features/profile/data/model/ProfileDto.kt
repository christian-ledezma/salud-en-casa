package bo.saludencasa.features.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val role: String? = null,
)

@Serializable
data class PatientDto(
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("medical_notes") val medicalNotes: String? = null,
)

@Serializable
data class SaveProfileParams(
    @SerialName("p_full_name") val fullName: String,
    @SerialName("p_phone") val phone: String? = null,
    @SerialName("p_birth_date") val birthDate: String? = null,
    @SerialName("p_emergency_contact") val emergencyContact: String? = null,
    @SerialName("p_medical_notes") val medicalNotes: String? = null,
)
