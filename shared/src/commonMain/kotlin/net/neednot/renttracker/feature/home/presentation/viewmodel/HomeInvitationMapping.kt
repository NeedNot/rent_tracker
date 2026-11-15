package net.neednot.renttracker.feature.home.presentation.viewmodel

import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.HomeInvitation
import net.neednot.renttracker.feature.home.presentation.uistate.DeclineInviteUiState
import net.neednot.renttracker.feature.home.presentation.uistate.InvitationCardUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_decline_message
import net.neednot.renttracker.resources.home_decline_title
import net.neednot.renttracker.resources.home_invitation_as_bookkeeper
import net.neednot.renttracker.resources.home_invitation_as_custom
import net.neednot.renttracker.resources.home_invitation_as_manager
import net.neednot.renttracker.resources.home_invitation_as_viewer
import net.neednot.renttracker.resources.home_invitation_description
import org.jetbrains.compose.resources.StringResource

internal fun HomeInvitation.toCardUiState(isWorking: Boolean): InvitationCardUiState =
    InvitationCardUiState(
        inviteId = inviteId,
        groupName = groupName,
        summary = UiText.Resource(access.invitedAsText(), listOf(invitedByName)),
        description = UiText.Resource(Res.string.home_invitation_description, listOf(groupName)),
        isWorking = isWorking,
    )

internal fun HomeInvitation.toDeclineDialog(): DeclineInviteUiState =
    DeclineInviteUiState(
        inviteId = inviteId,
        title = UiText.Resource(Res.string.home_decline_title, listOf(groupName)),
        message = UiText.Resource(Res.string.home_decline_message, listOf(invitedByName)),
    )

private fun MemberPermissionLabel.invitedAsText(): StringResource =
    when (this) {
        MemberPermissionLabel.Viewer -> Res.string.home_invitation_as_viewer
        MemberPermissionLabel.Bookkeeper -> Res.string.home_invitation_as_bookkeeper
        MemberPermissionLabel.Manager -> Res.string.home_invitation_as_manager
        MemberPermissionLabel.Owner, MemberPermissionLabel.Custom -> Res.string.home_invitation_as_custom
    }
