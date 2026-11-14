package net.neednot.renttracker.core.auth

sealed interface AuthSessionError {
    data object NotSignedIn : AuthSessionError

    data object SessionExpired : AuthSessionError

    data object InvalidCode : AuthSessionError

    data object CodeExpired : AuthSessionError

    data object TooManyAttempts : AuthSessionError

    data class RateLimited(
        val retryAfterSeconds: Int?,
    ) : AuthSessionError

    data object Network : AuthSessionError

    data object Storage : AuthSessionError

    data class Server(
        val status: Int,
        val code: String?,
        val message: String?,
    ) : AuthSessionError

    data class UnexpectedResponse(
        val message: String,
    ) : AuthSessionError
}
