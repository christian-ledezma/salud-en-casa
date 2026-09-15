package bo.saludencasa.features.profile.domain.model

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.domain.vo.BirthDate
import java.math.BigDecimal

data class UserProfile(
    val userId: String,
    val fullName: String,
    val email: Email?,
    val phone: PhoneNumber?,
    val photoUrl: String?,
    val role: UserRole?,
    val patient: PatientDetails?,
    val professional: ProfessionalDetails?,
)

data class PatientDetails(
    val birthDate: BirthDate?,
    val emergencyContact: String?,
    val medicalNotes: String?,
)

// Read as stored, not through the value objects (docs/decisions.md,
// 2026-09-13): the type and the rate are null until the professional declares
// them, and a stored value the rule would now refuse still has to reach the
// screen so its owner can correct it.
data class ProfessionalDetails(
    val professionalType: ProfessionalType?,
    val specialty: String?,
    val biography: String?,
    val yearsOfExperience: Int,
    val baseRateBob: BigDecimal?,
    val coverageRadiusKm: BigDecimal,
    val availableNow: Boolean,
)

// The projection of professional_directory: what any signed in user reads
// about a professional, with no contact data by construction.
data class PublicProfile(
    val professionalId: String,
    val fullName: String,
    val photoUrl: String?,
    val averageRating: Double,
    val totalReviews: Int,
    val totalServices: Int,
    val professionalType: ProfessionalType?,
    val specialty: String?,
    val biography: String?,
    val yearsOfExperience: Int,
    val baseRateBob: BigDecimal?,
    val availableNow: Boolean,
)
