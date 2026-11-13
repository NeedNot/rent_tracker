package net.neednot.renttracker.feature.auth.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class SignInCodeUiState(
    val email: String,
    val error: UiText? = null,
    val isVerifying: Boolean = false,
    val isSendingNewCode: Boolean = false,
    val secondsUntilNewCode: Int = 0,
) {
    val isBusy: Boolean get() = isVerifying || isSendingNewCode
    val canSendNewCode: Boolean get() = !isBusy && secondsUntilNewCode == 0
}
