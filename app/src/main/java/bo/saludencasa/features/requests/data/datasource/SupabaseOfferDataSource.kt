package bo.saludencasa.features.requests.data.datasource

import bo.saludencasa.features.requests.data.model.NewOfferRow
import bo.saludencasa.features.requests.data.model.OfferIdParam
import bo.saludencasa.features.requests.data.model.OfferableRequestDto
import bo.saludencasa.features.requests.data.model.RequestIdParam
import bo.saludencasa.features.requests.data.model.RequestOfferDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap

class SupabaseOfferDataSource(
    private val supabase: SupabaseClient,
    // The application's scope, so a shared signal outlives the screen that
    // started it while another screen still listens.
    private val scope: CoroutineScope,
) {
    private val signalsByTopic = ConcurrentHashMap<String, SharedFlow<Unit>>()

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // A direct insert under request_offers_insert_participants: the policy folds
    // in verification, coverage or addressee, and the open status (RN-01, RN-04).
    suspend fun submitOffer(row: NewOfferRow) {
        supabase.from(REQUEST_OFFERS).insert(row)
    }

    // my_request_offers is a definer function pinned to the caller's own request,
    // joined to the public projection of each professional.
    suspend fun fetchOffersForRequest(requestId: String): List<RequestOfferDto> =
        supabase.postgrest
            .rpc(MY_REQUEST_OFFERS, RequestIdParam(requestId))
            .decodeList<RequestOfferDto>()

    // The professional's own chain, straight off the table: their select policy
    // already pins it to professional_id = auth.uid(), and nothing of the patient
    // is in these columns (RF-08.6), so no definer is needed on this side.
    suspend fun fetchMyThread(
        requestId: String,
        professionalId: String,
    ): List<RequestOfferDto> =
        supabase
            .from(REQUEST_OFFERS)
            .select(THREAD_COLUMNS) {
                filter {
                    eq("request_id", requestId)
                    eq("professional_id", professionalId)
                }
                order("created_at", Order.ASCENDING)
                order("id", Order.ASCENDING)
            }.decodeList<RequestOfferDto>()

    // Only status travels, and only to REJECTED: that is all
    // request_offers_reject_counterparty admits. The rows come back because a
    // policy refusal updates zero rows without raising, and a rejection that
    // changed nothing must not read as a success.
    suspend fun rejectOffer(offerId: String): Int =
        supabase
            .from(REQUEST_OFFERS)
            .update({ set("status", REJECTED) }) {
                select(THREAD_COLUMNS)
                filter { eq("id", offerId) }
            }.decodeList<RequestOfferDto>()
            .size

    // accept_offer runs the whole acceptance in one transaction and returns the
    // new service id. A policy or guard refusal raises, which the repository maps;
    // the id is not needed here, the attention opens by request.
    suspend fun acceptOffer(offerId: String) {
        supabase.postgrest.rpc(ACCEPT_OFFER, OfferIdParam(offerId))
    }

    // inbox_request returns at most the one row, with the same guards as the inbox.
    suspend fun fetchOfferableRequest(requestId: String): OfferableRequestDto? =
        supabase.postgrest
            .rpc(INBOX_REQUEST, RequestIdParam(requestId))
            .decodeList<OfferableRequestDto>()
            .firstOrNull()

    // A private broadcast channel, one topic per person and per side:
    // notify_offer_parties sends an empty "offers_changed" to the topic of
    // whoever has to re-query, and the realtime.messages policy lets only that
    // person receive it. The payload carries nothing, so the flow is mapped to
    // Unit and the caller re-queries.
    //
    // One shared flow per topic, because two screens listen on the same one at
    // once -- the wait and the thread opened from it -- and the client hands both
    // the same channel: the screen that closed first would otherwise unsubscribe
    // it from under the one still open. The channel lives while anyone listens.
    fun offerSignals(
        userId: String,
        asProfessional: Boolean,
    ): Flow<Unit> {
        val prefix = if (asProfessional) PROFESSIONAL_OFFERS_TOPIC_PREFIX else PATIENT_OFFERS_TOPIC_PREFIX
        val topic = "$prefix$userId"
        return signalsByTopic.getOrPut(topic) {
            topicSignals(topic).shareIn(scope, SharingStarted.WhileSubscribed())
        }
    }

    private fun topicSignals(topic: String): Flow<Unit> =
        flow {
            val channel = supabase.realtime.channel(topic) { isPrivate = true }
            try {
                emitAll(
                    channel
                        .broadcastFlow<JsonObject>(OFFERS_EVENT)
                        .map { }
                        .onStart { channel.subscribe() },
                )
            } finally {
                // The collection ends by cancellation, and a suspending call in a
                // cancelled coroutine throws before it does anything: without
                // NonCancellable the channel would never be left.
                withContext(NonCancellable) {
                    runCatching { channel.unsubscribe() }
                    runCatching { supabase.realtime.removeChannel(channel) }
                }
            }
        }

    private companion object {
        const val REQUEST_OFFERS = "request_offers"
        const val MY_REQUEST_OFFERS = "my_request_offers"
        const val INBOX_REQUEST = "inbox_request"
        const val ACCEPT_OFFER = "accept_offer"
        const val PATIENT_OFFERS_TOPIC_PREFIX = "patient_offers:"
        const val PROFESSIONAL_OFFERS_TOPIC_PREFIX = "professional_offers:"
        const val OFFERS_EVENT = "offers_changed"
        const val REJECTED = "REJECTED"

        val THREAD_COLUMNS: Columns =
            Columns.list(
                "id",
                "parent_offer_id",
                "professional_id",
                "amount_bob",
                "message",
                "issuer",
                "status",
                "created_at",
            )
    }
}
