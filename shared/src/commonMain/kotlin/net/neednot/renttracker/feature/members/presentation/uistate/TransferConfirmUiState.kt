package net.neednot.renttracker.feature.members.presentation.uistate

data class TransferConfirmUiState(
    val membershipId: String,
    val newOwnerName: String,
    val newOwnerFirstName: String,
    val groupName: String,
    val isWorking: Boolean = false,
)
