package bo.saludencasa.features.auth.data.datasource

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow

class SupabaseAuthDataSource(
    private val supabase: SupabaseClient,
    private val supabaseUrl: String,
    private val supabaseAnonKey: String,
) {
    // A clean checkout compiles with empty secrets on purpose, so the absence
    // is detected here and reported as configuration rather than as a rejected
    // token (README, "Archivos que no estan en el repositorio").
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    val sessionStatus: Flow<SessionStatus>
        get() = supabase.auth.sessionStatus

    suspend fun signInWithGoogle(
        idToken: String,
        rawNonce: String,
    ): UserInfo? {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            // The raw value, never the hash: Google signs over the digest and
            // Supabase verifies the original (docs/decisions.md, 2026-09-12).
            this.nonce = rawNonce
        }
        return supabase.auth.currentUserOrNull()
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    suspend fun clearStoredSession() {
        supabase.auth.clearSession()
    }
}
