package net.neednot.renttracker.feature.auth.domain

sealed interface AuthError {
    data object InvalidCode : AuthError

    data object CodeExpired : AuthError

    data object TooManyAttempts : AuthError

    data class RateLimited(
        val retryAfterSeconds: Int?,
    ) : AuthError

    data object Offline : AuthError

    data object SessionExpired : AuthError

    data object GoogleSignInCancelled : AuthError

    data object GoogleSignInFailed : AuthError

    data object InvalidDisplayName : AuthError

    data class Unexpected(
        val message: String,
    ) : AuthError
}
