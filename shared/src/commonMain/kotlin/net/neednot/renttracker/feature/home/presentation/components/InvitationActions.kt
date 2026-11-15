package net.neednot.renttracker.feature.home.presentation.components

data class InvitationActions(
    val onAcceptClicked: (inviteId: String) -> Unit,
    val onDeclineClicked: (inviteId: String) -> Unit,
    val onDeclineConfirmed: () -> Unit,
    val onDeclineDismissed: () -> Unit,
) {
    companion object {
        val None = InvitationActions({}, {}, {}, {})
    }
}
