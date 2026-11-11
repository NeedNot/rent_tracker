package net.neednot.renttracker.core.backend

sealed interface BackendAuthState {
    data object SignedOut : BackendAuthState

    data object Loading : BackendAuthState

    data object SignedIn : BackendAuthState
}
