package bo.saludencasa.features.profile.domain.model

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.domain.vo.BirthDate

data class UserProfile(
    val userId: String,
    val fullName: String,
    val email: Email?,
    val phone: PhoneNumber?,
    val photoUrl: String?,
    val role: UserRole?,
    val patient: PatientDetails?,
)

data class PatientDetails(
    val birthDate: BirthDate?,
    val emergencyContact: String?,
    val medicalNotes: String?,
)
