package bo.saludencasa.features.profile.domain.model

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.domain.vo.BirthDate
import java.math.BigDecimal

// patient and professional are both present for someone who holds both roles
// (RF-01.8). activeRole is what the profile screen edits and shows; it is never
// derived from which of the two sections happens to be loaded.
data class UserProfile(
    val userId: String,
    val fullName: String,
    val email: Email?,
    val phone: PhoneNumber?,
    val photoUrl: String?,
    val activeRole: UserRole?,
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
    val isVerified: Boolean,
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
