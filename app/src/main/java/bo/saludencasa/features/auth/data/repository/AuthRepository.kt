package bo.saludencasa.features.auth.data.repository

import bo.saludencasa.features.auth.data.datasource.SupabaseAuthDataSource
import bo.saludencasa.features.auth.data.mapper.toAuthError
import bo.saludencasa.features.auth.data.mapper.toAuthSession
import bo.saludencasa.features.auth.data.mapper.toSessionState
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthResult
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.auth.domain.repository.IAuthRepository
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
        } catch (failure: Exception) {
            AuthResult.Failure(failure.toAuthError())
        }
    }

    override suspend fun signOut(): SignOutResult =
        try {
            dataSource.signOut()
            SignOutResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            when (failure.toAuthError()) {
                AuthError.NetworkUnavailable -> {
                    dataSource.clearStoredSession()
                    SignOutResult.Success
                }

                else -> {
                    SignOutResult.Failure(AuthError.Unexpected)
                }
            }
        }
}
