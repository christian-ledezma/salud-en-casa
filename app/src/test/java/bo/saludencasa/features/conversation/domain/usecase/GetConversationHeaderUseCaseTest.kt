package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.REQUEST_ID
import bo.saludencasa.features.conversation.domain.model.ConversationHeader
import bo.saludencasa.features.conversation.domain.model.ConversationHeaderResult
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.dualRoleProfile
import bo.saludencasa.features.profile.professionalDetails
import bo.saludencasa.features.profile.professionalProfile
import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GetConversationHeaderUseCaseTest {
    private val profiles = FakeProfileRepository(profileResult = professional(ProfessionalType.NURSE))

    private fun professional(type: ProfessionalType) =
        ProfileResult.Success(professionalProfile(professionalDetails(professionalType = type)))

    private suspend fun header(
        status: ServiceStatus,
        iAmThePatient: Boolean = false,
        conversationOpen: Boolean = status != ServiceStatus.CANCELLED,
        writableUntil: Instant? = null,
    ): ConversationHeader {
        val services =
            FakeServiceRepository().apply {
                result =
                    AttentionResult.Loaded(
                        attention(
                            status = status,
                            iAmThePatient = iAmThePatient,
                            conversationOpen = conversationOpen,
                            conversationWritableUntil = writableUntil,
                        ),
                    )
            }
        val loaded =
            GetConversationHeaderUseCase(GetAttentionUseCase(services), GetProfileUseCase(profiles))(REQUEST_ID)
        return (loaded as ConversationHeaderResult.Loaded).header
    }

    // docs/decisions.md, 2026-11-03, HU-21: a cancelled attention is read only.
    @Test
    fun aCancelledAttentionIsReadOnly() =
        runTest {
            assertEquals(false, header(ServiceStatus.CANCELLED).canWrite)
        }

    // docs/decisions.md, 2026-11-04, HU-24: the window is the engine's.
    @Test
    fun aCompletedAttentionTakesMessagesWhileTheEngineSaysSoAndKnowsUntilWhen() =
        runTest {
            val until = Instant.parse("2026-11-16T19:40:00Z")

            val open = header(ServiceStatus.COMPLETED, conversationOpen = true, writableUntil = until)
            assertTrue(open.canWrite)
            assertEquals(until, open.writableUntil)

            val closed = header(ServiceStatus.COMPLETED, conversationOpen = false, writableUntil = until)
            assertFalse(closed.canWrite)
            assertEquals(until, closed.writableUntil)
        }

    @Test
    fun pastItsWindowTheProfessionalNoLongerRecords() =
        runTest {
            val closed = header(ServiceStatus.COMPLETED, conversationOpen = false)

            assertFalse(closed.canRecord)
            assertFalse(closed.showsStudentNotice)
        }

    // docs/decisions.md, 2026-11-03, HU-22: what the insert policy allows, so the
    // screen does not offer what the engine would refuse.
    @Test
    fun theProfessionalRecordsOnlyOnceTheAttentionIsUnderWay() =
        runTest {
            assertTrue(header(ServiceStatus.IN_PROGRESS).canRecord)
            assertTrue(header(ServiceStatus.COMPLETED).canRecord)
            assertFalse(header(ServiceStatus.ASSIGNED).canRecord)
            assertFalse(header(ServiceStatus.CANCELLED).canRecord)
        }

    @Test
    fun thePatientNeverRecords() =
        runTest {
            ServiceStatus.entries.forEach { status ->
                assertFalse(status.name, header(status, iAmThePatient = true).canRecord)
            }
        }

    // RN-03, HU-22 criterion 3.
    @Test
    fun aStudentIsToldNotToIncludeADiagnosis() =
        runTest {
            profiles.profileResult = professional(ProfessionalType.STUDENT)

            assertTrue(header(ServiceStatus.IN_PROGRESS).showsStudentNotice)
        }

    @Test
    fun aTitledProfessionalIsNotToldThat() =
        runTest {
            assertFalse(header(ServiceStatus.IN_PROGRESS).showsStudentNotice)
        }

    // Someone with both roles carries both sections, and the attention decides the
    // side: a student is one even with the patient role active.
    @Test
    fun aStudentWithBothRolesIsToldAsWell() =
        runTest {
            profiles.profileResult =
                ProfileResult.Success(
                    dualRoleProfile(professional = professionalDetails(professionalType = ProfessionalType.STUDENT)),
                )

            assertTrue(header(ServiceStatus.IN_PROGRESS).showsStudentNotice)
        }

    // docs/decisions.md, 2026-11-03, HU-22. Losing the notice would leave a student
    // with no warning at all, and a failed read must not close the conversation.
    @Test
    fun aFailedProfileReadShowsTheNoticeAndKeepsTheConversationOpen() =
        runTest {
            profiles.profileResult = ProfileResult.Failure(ProfileError.NetworkUnavailable)

            val header = header(ServiceStatus.IN_PROGRESS)

            assertTrue(header.showsStudentNotice)
            assertTrue(header.canRecord)
            assertTrue(header.canWrite)
        }

    @Test
    fun theProfileIsReadOnlyWhenARecordIsPossible() =
        runTest {
            header(ServiceStatus.ASSIGNED)
            header(ServiceStatus.CANCELLED)
            header(ServiceStatus.IN_PROGRESS, iAmThePatient = true)
            assertEquals(0, profiles.profileReads)

            header(ServiceStatus.IN_PROGRESS)
            assertEquals(1, profiles.profileReads)
        }
}
