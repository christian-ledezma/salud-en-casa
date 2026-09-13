package bo.saludencasa.features.auth.data.repository

import bo.saludencasa.features.auth.data.datasource.SupabaseAuthDataSource
import bo.saludencasa.features.auth.data.mapper.toAuthSession
import bo.saludencasa.features.auth.data.mapper.toSessionState
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthResult
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.auth.domain.repository.IAuthRepository
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val dataSource: SupabaseAuthDataSource,
) : IAuthRepository {
    override fun observeSession(): Flow<SessionState> = dataSource.sessionStatus.map { it.toSessionState() }

    override suspend fun signInWithGoogle(
        idToken: String,
        rawNonce: String,
    ): AuthResult {
        if (!dataSource.isConfigured) return AuthResult.Failure(AuthError.MissingConfiguration)

        return try {
            dataSource
                .signInWithGoogle(idToken = idToken, rawNonce = rawNonce)
                ?.let { AuthResult.Success(it.toAuthSession()) }
                ?: AuthResult.Failure(AuthError.TokenRejected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: HttpRequestException) {
            AuthResult.Failure(AuthError.NetworkUnavailable)
        } catch (_: RestException) {
            AuthResult.Failure(AuthError.TokenRejected)
        } catch (_: Exception) {
            AuthResult.Failure(AuthError.Unexpected)
        }
    }

    override suspend fun signOut(): SignOutResult =
        try {
            dataSource.signOut()
            SignOutResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: HttpRequestException) {
            // The client keeps the stored session when the logout request never
            // reaches the server. Clearing it here is what makes RF-01.7 hold
            // with no connection: the sign out is local in scope anyway.
            dataSource.clearStoredSession()
            SignOutResult.Success
        } catch (_: Exception) {
            SignOutResult.Failure(AuthError.Unexpected)
        }
}
