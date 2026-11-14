package net.neednot.renttracker.feature.auth.presentation.viewmodel

sealed interface SignInEvent {
    data class CodeSent(
        val email: String,
    ) : SignInEvent

    data class SignedIn(
        val destination: SignedInDestination,
    ) : SignInEvent
}
