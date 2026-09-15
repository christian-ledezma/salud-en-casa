package bo.saludencasa.features.profile.data.model

import bo.saludencasa.core.network.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

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
data class ProfessionalDto(
    @SerialName("professional_type") val professionalType: String? = null,
    val specialty: String? = null,
    val biography: String? = null,
    @SerialName("years_of_experience") val yearsOfExperience: Int = 0,
    @SerialName("base_rate_bob")
    @Serializable(with = BigDecimalSerializer::class)
    val baseRateBob: BigDecimal? = null,
    @SerialName("coverage_radius_km")
    @Serializable(with = BigDecimalSerializer::class)
    val coverageRadiusKm: BigDecimal,
    @SerialName("available_now") val availableNow: Boolean = false,
)

// professional_directory, the public projection. It carries no phone and no
// email by construction, which is how RN-06 is kept without a column level
// policy (docs/decisions.md, 2026-09-11).
@Serializable
data class PublicProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("average_rating") val averageRating: Double = 0.0,
    @SerialName("total_reviews") val totalReviews: Int = 0,
    @SerialName("total_services") val totalServices: Int = 0,
    @SerialName("professional_type") val professionalType: String? = null,
    val specialty: String? = null,
    val biography: String? = null,
    @SerialName("years_of_experience") val yearsOfExperience: Int = 0,
    @SerialName("base_rate_bob")
    @Serializable(with = BigDecimalSerializer::class)
    val baseRateBob: BigDecimal? = null,
    @SerialName("available_now") val availableNow: Boolean = false,
)

@Serializable
data class SaveProfileParams(
    @SerialName("p_full_name") val fullName: String,
    @SerialName("p_phone") val phone: String? = null,
    @SerialName("p_birth_date") val birthDate: String? = null,
    @SerialName("p_emergency_contact") val emergencyContact: String? = null,
    @SerialName("p_medical_notes") val medicalNotes: String? = null,
    @SerialName("p_professional_type") val professionalType: String? = null,
    @SerialName("p_specialty") val specialty: String? = null,
    @SerialName("p_biography") val biography: String? = null,
    @SerialName("p_years_of_experience") val yearsOfExperience: Int? = null,
    @SerialName("p_base_rate_bob")
    @Serializable(with = BigDecimalSerializer::class)
    val baseRateBob: BigDecimal? = null,
    @SerialName("p_coverage_radius_km")
    @Serializable(with = BigDecimalSerializer::class)
    val coverageRadiusKm: BigDecimal? = null,
)
