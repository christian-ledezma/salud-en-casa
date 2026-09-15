package bo.saludencasa.features.profile.domain.model

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.domain.vo.CoverageRadiusKm
import bo.saludencasa.features.profile.domain.vo.YearsOfExperience
import java.time.LocalDate

data class ProfileDraft(
    val fullName: String,
    val phone: String,
    val patient: PatientDraft?,
    val professional: ProfessionalDraft?,
)

data class PatientDraft(
    val birthDate: LocalDate?,
    val emergencyContact: String,
    val medicalNotes: String,
)

// The three numbers travel as the text that was typed. Turning them into
// numbers is a rule, so it belongs to the use case and its value objects, not
// to the screen that collected them.
data class ProfessionalDraft(
    val professionalType: ProfessionalType?,
    val specialty: String,
    val biography: String,
    val yearsOfExperience: String,
    val baseRateBob: String,
    val coverageRadiusKm: String,
)

data class ProfileUpdate(
    val fullName: PersonName,
    val phone: PhoneNumber?,
    val patient: PatientDetails?,
    val professional: ProfessionalUpdate?,
)

data class ProfessionalUpdate(
    val professionalType: ProfessionalType?,
    val specialty: String?,
    val biography: String?,
    val yearsOfExperience: YearsOfExperience,
    val baseRateBob: AmountBob?,
    val coverageRadiusKm: CoverageRadiusKm,
)
