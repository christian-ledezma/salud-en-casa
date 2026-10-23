package bo.saludencasa.features.notifications.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class NotificationPreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun installationId(): String? = dataStore.data.first()[INSTALLATION_ID]

    suspend fun saveInstallationId(installationId: String) {
        dataStore.edit { it[INSTALLATION_ID] = installationId }
    }

    suspend fun clearInstallationId() {
        dataStore.edit { it.remove(INSTALLATION_ID) }
    }

    fun promptAnswered(): Flow<Boolean> = dataStore.data.map { it[PROMPT_ANSWERED] ?: false }

    suspend fun markPromptAnswered() {
        dataStore.edit { it[PROMPT_ANSWERED] = true }
    }

    private companion object {
        val INSTALLATION_ID = stringPreferencesKey("installation_id")
        val PROMPT_ANSWERED = booleanPreferencesKey("notification_prompt_answered")
    }
}
