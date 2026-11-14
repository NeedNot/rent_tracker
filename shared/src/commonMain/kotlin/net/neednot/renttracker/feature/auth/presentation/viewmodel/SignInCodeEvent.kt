package net.neednot.renttracker.feature.auth.presentation.viewmodel

sealed interface SignInCodeEvent {
    data class SignedIn(
        val destination: SignedInDestination,
    ) : SignInCodeEvent
}
