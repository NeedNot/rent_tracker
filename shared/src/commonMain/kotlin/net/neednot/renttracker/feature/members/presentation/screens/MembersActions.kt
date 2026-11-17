package net.neednot.renttracker.feature.members.presentation.screens

internal data class MembersActions(
    val onBackClicked: () -> Unit = {},
    val onInviteClicked: () -> Unit = {},
    val onMemberClicked: (membershipId: String) -> Unit = {},
    val onTransferOwnershipClicked: () -> Unit = {},
    val onRemoveClicked: (membershipId: String) -> Unit = {},
    val onRemoveConfirmed: () -> Unit = {},
    val onRemoveDismissed: () -> Unit = {},
    val onRevokeClicked: (inviteId: String) -> Unit = {},
)
