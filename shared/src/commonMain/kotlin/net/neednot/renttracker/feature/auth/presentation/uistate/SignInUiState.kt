package net.neednot.renttracker.feature.auth.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class SignInUiState(
    val email: String = "",
    val emailError: UiText? = null,
    val error: UiText? = null,
    val isSendingCode: Boolean = false,
    val isSigningInWithGoogle: Boolean = false,
) {
    val isBusy: Boolean get() = isSendingCode || isSigningInWithGoogle
}
