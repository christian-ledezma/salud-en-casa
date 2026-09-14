package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.features.profile.data.model.PatientDto
import bo.saludencasa.features.profile.data.model.ProfileDto
import bo.saludencasa.features.profile.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProfileMapperTest {
    // handle_new_user() copies the name Google gives straight into the column,
    // without passing it through PersonName. A name with a digit in it is
    // therefore possible, and reading it through the value object would hand the
    // person an empty name field and offer to overwrite what they had.
    @Test
    fun aStoredNameTheValueObjectWouldRefuseStillArrivesWhole() {
        val profile = profileDto(fullName = "Ana 2 Quispe").toUserProfile(null)

        assertEquals("Ana 2 Quispe", profile.fullName)
    }

    @Test
    fun eachStoredFieldReachesItsOwnDomainValue() {
        val profile =
            profileDto(phone = "+59171234567", role = "PATIENT")
                .toUserProfile(PatientDto(birthDate = "1990-05-14", emergencyContact = "Luis", medicalNotes = null))

        assertEquals("+59171234567", profile.phone?.value)
        assertEquals(UserRole.PATIENT, profile.role)
        assertEquals(LocalDate.of(1990, 5, 14), profile.patient?.birthDate?.value)
        assertEquals("Luis", profile.patient?.emergencyContact)
        assertNull(profile.patient?.medicalNotes)
    }

    // An empty string in the column is the same thing as no answer. Carrying it
    // into the domain would show a field that looks filled in and is not.
    @Test
    fun blankStoredTextIsReadAsAbsent() {
        val profile = profileDto(photoUrl = "").toUserProfile(PatientDto(emergencyContact = "", medicalNotes = "  "))

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
                .toUserProfile(PatientDto(birthDate = "14/05/1990"))

        assertEquals("Ana Quispe", profile.fullName)
        assertNull(profile.role)
        assertNull(profile.patient?.birthDate)
    }
}

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
        role = role,
    )
