package bo.saludencasa.features.verification.domain.model

import java.time.Instant

data class PendingReviewSubject(
    val profileId: String,
    val fullName: String,
    val email: String,
    val pendingCount: Int,
    val oldestPendingAt: Instant,
)
