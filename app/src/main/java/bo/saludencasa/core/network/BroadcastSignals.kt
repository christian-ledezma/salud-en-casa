package bo.saludencasa.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

// docs/decisions.md, 2026-10-13, HU-14: a private Broadcast topic that carries no
// data, so each emission only tells the collector to re-query.
fun SupabaseClient.broadcastSignals(
    topic: String,
    event: String,
): Flow<Unit> =
    flow {
        val channel = realtime.channel(topic) { isPrivate = true }
        try {
            emitAll(
                channel
                    .broadcastFlow<JsonObject>(event)
                    .map { }
                    .onStart { channel.subscribe() },
            )
        } finally {
            // The collection ends by cancellation, and a suspending call in a
            // cancelled coroutine throws before it does anything: without
            // NonCancellable the channel would never be left.
            withContext(NonCancellable) {
                runCatching { channel.unsubscribe() }
                runCatching { realtime.removeChannel(channel) }
            }
        }
    }
