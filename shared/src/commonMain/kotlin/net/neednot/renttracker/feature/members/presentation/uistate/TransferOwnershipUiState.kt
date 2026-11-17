package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class TransferOwnershipUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    val candidates: List<TransferCandidateUiState> = emptyList(),
    val confirm: TransferConfirmUiState? = null,
    val error: UiText? = null,
)
