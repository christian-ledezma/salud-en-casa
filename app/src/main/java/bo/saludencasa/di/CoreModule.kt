package bo.saludencasa.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import bo.saludencasa.BuildConfig
import bo.saludencasa.core.network.DataStoreSessionManager
import bo.saludencasa.core.network.GoogleAuthClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// DataStore is created once per process, which is what this delegate guarantees.
private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Cross cutting dependencies: the Supabase client, the session storage and the
 * JSON format they share. One Koin module per feature is the convention; this
 * one is what every feature stands on.
 *
 * Scopes follow .claude/rules/arquitectura.md: data sources and repositories are
 * singletons because they hold a connection or shared state, use cases are
 * factories because they hold none.
 */
val coreModule =
    module {

        single {
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }

        single<DataStore<Preferences>> { androidContext().sessionStore }

        single { DataStoreSessionManager(dataStore = get(), json = get()) }

        single<SupabaseClient> {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                // The anonymous key grants nothing on its own: the row level
                // security policy is what concedes or denies. The service key never
                // leaves the server (INV-14).
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth) {
                    // RF-01.6: the session survives a restart and the token renews
                    // on its own. Both are on by default; they are written out here
                    // because the requirement depends on them staying that way.
                    sessionManager = get<DataStoreSessionManager>()
                    autoLoadFromStorage = true
                    autoSaveToStorage = true
                    alwaysAutoRefresh = true
                }
                install(Postgrest)
                install(Realtime)
                install(Storage)
            }
        }

        single { GoogleAuthClient(supabase = get(), webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID) }
    }
