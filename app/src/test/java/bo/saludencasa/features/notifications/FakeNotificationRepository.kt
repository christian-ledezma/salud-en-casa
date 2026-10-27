package bo.saludencasa.features.notifications

import bo.saludencasa.features.notifications.domain.model.DeviceResult
import bo.saludencasa.features.notifications.domain.model.PushPayload
import bo.saludencasa.features.notifications.domain.model.PushSubject
import bo.saludencasa.features.notifications.domain.repository.INotificationRepository
import bo.saludencasa.features.profile.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import java.time.Instant

const val SIGNED_IN_USER = "08ddb28f-0000-4000-8000-000000000000"
const val OTHER_USER = "7f435cb7-0000-4000-8000-000000000000"
const val REQUEST_ID = "a745c750-0000-4000-8000-000000000000"

// Every call lands in one ordered log, because what the notification rules
// care about is often the order: the server row before the session closes.
class FakeNotificationRepository(
    var signedInUserId: String? = SIGNED_IN_USER,
    var storedInstallationId: String? = null,
    var uploadResult: DeviceResult = DeviceResult.Success,
    var removeResult: DeviceResult = DeviceResult.Success,
    var onRemoveDevice: () -> Unit = {},
) : INotificationRepository {
    val calls = mutableListOf<String>()
    val promptAnswered = MutableStateFlow(false)

    override suspend fun awaitSignedInUserId(): String? = signedInUserId

    override fun requestRegistration() {
        calls += "requestRegistration"
    }

    override suspend fun saveInstallationId(installationId: String) {
        calls += "save:$installationId"
        storedInstallationId = installationId
    }

    override suspend fun storedInstallationId(): String? = storedInstallationId

    override suspend fun uploadDevice(installationId: String): DeviceResult {
        calls += "upload:$installationId"
        return uploadResult
    }

    override suspend fun removeDevice(installationId: String): DeviceResult {
        calls += "remove:$installationId"
        onRemoveDevice()
        return removeResult
    }

    override suspend fun stopRegistration() {
        calls += "stopRegistration"
        storedInstallationId = null
    }

    override fun observePromptAnswered(): Flow<Boolean> = promptAnswered

    override suspend fun markPromptAnswered() {
        calls += "markPromptAnswered"
        promptAnswered.value = true
    }
}

fun pushSubject(
    recipientId: String = SIGNED_IN_USER,
    recipientRole: UserRole = UserRole.PROFESSIONAL,
    serviceTypeName: String = "Inyectables",
): PushSubject = PushSubject(REQUEST_ID, recipientId, recipientRole, serviceTypeName)

fun nearbyRequest(
    recipientId: String = SIGNED_IN_USER,
    distanceM: Int = 1200,
): PushPayload = PushPayload.NearbyRequest(pushSubject(recipientId = recipientId), distanceM)

val APPOINTMENT_AT: Instant = Instant.parse("2026-10-20T14:00:00Z")

fun appointmentScheduled(): PushPayload = PushPayload.AppointmentScheduled(pushSubject(), APPOINTMENT_AT)

fun appointmentCancelled(): PushPayload = PushPayload.AppointmentCancelled(pushSubject(), APPOINTMENT_AT)

fun requestExpired(recipientId: String = SIGNED_IN_USER): PushPayload =
    PushPayload.RequestExpired(pushSubject(recipientId = recipientId, recipientRole = UserRole.PATIENT))

fun offerReceived(
    recipientId: String = SIGNED_IN_USER,
    amountBob: BigDecimal = BigDecimal("160.00"),
): PushPayload =
    PushPayload.OfferReceived(
        pushSubject(recipientId = recipientId, recipientRole = UserRole.PATIENT),
        amountBob,
    )

// One of each, so a test that walks every event cannot miss one: the sealed
// hierarchy is compared against this list.
fun everyPayload(): List<PushPayload> =
    listOf(nearbyRequest(), appointmentScheduled(), appointmentCancelled(), requestExpired(), offerReceived())
