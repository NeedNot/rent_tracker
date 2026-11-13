package net.neednot.renttracker.core.auth

sealed interface GoogleIdTokenError {
    data object Cancelled : GoogleIdTokenError

    data class Failed(
        val message: String,
    ) : GoogleIdTokenError
}
