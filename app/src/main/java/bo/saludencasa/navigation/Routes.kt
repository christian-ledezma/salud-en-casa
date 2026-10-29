package bo.saludencasa.navigation

import kotlinx.serialization.Serializable

@Serializable
data class StartupRoute(
    val playIntro: Boolean = true,
)

@Serializable
data object WelcomeRoute

@Serializable
data object RoleSelectionRoute

@Serializable
data object ProfileRoute

@Serializable
data object AccountRoute

@Serializable
data class PublicProfileRoute(
    val professionalId: String,
)

@Serializable
data class ScheduleAppointmentRoute(
    val professionalId: String,
    val serviceTypeId: String? = null,
)

@Serializable
data object ProfessionalSearchRoute

@Serializable
data object ImmediateCareRoute

@Serializable
data class MyRequestsRoute(
    val justCreated: Boolean = false,
)

@Serializable
data object InboxRoute

// The one screen of a negotiation, for either side: the professional's first
// offer and the thread that follows it (docs/decisions.md, 2026-10-27, HU-18 and
// HT-24). A null professionalId means the caller is the professional of it.
@Serializable
data class OfferThreadRoute(
    val requestId: String,
    val professionalId: String? = null,
)

// HU-19: the sealed agreement of a request, reached once an offer is accepted.
@Serializable
data class AttentionRoute(
    val requestId: String,
)

@Serializable
data object AvailabilityRoute

@Serializable
data object MyServicesRoute

@Serializable
data object AddressListRoute

@Serializable
data class AddressRoute(
    val addressId: String? = null,
)

@Serializable
data object VerificationRoute

@Serializable
data object DocumentReviewQueueRoute

@Serializable
data class DocumentReviewRoute(
    val profileId: String,
)
