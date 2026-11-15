package net.neednot.renttracker.feature.members.presentation.viewmodel

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.domain.Member
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersOverview
import net.neednot.renttracker.feature.members.domain.PendingInvite
import net.neednot.renttracker.feature.members.presentation.uistate.InviteRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowAction
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MembersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.RemoveMemberDialogUiState

internal fun Synced<MembersOverview?, MembersError>.toMembersUiState(
    timeZone: TimeZone,
    removeDialog: RemoveMemberDialogUiState?,
): MembersUiState {
    val overview = value
    return when {
        overview != null -> overview.toContent(timeZone, syncErrorText(), removeDialog)
        status == SyncStatus.Syncing -> MembersUiState.Loading
        else -> MembersUiState.Error(loadErrorText())
    }
}

private fun MembersOverview.toContent(
    timeZone: TimeZone,
    syncError: UiText?,
    removeDialog: RemoveMemberDialogUiState?,
): MembersUiState.Content =
    MembersUiState.Content(
        groupName = groupName,
        showsViewOnlyInfo = !access.canManageMembers,
        members = members.map { it.toRowUiState(access) },
        invites = invites.map { it.toRowUiState(access, timeZone) },
        canInvite = access.canManageMembers,
        canTransferOwnership = access.isOwner && access.canEditPermissions && members.size > 1,
        syncError = syncError,
        removeDialog = removeDialog,
    )

private fun Member.toRowUiState(access: CallerAccess): MemberRowUiState =
    MemberRowUiState(
        membershipId = membershipId,
        initials = initialsOf(displayName),
        name = displayName,
        email = email,
        isYou = access.isYou(accountId),
        label = MemberPermissionLabel.of(role, permissions),
        action = rowAction(access),
    )

private fun Member.rowAction(access: CallerAccess): MemberRowAction =
    when {
        role == MembershipRole.Owner || access.isYou(accountId) -> MemberRowAction.None
        access.canEditPermissions -> MemberRowAction.OpenPermissions
        access.canRemove(role, permissions, accountId) -> MemberRowAction.Remove
        else -> MemberRowAction.None
    }

private fun PendingInvite.toRowUiState(
    access: CallerAccess,
    timeZone: TimeZone,
): InviteRowUiState =
    InviteRowUiState(
        inviteId = inviteId,
        email = email,
        label = MemberPermissionLabel.of(MembershipRole.Member, permissions),
        expiresOn = formatMonthDay(expiresAt.toLocalDateTime(timeZone).date),
        canRevoke = access.canManageMembers,
    )
