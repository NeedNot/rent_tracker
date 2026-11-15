package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.access.MemberPermissionLabel

data class MemberRowUiState(
    val membershipId: String,
    val initials: String,
    val name: String,
    val email: String?,
    val isYou: Boolean,
    val label: MemberPermissionLabel,
    val action: MemberRowAction,
) {
    val isOwner: Boolean get() = label == MemberPermissionLabel.Owner
}
