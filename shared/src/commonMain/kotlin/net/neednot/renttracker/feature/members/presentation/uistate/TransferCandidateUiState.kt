package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.access.MemberPermissionLabel

data class TransferCandidateUiState(
    val membershipId: String,
    val initials: String,
    val name: String,
    val label: MemberPermissionLabel,
)
