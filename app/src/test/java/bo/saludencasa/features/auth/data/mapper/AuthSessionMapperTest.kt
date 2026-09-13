package bo.saludencasa.features.auth.data.mapper

import bo.saludencasa.features.auth.domain.model.SessionState
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthSessionMapperTest {
    @Test
    fun `reads the name Google stores under full_name`() {
        val user = userInfo(metadata = buildJsonObject { put("full_name", "Ana Quispe") })

        assertEquals("Ana Quispe", user.toAuthSession().fullName?.value)
    }

    // The same claim reaches the metadata as name when the token came from a
    // scope that does not carry full_name, so reading only one spelling leaves
    // half the accounts without a name for no visible reason.
    @Test
    fun `falls back to the name claim when full_name is absent`() {
        val user = userInfo(metadata = buildJsonObject { put("name", "Ana Quispe") })

        assertEquals("Ana Quispe", user.toAuthSession().fullName?.value)
    }

    @Test
    fun `reports no name when the metadata carries neither spelling`() {
        val user = userInfo(metadata = buildJsonObject { put("avatar_url", "https://example.com/a.png") })

        assertNull(user.toAuthSession().fullName)
    }

    // Supabase already accepted the token, so a value this project's rules
    // reject must cost the name, never the session.
    @Test
    fun `keeps the session when the provider sends a name the rules reject`() {
        val user = userInfo(metadata = buildJsonObject { put("full_name", "Ana Quispe 2") })

        val session = user.toAuthSession()

        assertEquals("08ddb28f-0000-4000-8000-000000000000", session.userId)
        assertNull(session.fullName)
    }

    @Test
    fun `keeps the session when the provider sends no email`() {
        val session = userInfo(email = null).toAuthSession()

        assertNull(session.email)
        assertEquals("08ddb28f-0000-4000-8000-000000000000", session.userId)
    }

    @Test
    fun `reports the restoring session as still loading`() {
        assertEquals(SessionState.Loading, SessionStatus.Initializing.toSessionState())
    }

    @Test
    fun `reports an absent session as signed out`() {
        assertEquals(SessionState.SignedOut, SessionStatus.NotAuthenticated(isSignOut = true).toSessionState())
    }

    // An authenticated status whose session carries no user is not somebody
    // signed in: treating it as one leaves the application with a screen that
    // has nobody to show.
    @Test
    fun `reports an authenticated status with no user as signed out`() {
        val status = SessionStatus.Authenticated(session = userSession(user = null))

        assertEquals(SessionState.SignedOut, status.toSessionState())
    }

    @Test
    fun `reports an authenticated status with a user as signed in`() {
        val status = SessionStatus.Authenticated(session = userSession(user = userInfo()))

        assertEquals(SessionState.SignedIn(userInfo().toAuthSession()), status.toSessionState())
    }
}

private fun userInfo(
    email: String? = "Ana.Quispe@Example.com",
    metadata: JsonObject = buildJsonObject { put("full_name", "Ana Quispe") },
): UserInfo =
    UserInfo(
        aud = "authenticated",
        id = "08ddb28f-0000-4000-8000-000000000000",
        email = email,
        userMetadata = metadata,
    )

private fun userSession(user: UserInfo?): UserSession =
    UserSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 3_600,
        tokenType = "bearer",
        user = user,
    )
