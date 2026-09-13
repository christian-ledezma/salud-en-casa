package bo.saludencasa.features.auth

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.features.auth.domain.model.AuthResult
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.auth.domain.model.SessionState
import bo.saludencasa.features.auth.domain.model.SignOutResult
import bo.saludencasa.features.auth.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAuthRepository(
    private val sessions: Flow<SessionState> = flowOf(SessionState.SignedOut),
    private val signInResult: AuthResult = AuthResult.Success(authSession()),
    var signOutResult: SignOutResult = SignOutResult.Success,
) : IAuthRepository {
    var signInAttempts: Int = 0
        private set
    var lastIdToken: String? = null
        private set
    var lastRawNonce: String? = null
        private set
    var signOutAttempts: Int = 0
        private set

    override fun observeSession(): Flow<SessionState> = sessions

    override suspend fun signInWithGoogle(
        idToken: String,
        rawNonce: String,
    ): AuthResult {
        signInAttempts++
        lastIdToken = idToken
        lastRawNonce = rawNonce
        return signInResult
    }

    override suspend fun signOut(): SignOutResult {
        signOutAttempts++
        return signOutResult
    }
}

fun authSession(
    userId: String = "08ddb28f-0000-4000-8000-000000000000",
    email: String = "ana.quispe@example.com",
    fullName: String = "Ana Quispe",
): AuthSession =
    AuthSession(
        userId = userId,
        email = Email.create(email).getOrNull(),
        fullName = PersonName.create(fullName).getOrNull(),
    )
