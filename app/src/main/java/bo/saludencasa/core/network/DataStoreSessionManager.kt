package bo.saludencasa.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

// DataStore only: no local database in this phase (docs/decisions.md, 2026-09-10).
class DataStoreSessionManager(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : SessionManager {
    override suspend fun saveSession(session: UserSession) {
        dataStore.edit { preferences ->
            preferences[SESSION_KEY] = json.encodeToString(session)
        }
    }

    override suspend fun loadSession(): UserSession {
        val stored = dataStore.data.first()[SESSION_KEY] ?: throw NoSessionFoundException()
        return json.decodeFromString(stored)
    }

    override suspend fun deleteSession() {
        dataStore.edit { preferences ->
            preferences.remove(SESSION_KEY)
        }
    }

    private companion object {
        val SESSION_KEY = stringPreferencesKey("supabase_session")
    }
}
