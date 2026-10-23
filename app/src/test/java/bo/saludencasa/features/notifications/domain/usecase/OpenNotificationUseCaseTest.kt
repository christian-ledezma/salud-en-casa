package bo.saludencasa.features.notifications.domain.usecase

import bo.saludencasa.features.notifications.FakeNotificationRepository
import bo.saludencasa.features.notifications.OTHER_USER
import bo.saludencasa.features.notifications.REQUEST_ID
import bo.saludencasa.features.notifications.appointmentCancelled
import bo.saludencasa.features.notifications.appointmentScheduled
import bo.saludencasa.features.notifications.domain.model.NotificationDestination
import bo.saludencasa.features.notifications.domain.model.NotificationOpenResult
import bo.saludencasa.features.notifications.nearbyRequest
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
            assertEquals(NotificationOpenResult.Open(NotificationDestination.Inbox(REQUEST_ID)), result)
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

    // A cancelled appointment never shows in the inbox, so highlighting it
    // would always end in "no longer available".
    @Test
    fun eachEventOpensItsOwnDestination() =
        runTest {
            val profiles =
                FakeProfileRepository(roleResult = loadedRoles(held = bothRoles, active = UserRole.PROFESSIONAL))

            assertEquals(
                NotificationOpenResult.Open(NotificationDestination.Inbox(REQUEST_ID)),
                useCase(profiles)(appointmentScheduled()),
            )
            assertEquals(
                NotificationOpenResult.Open(NotificationDestination.Inbox(null)),
                useCase(profiles)(appointmentCancelled()),
            )
        }
}
