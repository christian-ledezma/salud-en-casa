package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.PatientDraft
import bo.saludencasa.features.profile.domain.model.ProfessionalDraft
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileDraft
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class SaveProfileUseCaseTest {
    // RF-02.1 with the criterion of HU-03: a phone in the wrong format is
    // reported and nothing is written. Sending it and letting the column check
    // refuse it would cost a round trip and return a message about a constraint.
    @Test
    fun rejectsAPhoneTheValueObjectRefusesAndWritesNothing() =
        runTest {
            val repository = FakeProfileRepository()

            val result = SaveProfileUseCase(repository)(draft(phone = "123"))

            assertEquals(ProfileResult.Failure(ProfileError.InvalidPhone), result)
            assertEquals(0, repository.saveAttempts)
        }

    // The column allows a null phone, so an empty field is a person who has not
    // given one yet, not a person who typed something invalid.
    @Test
    fun anEmptyPhoneIsSavedAsNoPhoneAtAll() =
        runTest {
            val repository = FakeProfileRepository()

            SaveProfileUseCase(repository)(draft(phone = "   "))

            assertEquals(1, repository.saveAttempts)
            assertNull(repository.lastUpdate?.phone)
        }

    @Test
    fun rejectsANameTheValueObjectRefusesAndWritesNothing() =
        runTest {
            val repository = FakeProfileRepository()

            val result = SaveProfileUseCase(repository)(draft(fullName = "A"))

            assertEquals(ProfileResult.Failure(ProfileError.InvalidName), result)
            assertEquals(0, repository.saveAttempts)
        }

    @Test
    fun rejectsABirthDateThatIsNotInThePastAndWritesNothing() =
        runTest {
            val repository = FakeProfileRepository()
            val tomorrow = LocalDate.now().plusDays(1)

            val result = SaveProfileUseCase(repository)(draft(patient = patientDraft(birthDate = tomorrow)))

            assertEquals(ProfileResult.Failure(ProfileError.InvalidBirthDate), result)
            assertEquals(0, repository.saveAttempts)
        }

    // An emptied field has to reach the column as null. Writing the empty string
    // instead would leave an emergency contact that looks filled in to every
    // query that only checks whether the column is null.
    @Test
    fun clearedPatientTextIsStoredAsNullAndNotAsAnEmptyString() =
        runTest {
            val repository = FakeProfileRepository()

            SaveProfileUseCase(repository)(
                draft(patient = patientDraft(emergencyContact = "   ", medicalNotes = "")),
            )

            assertNull(repository.lastUpdate?.patient?.emergencyContact)
            assertNull(repository.lastUpdate?.patient?.medicalNotes)
        }

    // A professional has no row in patients yet, and the patient columns are not
    // theirs to fill. The draft carries no patient section and the update must
    // not invent one.
    @Test
    fun aProfessionalWritesNoPatientData() =
        runTest {
            val repository = FakeProfileRepository()

            SaveProfileUseCase(repository)(draft(patient = null, professional = professionalDraft()))

            assertEquals(1, repository.saveAttempts)
            assertNull(repository.lastUpdate?.patient)
        }

    // The mirror of the rule above. A patient has no row in professionals, so a
    // professional section reaching the update would be columns nobody may fill.
    @Test
    fun aPatientWritesNoProfessionalData() =
        runTest {
            val repository = FakeProfileRepository()

            SaveProfileUseCase(repository)(draft())

            assertEquals(1, repository.saveAttempts)
            assertNull(repository.lastUpdate?.professional)
        }

    // The criterion of HU-04: a rate of zero or below is refused with a message,
    // not with the constraint name the column would return.
    @Test
    fun rejectsARateTheValueObjectRefusesAndWritesNothing() =
        runTest {
            val repository = FakeProfileRepository()

            val result =
                SaveProfileUseCase(repository)(
                    draft(patient = null, professional = professionalDraft(baseRateBob = "0")),
                )

            assertEquals(ProfileResult.Failure(ProfileError.InvalidBaseRate), result)
            assertEquals(0, repository.saveAttempts)
        }

    @Test
    fun rejectsARadiusOutsideTheAllowedRangeAndWritesNothing() =
        runTest {
            val repository = FakeProfileRepository()

            val result =
                SaveProfileUseCase(repository)(
                    draft(patient = null, professional = professionalDraft(coverageRadiusKm = "60")),
                )

            assertEquals(ProfileResult.Failure(ProfileError.InvalidCoverageRadius), result)
            assertEquals(0, repository.saveAttempts)
        }

    @Test
    fun rejectsYearsOfExperienceOutsideTheAllowedRangeAndWritesNothing() =
        runTest {
            val repository = FakeProfileRepository()

            val result =
                SaveProfileUseCase(repository)(
                    draft(patient = null, professional = professionalDraft(yearsOfExperience = "80")),
                )

            assertEquals(ProfileResult.Failure(ProfileError.InvalidYearsOfExperience), result)
            assertEquals(0, repository.saveAttempts)
        }

    // base_rate_bob admits null, so a professional who has not priced their
    // visit yet saves the rest of the profile. What they cannot do is become
    // visible: professionals_approved_profile_is_complete blocks the approval.
    @Test
    fun anEmptyRateIsSavedAsNoRateAtAll() =
        runTest {
            val repository = FakeProfileRepository()

            SaveProfileUseCase(repository)(
                draft(patient = null, professional = professionalDraft(baseRateBob = "  ")),
            )

            assertEquals(1, repository.saveAttempts)
            assertNull(repository.lastUpdate?.professional?.baseRateBob)
        }

    @Test
    fun clearedProfessionalTextIsStoredAsNullAndNotAsAnEmptyString() =
        runTest {
            val repository = FakeProfileRepository()

            SaveProfileUseCase(repository)(
                draft(patient = null, professional = professionalDraft(specialty = "   ", biography = "")),
            )

            assertNull(repository.lastUpdate?.professional?.specialty)
            assertNull(repository.lastUpdate?.professional?.biography)
        }

    // The professional type is nullable until the profile is complete, so a
    // save with none chosen has to reach the repository rather than stop.
    @Test
    fun aProfessionalWithoutATypeChosenYetStillSavesTheRest() =
        runTest {
            val repository = FakeProfileRepository()

            SaveProfileUseCase(repository)(
                draft(patient = null, professional = professionalDraft(professionalType = null)),
            )

            assertEquals(1, repository.saveAttempts)
            assertNull(repository.lastUpdate?.professional?.professionalType)
            assertEquals(
                BigDecimal("120.00"),
                repository.lastUpdate
                    ?.professional
                    ?.baseRateBob
                    ?.value,
            )
        }
}

