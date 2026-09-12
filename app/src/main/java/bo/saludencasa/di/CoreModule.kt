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

// preferencesDataStore guarantees a single instance per process.
private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")

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
                // Anonymous key only; row level security decides access, not the
                // key itself (docs/decisions.md, 2026-09-05). INV-14.
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth) {
                    // Written out even though they are the defaults: RF-01.6
                    // depends on them staying this way.
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
