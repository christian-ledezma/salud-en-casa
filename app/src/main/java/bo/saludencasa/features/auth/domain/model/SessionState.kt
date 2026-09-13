package bo.saludencasa.features.auth.domain.model

sealed interface SessionState {
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(
        val session: AuthSession,
    ) : SessionState
}
