package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.data.model.PatientDto
import bo.saludencasa.features.profile.data.model.ProfessionalDto
import bo.saludencasa.features.profile.data.model.ProfileDto
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfessionalUpdate
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.vo.BirthDate
import bo.saludencasa.features.profile.domain.vo.CoverageRadiusKm
import bo.saludencasa.features.profile.domain.vo.YearsOfExperience
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ProfileMapperTest {
    // handle_new_user() copies the name Google gives straight into the column,
    // without passing it through PersonName. A name with a digit in it is
    // therefore possible, and reading it through the value object would hand the
    // person an empty name field and offer to overwrite what they had.
    @Test
    fun aStoredNameTheValueObjectWouldRefuseStillArrivesWhole() {
        val profile = profileDto(fullName = "Ana 2 Quispe").toUserProfile(null, null)

        assertEquals("Ana 2 Quispe", profile.fullName)
    }

    @Test
    fun eachStoredFieldReachesItsOwnDomainValue() {
        val profile =
            profileDto(phone = "+59171234567", role = "PATIENT")
                .toUserProfile(
                    PatientDto(birthDate = "1990-05-14", emergencyContact = "Luis", medicalNotes = null),
                    null,
                )

        assertEquals("+59171234567", profile.phone?.value)
        assertEquals(UserRole.PATIENT, profile.activeRole)
        assertEquals(LocalDate.of(1990, 5, 14), profile.patient?.birthDate?.value)
        assertEquals("Luis", profile.patient?.emergencyContact)
        assertNull(profile.patient?.medicalNotes)
    }

    // An empty string in the column is the same thing as no answer. Carrying it
    // into the domain would show a field that looks filled in and is not.
    @Test
    fun blankStoredTextIsReadAsAbsent() {
        val profile =
            profileDto(photoUrl = "")
                .toUserProfile(PatientDto(emergencyContact = "", medicalNotes = "  "), null)

        assertNull(profile.photoUrl)
        assertNull(profile.patient?.emergencyContact)
        assertNull(profile.patient?.medicalNotes)
    }

    // A date the column could not hold, or a value user_role gains later, must
    // cost that one field and never the whole profile: the person still has to
    // be able to open the screen and fix it.
    @Test
    fun anUnreadableFieldCostsItselfAndNotTheProfile() {
        val profile =
            profileDto(role = "AUDITOR")
                .toUserProfile(PatientDto(birthDate = "14/05/1990"), null)

        assertEquals("Ana Quispe", profile.fullName)
        assertNull(profile.activeRole)
        assertNull(profile.patient?.birthDate)
    }

    // The same rule for professional_type. A value the enumerated type gains
    // later must not blank out the rate and the radius on the way in.
    @Test
    fun anUnknownProfessionalTypeCostsItselfAndNotTheProfessionalProfile() {
        val profile =
            profileDto(role = "PROFESSIONAL")
                .toUserProfile(null, professionalDto(professionalType = "MIDWIFE"))

        assertNull(profile.professional?.professionalType)
        assertEquals(BigDecimal("120.00"), profile.professional?.baseRateBob)
        assertEquals(BigDecimal("8.00"), profile.professional?.coverageRadiusKm)
    }

    // A professional who has not priced their visit yet reads back with no rate
    // rather than with a zero, which the column would never have accepted.
    @Test
    fun aProfessionalWithoutARateReadsBackWithNoRate() {
        val profile =
            profileDto(role = "PROFESSIONAL")
                .toUserProfile(null, professionalDto(baseRateBob = null, specialty = "  "))

        assertNull(profile.professional?.baseRateBob)
        assertNull(profile.professional?.specialty)
        assertEquals(0, profile.professional?.yearsOfExperience)
    }

    // The save travels as one call to save_my_profile, so the arguments are
    // flattened here. A birth date has to leave as an ISO date because the
    // argument is typed `date`; anything else is rejected by the server with a
    // cast error rather than a validation message.
    @Test
    fun aPatientSaveCarriesTheArgumentsTheFunctionExpects() {
        val params =
            ProfileUpdate(
                fullName = PersonName.create("Ana Quispe").getOrThrow(),
                phone = PhoneNumber.create("71234567").getOrThrow(),
                patient =
                    PatientDetails(
                        birthDate = BirthDate.create(LocalDate.of(1990, 5, 14)).getOrThrow(),
                        emergencyContact = "Luis Quispe",
                        medicalNotes = null,
                    ),
                professional = null,
            ).toSaveProfileParams()

        assertEquals("Ana Quispe", params.fullName)
        assertEquals("+59171234567", params.phone)
        assertEquals("1990-05-14", params.birthDate)
        assertEquals("Luis Quispe", params.emergencyContact)
        assertNull(params.medicalNotes)
    }

    // A professional has no patient section. Flattening it into the same
    // arguments makes it easy to leak one by accident, and the columns are not
    // theirs to fill.
    @Test
    fun aProfessionalSaveCarriesNoPatientArgument() {
        val params = professionalUpdate().toSaveProfileParams()

        assertNull(params.birthDate)
        assertNull(params.emergencyContact)
        assertNull(params.medicalNotes)
    }

    // The professional type leaves as the name of the enumerated value, because
    // the argument is typed professional_type and the server casts the text it
    // receives. A label instead of the name is rejected as an invalid value.
    @Test
    fun aProfessionalSaveCarriesTheProfessionalArguments() {
        val params = professionalUpdate().toSaveProfileParams()

        assertEquals("NURSE", params.professionalType)
        assertEquals("Enfermería geriátrica", params.specialty)
        assertEquals(10, params.yearsOfExperience)
        assertEquals(BigDecimal("120.00"), params.baseRateBob)
        assertEquals(BigDecimal("8"), params.coverageRadiusKm)
    }

    // The mirror: a patient never sends a professional argument, even as null
    // by accident of a copied constructor.
    @Test
    fun aPatientSaveCarriesNoProfessionalArgument() {
        val params =
            ProfileUpdate(
                fullName = PersonName.create("Ana Quispe").getOrThrow(),
                phone = null,
                patient = PatientDetails(null, null, null),
                professional = null,
            ).toSaveProfileParams()

        assertNull(params.professionalType)
        assertNull(params.yearsOfExperience)
        assertNull(params.baseRateBob)
        assertNull(params.coverageRadiusKm)
    }
}