private fun draft(
    fullName: String = "Ana Quispe",
    phone: String = "71234567",
    patient: PatientDraft? = patientDraft(),
    professional: ProfessionalDraft? = null,
): ProfileDraft =
    ProfileDraft(
        fullName = fullName,
        phone = phone,
        patient = patient,
        professional = professional,
    )

private fun patientDraft(
    birthDate: LocalDate? = LocalDate.of(1990, 5, 14),
    emergencyContact: String = "Luis Quispe",
    medicalNotes: String = "",
): PatientDraft =
    PatientDraft(
        birthDate = birthDate,
        emergencyContact = emergencyContact,
        medicalNotes = medicalNotes,
    )

private fun professionalDraft(
    professionalType: ProfessionalType? = ProfessionalType.NURSE,
    specialty: String = "Enfermería geriátrica",
    biography: String = "Diez años atendiendo a domicilio.",
    yearsOfExperience: String = "10",
    baseRateBob: String = "120.00",
    coverageRadiusKm: String = "8",
): ProfessionalDraft =
    ProfessionalDraft(
        professionalType = professionalType,
        specialty = specialty,
        biography = biography,
        yearsOfExperience = yearsOfExperience,
        baseRateBob = baseRateBob,
        coverageRadiusKm = coverageRadiusKm,
    )
