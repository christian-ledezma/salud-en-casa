package bo.saludencasa.features.auth.domain.repository

import bo.saludencasa.features.auth.domain.model.AuthResult
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.model.SignOutResult
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    fun observeSession(): Flow<SessionState>

    suspend fun signInWithGoogle(
        idToken: String,
        rawNonce: String,
    ): AuthResult

    suspend fun signOut(): SignOutResult
}
