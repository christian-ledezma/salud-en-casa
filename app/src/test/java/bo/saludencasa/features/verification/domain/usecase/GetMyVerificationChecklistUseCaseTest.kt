package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
import bo.saludencasa.features.profile.loadedRoles
import bo.saludencasa.features.profile.professionalDetails
import bo.saludencasa.features.profile.professionalProfile
import bo.saludencasa.features.verification.FakeVerificationRepository
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.VerificationChecklistResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMyVerificationChecklistUseCaseTest {
    @Test
    fun `a patient-only checklist is loaded without reading the profile`() =
        runTest {
            val verification = FakeVerificationRepository()
            val profiles = FakeProfileRepository(roleResult = loadedRoles(setOf(UserRole.PATIENT), UserRole.PATIENT))
            val useCase = useCaseWith(verification, profiles)

            val result = useCase() as VerificationChecklistResult.Loaded

            assertEquals(0, profiles.profileReads)
            assertEquals(
                listOf(DocumentType.ID_FRONT, DocumentType.ID_BACK, DocumentType.SELFIE),
                result.checklist.required,
            )
        }

    @Test
    fun `a professional's checklist is tailored to their declared type`() =
        runTest {
            val verification = FakeVerificationRepository()
            val profiles =
                FakeProfileRepository(
                    roleResult = loadedRoles(setOf(UserRole.PROFESSIONAL), UserRole.PROFESSIONAL),
                    profileResult =
                        ProfileResult.Success(
                            professionalProfile(
                                professional = professionalDetails(professionalType = ProfessionalType.STUDENT),
                            ),
                        ),
                )
            val useCase = useCaseWith(verification, profiles)

            val result = useCase() as VerificationChecklistResult.Loaded

            assertTrue(DocumentType.STUDENT_CARD in result.checklist.required)
            assertTrue(DocumentType.LICENSE !in result.checklist.required)
        }

    // Keeps a connectivity failure from reaching the screen as "unexpected".
    @Test
    fun `a network failure reading roles maps to NetworkUnavailable`() =
        runTest {
            val verification = FakeVerificationRepository()
            val profiles = FakeProfileRepository(roleResult = RoleResult.Failure(ProfileError.NetworkUnavailable))
            val useCase = useCaseWith(verification, profiles)

            val result = useCase()

            assertEquals(VerificationChecklistResult.Failure(VerificationError.NetworkUnavailable), result)
            assertEquals(0, verification.documentReads)
        }

    @Test
    fun `a failed documents read propagates the mapped error`() =
        runTest {
            val verification =
                FakeVerificationRepository(
                    documentsResult = MyDocumentsResult.Failure(VerificationError.NetworkUnavailable),
                )
            val profiles = FakeProfileRepository(roleResult = loadedRoles(setOf(UserRole.PATIENT), UserRole.PATIENT))
            val useCase = useCaseWith(verification, profiles)

            val result = useCase()

            assertEquals(VerificationChecklistResult.Failure(VerificationError.NetworkUnavailable), result)
        }

    private fun useCaseWith(
        verification: FakeVerificationRepository,
        profiles: FakeProfileRepository,
    ): GetMyVerificationChecklistUseCase =
        GetMyVerificationChecklistUseCase(
            repository = verification,
            getRoles = GetRolesUseCase(profiles),
            getProfile = GetProfileUseCase(profiles),
        )
}