private fun professionalUpdate(): ProfileUpdate =
    ProfileUpdate(
        fullName = PersonName.create("Ana Quispe").getOrThrow(),
        phone = null,
        patient = null,
        professional =
            ProfessionalUpdate(
                professionalType = ProfessionalType.NURSE,
                specialty = "Enfermería geriátrica",
                biography = null,
                yearsOfExperience = YearsOfExperience.create(10).getOrThrow(),
                baseRateBob = AmountBob.create(BigDecimal("120.00")).getOrThrow(),
                coverageRadiusKm = CoverageRadiusKm.create(BigDecimal("8")).getOrThrow(),
            ),
    )

private fun profileDto(
    fullName: String = "Ana Quispe",
    phone: String? = null,
    photoUrl: String? = null,
    role: String? = null,
): ProfileDto =
    ProfileDto(
        id = "08ddb28f-0000-4000-8000-000000000000",
        fullName = fullName,
        email = "ana.quispe@example.com",
        phone = phone,
        photoUrl = photoUrl,
        activeRole = role,
    )

private fun professionalDto(
    professionalType: String? = "NURSE",
    specialty: String? = "Enfermería geriátrica",
    baseRateBob: BigDecimal? = BigDecimal("120.00"),
): ProfessionalDto =
    ProfessionalDto(
        professionalType = professionalType,
        specialty = specialty,
        biography = null,
        yearsOfExperience = 0,
        baseRateBob = baseRateBob,
        coverageRadiusKm = BigDecimal("8.00"),
        availableNow = false,
    )
