package net.neednot.renttracker.feature.members.presentation.viewmodel

import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.members.domain.Member
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersOverview
import net.neednot.renttracker.feature.members.presentation.uistate.TransferCandidateUiState
import net.neednot.renttracker.feature.members.presentation.uistate.TransferOwnershipUiState

internal fun Synced<MembersOverview?, MembersError>.toTransferOwnershipUiState(
    choice: TransferOwnershipUiState,
): TransferOwnershipUiState {
    val overview = value
    if (overview == null) {
        val isLoading = status == SyncStatus.Syncing
        return choice.copy(isLoading = isLoading, error = choice.error ?: loadErrorText().takeUnless { isLoading })
    }
    val candidates =
        overview.members.filterNot { overview.access.isYou(it.accountId) }.map { it.toCandidateUiState() }
    return choice.copy(
        isLoading = false,
        groupName = overview.groupName,
        candidates = candidates,
        selectedMembershipId = choice.selectedMembershipId?.takeIf { id -> candidates.any { it.membershipId == id } },
        error = choice.error ?: syncErrorText(),
    )
}

private fun Member.toCandidateUiState(): TransferCandidateUiState =
    TransferCandidateUiState(
        membershipId = membershipId,
        initials = initialsOf(displayName),
        name = displayName,
        label = MemberPermissionLabel.of(role, permissions),
    )
