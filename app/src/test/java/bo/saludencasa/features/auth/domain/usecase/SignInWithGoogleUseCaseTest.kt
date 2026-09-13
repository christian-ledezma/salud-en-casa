package bo.saludencasa.features.auth.domain.usecase

import bo.saludencasa.features.auth.FakeAuthRepository
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SignInWithGoogleUseCaseTest {
    // Credential Manager reports success with an empty token when the chooser
    // returns a credential it could not read. Forwarding it costs a round trip
    // and comes back blaming the provider configuration instead.
    @Test
    fun `rejects a blank identity token without reaching the remote`() =
        runTest {
            val repository = FakeAuthRepository()

            val result = SignInWithGoogleUseCase(repository)(idToken = "  ", rawNonce = "abc")

            assertEquals(AuthResult.Failure(AuthError.CredentialProviderFailure), result)
            assertEquals(0, repository.signInAttempts)
        }

    @Test
    fun `rejects a blank nonce without reaching the remote`() =
        runTest {
            val repository = FakeAuthRepository()

            val result = SignInWithGoogleUseCase(repository)(idToken = "token", rawNonce = "")

            assertEquals(AuthResult.Failure(AuthError.CredentialProviderFailure), result)
            assertEquals(0, repository.signInAttempts)
        }

    // The nonce travels in two forms and the token in one. Swapping the two
    // arguments produces a rejection Supabase reports as an invalid nonce, with
    // nothing in the message pointing at the call site
    // (docs/decisions.md, 2026-09-12).
    @Test
    fun `hands the token and the raw nonce to the repository in that order`() =
        runTest {
            val repository = FakeAuthRepository()

            SignInWithGoogleUseCase(repository)(idToken = "the-token", rawNonce = "the-raw-nonce")

            assertEquals("the-token", repository.lastIdToken)
            assertEquals("the-raw-nonce", repository.lastRawNonce)
        }

    @Test
    fun `passes the failure of the repository through as its own`() =
        runTest {
            val repository = FakeAuthRepository(signInResult = AuthResult.Failure(AuthError.NetworkUnavailable))

            val result = SignInWithGoogleUseCase(repository)(idToken = "token", rawNonce = "nonce")

            assertEquals(AuthResult.Failure(AuthError.NetworkUnavailable), result)
        }
}
