package bo.saludencasa.features.profile.domain.model

import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.core.vo.PhoneNumber
import java.time.LocalDate

data class ProfileDraft(
    val fullName: String,
    val phone: String,
    val patient: PatientDraft?,
)

data class PatientDraft(
    val birthDate: LocalDate?,
    val emergencyContact: String,
    val medicalNotes: String,
)

data class ProfileUpdate(
    val fullName: PersonName,
    val phone: PhoneNumber?,
    val patient: PatientDetails?,
)
