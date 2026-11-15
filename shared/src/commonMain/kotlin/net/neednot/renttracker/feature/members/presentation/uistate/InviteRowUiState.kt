package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.access.MemberPermissionLabel

data class InviteRowUiState(
    val inviteId: String,
    val email: String,
    val label: MemberPermissionLabel,
    val expiresOn: String,
    val canRevoke: Boolean,
)
