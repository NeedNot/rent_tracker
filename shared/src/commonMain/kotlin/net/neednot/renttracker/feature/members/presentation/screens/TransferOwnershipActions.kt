package net.neednot.renttracker.feature.members.presentation.screens

internal data class TransferOwnershipActions(
    val onCandidateClicked: (membershipId: String) -> Unit = {},
    val onTransferConfirmed: () -> Unit = {},
    val onConfirmDismissed: () -> Unit = {},
    val onInviteClicked: () -> Unit = {},
    val onCloseClicked: () -> Unit = {},
)
