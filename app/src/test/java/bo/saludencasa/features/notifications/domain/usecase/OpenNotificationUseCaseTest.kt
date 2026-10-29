package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.OTHER_USER
import bo.saludencasa.features.notifications.PROFESSIONAL_ID
import bo.saludencasa.features.notifications.REQUEST_ID
import bo.saludencasa.features.notifications.appointmentCancelled
import bo.saludencasa.features.notifications.appointmentScheduled
import bo.saludencasa.features.notifications.counterOfferReceived
import bo.saludencasa.features.notifications.domain.model.NotificationDestination
import bo.saludencasa.features.notifications.domain.model.NotificationOpenResult
import bo.saludencasa.features.notifications.nearbyRequest
import bo.saludencasa.features.notifications.offerReceived
import bo.saludencasa.features.notifications.requestExpired
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
import bo.saludencasa.features.profile.domain.usecase.SwitchActiveRoleUseCase
import bo.saludencasa.features.profile.loadedRoles
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenNotificationUseCaseTest {
    private val bothRoles = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL)

    private fun useCase(
        profiles: FakeProfileRepository,
        notifications: FakeNotificationRepository = FakeNotificationRepository(),
    ) = OpenNotificationUseCase(notifications, GetRolesUseCase(profiles), SwitchActiveRoleUseCase(profiles))

    // RF-09.11: the inbox only exists for the professional role, so a person
    // looking at the app as a patient is switched before it opens.
    @Test
    fun tappingANotificationOfTheOtherRoleActivatesThatRoleBeforeOpeningIt() =
        runTest {
            val profiles = FakeProfileRepository(roleResult = loadedRoles(held = bothRoles, active = UserRole.PATIENT))

            val result = useCase(profiles)(nearbyRequest())

            assertEquals(UserRole.PROFESSIONAL, profiles.lastActiveRole)
            assertEquals(
                NotificationOpenResult.Open(NotificationDestination.OfferThread(REQUEST_ID, professionalId = null)),
                result,
            )
        }

    @Test
    fun tappingANotificationOfThePatientRoleSwitchesTheOtherWay() =
        runTest {
            val profiles =
                FakeProfileRepository(roleResult = loadedRoles(held = bothRoles, active = UserRole.PROFESSIONAL))

            val result = useCase(profiles)(requestExpired())

            assertEquals(UserRole.PATIENT, profiles.lastActiveRole)
            assertEquals(NotificationOpenResult.Open(NotificationDestination.MyRequests), result)
        }

    @Test
    fun tappingANotificationOfTheActiveRoleDoesNotSwitch() =
        runTest {
            val profiles =
                FakeProfileRepository(roleResult = loadedRoles(held = bothRoles, active = UserRole.PROFESSIONAL))

            useCase(profiles)(nearbyRequest())

            assertEquals(0, profiles.switchAttempts)
        }

    // Opening the inbox in the patient role would show a screen that answers
    // as if the person were someone else.
    @Test
    fun aFailedRoleSwitchOpensNothing() =
        runTest {
            val profiles =
                FakeProfileRepository(
                    roleResult = loadedRoles(held = bothRoles, active = UserRole.PATIENT),
                    switchResult = SwitchRoleResult.Failure(ProfileError.NetworkUnavailable),
                )

            val result = useCase(profiles)(nearbyRequest())

            assertEquals(NotificationOpenResult.Failure(ProfileError.NetworkUnavailable), result)
        }

    @Test
    fun aTapWhileSignedOutIsDropped() =
        runTest {
            val profiles = FakeProfileRepository(roleResult = loadedRoles(held = bothRoles, active = UserRole.PATIENT))

            val result = useCase(profiles, FakeNotificationRepository(signedInUserId = null))(nearbyRequest())

            assertEquals(NotificationOpenResult.Dropped, result)
            assertEquals(0, profiles.switchAttempts)
        }

    // A notification left in the shade by the previous account on a shared
    // phone must not switch the new account's role nor open anything.
    @Test
    fun aTapMeantForAnotherAccountIsDropped() =
        runTest {
            val profiles = FakeProfileRepository(roleResult = loadedRoles(held = bothRoles, active = UserRole.PATIENT))

            val result = useCase(profiles)(nearbyRequest(recipientId = OTHER_USER))

            assertEquals(NotificationOpenResult.Dropped, result)
            assertEquals(0, profiles.switchAttempts)
        }

    // HT-24: a request for the professional opens that request, where they
    // offer, and an offer opens the thread of the professional who made it. A
    // cancelled appointment has nothing left to open, so it goes to the inbox.
    @Test
    fun eachEventOpensItsOwnDestination() =
        runTest {
            val profiles =
                FakeProfileRepository(roleResult = loadedRoles(held = bothRoles, active = UserRole.PROFESSIONAL))

            assertEquals(
                NotificationOpenResult.Open(NotificationDestination.OfferThread(REQUEST_ID, professionalId = null)),
                useCase(profiles)(appointmentScheduled()),
            )
            assertEquals(
                NotificationOpenResult.Open(NotificationDestination.Inbox),
                useCase(profiles)(appointmentCancelled()),
            )
            assertEquals(
                NotificationOpenResult.Open(NotificationDestination.OfferThread(REQUEST_ID, PROFESSIONAL_ID)),
                useCase(profiles)(offerReceived()),
            )
        }

    // HU-18's criterion 5: the counter-offer reaches either side, and each one
    // lands on the same thread with the role it receives it in activated first.
    @Test
    fun aCounterOfferOpensItsThreadForWhicheverSideReceivesIt() =
        runTest {
            listOf(UserRole.PATIENT, UserRole.PROFESSIONAL).forEach { role ->
                val profiles =
                    FakeProfileRepository(
                        roleResult = loadedRoles(held = bothRoles, active = role.other()),
                    )

                val result = useCase(profiles)(counterOfferReceived(recipientRole = role))

                assertEquals(role, profiles.lastActiveRole)
                // The professional opens their own thread without their own
                // identifier, so the screen knows its side before reading.
                val expectedProfessional = PROFESSIONAL_ID.takeIf { role == UserRole.PATIENT }
                assertEquals(
                    NotificationOpenResult.Open(
                        NotificationDestination.OfferThread(REQUEST_ID, expectedProfessional),
                    ),
                    result,
                )
            }
        }

    private fun UserRole.other(): UserRole = if (this == UserRole.PATIENT) UserRole.PROFESSIONAL else UserRole.PATIENT
}
