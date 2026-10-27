package bo.saludencasa.features.requests.data.datasource

import bo.saludencasa.features.requests.data.model.NewOfferRow
import bo.saludencasa.features.requests.data.model.OfferableRequestDto
import bo.saludencasa.features.requests.data.model.RequestIdParam
import bo.saludencasa.features.requests.data.model.RequestOfferDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.JsonObject

class SupabaseOfferDataSource(
    private val supabase: SupabaseClient,
) {
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

    // inbox_request returns at most the one row, with the same guards as the inbox.
    suspend fun fetchOfferableRequest(requestId: String): OfferableRequestDto? =
        supabase.postgrest
            .rpc(INBOX_REQUEST, RequestIdParam(requestId))
            .decodeList<OfferableRequestDto>()
            .firstOrNull()

    // A private broadcast channel, one topic per patient: notify_patient_offers
    // sends an empty "offers_changed" to patient_offers:<id>, and the
    // realtime.messages policy lets only that patient receive it. The payload
    // carries nothing, so the flow is mapped to Unit and the caller re-queries.
    fun offerSignals(userId: String): Flow<Unit> {
        val channel = supabase.realtime.channel("$OFFERS_TOPIC_PREFIX$userId") { isPrivate = true }

        return channel
            .broadcastFlow<JsonObject>(OFFERS_EVENT)
            .map { }
            .onStart { channel.subscribe() }
            .onCompletion {
                runCatching { channel.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(channel) }
            }
    }

    private companion object {
        const val REQUEST_OFFERS = "request_offers"
        const val MY_REQUEST_OFFERS = "my_request_offers"
        const val INBOX_REQUEST = "inbox_request"
        const val OFFERS_TOPIC_PREFIX = "patient_offers:"
        const val OFFERS_EVENT = "offers_changed"
    }
}
