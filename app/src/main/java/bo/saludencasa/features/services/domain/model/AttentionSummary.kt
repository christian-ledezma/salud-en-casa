package bo.saludencasa.features.services.domain.model

import java.math.BigDecimal
import java.time.Instant

// A row of «Mis atenciones»: only what its card shows, so neither the patient's
// identifier nor their photo leaves the engine (docs/decisions.md, 2026-11-03, HT-26).
data class AttentionSummary(
    val requestId: String,
    val status: ServiceStatus,
    val finalAmountBob: BigDecimal,
    val serviceTypeName: String,
    val patientName: String,
    val createdAt: Instant,
    val scheduledAt: Instant?,
)

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
