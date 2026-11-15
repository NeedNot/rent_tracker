package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface PendingBannerUiState {
    val title: UiText

    data class Waiting(
        override val title: UiText,
        val subtitle: UiText,
    ) : PendingBannerUiState

    data class Attention(
        override val title: UiText,
        val reviewClientId: String,
    ) : PendingBannerUiState
}
