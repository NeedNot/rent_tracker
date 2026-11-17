package net.neednot.renttracker.feature.members.presentation.uistate

data class TransferCandidateUiState(
    val membershipId: String,
    val initials: String,
    val name: String,
    val email: String?,
)
