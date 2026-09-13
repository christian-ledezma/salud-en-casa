package bo.saludencasa.features.auth.data.mapper

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.auth.domain.model.SessionState
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val FULL_NAME_KEYS = listOf("full_name", "name")

fun UserInfo.toAuthSession(): AuthSession =
    AuthSession(
        userId = id,
        email = email?.let { Email.create(it).getOrNull() },
        fullName = userMetadata.firstStringOf(FULL_NAME_KEYS)?.let { PersonName.create(it).getOrNull() },
    )

fun SessionStatus.toSessionState(): SessionState =
    when (this) {
        is SessionStatus.Authenticated -> {
            session.user?.let { SessionState.SignedIn(it.toAuthSession()) } ?: SessionState.SignedOut
        }

        SessionStatus.Initializing -> {
            SessionState.Loading
        }

        else -> {
            SessionState.SignedOut
        }
    }

private fun JsonObject?.firstStringOf(keys: List<String>): String? =
    keys
        .asSequence()
        .mapNotNull { key -> (this?.get(key) as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
        .firstOrNull { it.isNotBlank() }
