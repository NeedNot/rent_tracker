package net.neednot.renttracker.core.auth

sealed interface AuthSessionState {
    data object Loading : AuthSessionState

    data object SignedOut : AuthSessionState

    data class SignedIn(
        val userId: String,
    ) : AuthSessionState
}
