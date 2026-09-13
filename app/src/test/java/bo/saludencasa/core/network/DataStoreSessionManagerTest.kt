package bo.saludencasa.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreSessionManagerTest {
    // RF-01.6. A second manager over the same store is what a relaunch looks
    // like from here: the process is gone, the stored bytes are not. What this
    // catches is the session failing to survive the serialization round trip,
    // which on a device looks like being asked to sign in again every morning.
    @Test
    fun `session persists across application restart`() =
        runTest {
            val store = InMemoryPreferences()
            manager(store).saveSession(session)

            assertEquals(session, manager(store).loadSession())
        }

    @Test
    fun `reports no session once it has been deleted`() =
        runTest {
            val store = InMemoryPreferences()
            manager(store).saveSession(session)
            manager(store).deleteSession()

            assertNoSessionFound(runCatching { manager(store).loadSession() })
        }

    @Test
    fun `reports no session on a store that never held one`() =
        runTest {
            assertNoSessionFound(runCatching { manager(InMemoryPreferences()).loadSession() })
        }
}

private fun assertNoSessionFound(result: Result<*>) {
    val failure = result.exceptionOrNull()
    assertTrue("Expected NoSessionFoundException, got $result", failure is NoSessionFoundException)
}

private val session =
    UserSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 3_600,
        tokenType = "bearer",
    )

private fun manager(store: DataStore<Preferences>): DataStoreSessionManager =
    DataStoreSessionManager(
        dataStore = store,
        json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
    )

private class InMemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}
