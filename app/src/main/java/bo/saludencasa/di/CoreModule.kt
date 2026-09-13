package bo.saludencasa.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import bo.saludencasa.BuildConfig
import bo.saludencasa.core.network.DataStoreSessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

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
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth) {
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
    }
