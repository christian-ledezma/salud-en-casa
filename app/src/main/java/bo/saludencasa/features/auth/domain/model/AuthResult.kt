package bo.saludencasa.features.auth.domain.model

sealed interface AuthResult {
    data class Success(
        val session: AuthSession,
    ) : AuthResult

    data class Failure(
        val error: AuthError,
    ) : AuthResult
}

sealed interface SignOutResult {
    data object Success : SignOutResult

    data class Failure(
        val error: AuthError,
    ) : SignOutResult
}
