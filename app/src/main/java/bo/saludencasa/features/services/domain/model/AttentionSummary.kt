package bo.saludencasa.features.services.domain.model

import java.math.BigDecimal
import java.time.Instant

// INV-17: someone with both roles reads one side at a time.
enum class AttentionSide {
    PATIENT,
    PROFESSIONAL,
}

// A row of «Mis atenciones»: only what its card shows, so neither the counterpart's
// identifier nor their photo leaves the engine (docs/decisions.md, 2026-11-03, HT-26).
data class AttentionSummary(
    val requestId: String,
    val status: ServiceStatus,
    val finalAmountBob: BigDecimal,
    val serviceTypeName: String,
    val counterpartName: String,
    val createdAt: Instant,
    val scheduledAt: Instant?,
    val startedAt: Instant?,
) {
    // docs/decisions.md, 2026-11-04, HT-27, the date of the card.
    val cardDate: AttentionCardDate
        get() =
            when {
                scheduledAt != null -> AttentionCardDate(AttentionDateKind.APPOINTMENT, scheduledAt)
                startedAt != null -> AttentionCardDate(AttentionDateKind.STARTED, startedAt)
                else -> AttentionCardDate(AttentionDateKind.AGREED, createdAt)
            }
}

enum class AttentionDateKind {
    APPOINTMENT,
    STARTED,
    AGREED,
}

data class AttentionCardDate(
    val kind: AttentionDateKind,
    val at: Instant,
)

sealed interface ActiveAttentionResult {
    data class Found(
        val attention: AttentionSummary,
    ) : ActiveAttentionResult

    data object None : ActiveAttentionResult

    data class Failure(
        val error: ServiceError,
    ) : ActiveAttentionResult
}

sealed interface MyAttentionsResult {
    data class Loaded(
        val attentions: List<AttentionSummary>,
    ) : MyAttentionsResult

    data class Failure(
        val error: ServiceError,
    ) : MyAttentionsResult
}

sealed interface MyAttentionsPageResult {
    data class Loaded(
        val attentions: List<AttentionSummary>,
        val endReached: Boolean,
    ) : MyAttentionsPageResult

    data class Failure(
        val error: ServiceError,
    ) : MyAttentionsPageResult
}
