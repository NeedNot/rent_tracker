package net.neednot.renttracker.feature.members.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RtFab
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.SectionHeader
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.plus
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.components.InviteRow
import net.neednot.renttracker.feature.members.presentation.components.MemberRow
import net.neednot.renttracker.feature.members.presentation.components.RemoveMemberDialog
import net.neednot.renttracker.feature.members.presentation.uistate.InviteRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowAction
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MembersUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.MembersViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_add
import net.neednot.renttracker.resources.ic_chevron_right
import net.neednot.renttracker.resources.ic_swap_horiz
import net.neednot.renttracker.resources.ic_visibility
import net.neednot.renttracker.resources.members_error_title
import net.neednot.renttracker.resources.members_invite_title
import net.neednot.renttracker.resources.members_pending_invites
import net.neednot.renttracker.resources.members_title
import net.neednot.renttracker.resources.members_transfer_ownership
import net.neednot.renttracker.resources.members_view_only_info
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MembersScreen(
    groupId: String,
    onBackClicked: () -> Unit,
    onInviteClicked: () -> Unit,
    onMemberClicked: (membershipId: String) -> Unit,
    onTransferOwnershipClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = koinViewModel { parametersOf(groupId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    MembersContent(
        uiState = uiState,
        actions =
            MembersActions(
                onBackClicked = onBackClicked,
                onInviteClicked = onInviteClicked,
                onMemberClicked = onMemberClicked,
                onTransferOwnershipClicked = onTransferOwnershipClicked,
                onRemoveClicked = viewModel::onRemoveClicked,
                onRemoveConfirmed = viewModel::onRemoveConfirmed,
                onRemoveDismissed = viewModel::onRemoveDismissed,
                onRevokeClicked = viewModel::onRevokeClicked,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun MembersContent(
    uiState: MembersUiState,
    actions: MembersActions,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? MembersUiState.Content
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(
                title = stringResource(Res.string.members_title),
                subtitle = content?.groupName,
                onNavigationClicked = actions.onBackClicked,
            )
        },
        floatingActionButton = {
            if (content?.canInvite == true) {
                RtFab(
                    text = stringResource(Res.string.members_invite_title),
                    icon = Res.drawable.ic_add,
                    onClick = actions.onInviteClicked,
                )
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            content?.offlineBanner?.let { OfflineBanner(text = it.asString()) }
            when (uiState) {
                MembersUiState.Loading -> {
                    LoadingIndicator()
                }

                is MembersUiState.Error -> {
                    ErrorState(
                        title = stringResource(Res.string.members_error_title),
                        text = uiState.message.asString(),
                        onRetryClicked = UnimplementedAction,
                    )
                }

                is MembersUiState.Content -> {
                    MembersList(content = uiState, actions = actions)
                }
            }
        }
    }
    content?.removeDialog?.let { dialog ->
        RemoveMemberDialog(
            dialog = dialog,
            onConfirmed = actions.onRemoveConfirmed,
            onDismissed = actions.onRemoveDismissed,
        )
    }
}

@Composable
private fun MembersList(
    content: MembersUiState.Content,
    actions: MembersActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ContentLayout.List.padding + PaddingValues(bottom = Sizes.fabClearance),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
    ) {
        content.syncError?.let { error ->
            item(key = "syncError") { Banner(text = error.asString(), kind = BannerKind.Error) }
        }
        if (content.showsViewOnlyInfo) {
            item(key = "viewOnlyInfo") {
                Banner(
                    text = stringResource(Res.string.members_view_only_info),
                    kind = BannerKind.Info,
                    icon = Res.drawable.ic_visibility,
                )
            }
        }
        item(key = "members") {
            RtListCard(items = content.members) { member ->
                MemberRow(
                    member = member,
                    onClick = { actions.onMemberClicked(member.membershipId) },
                    onRemoveClicked = { actions.onRemoveClicked(member.membershipId) },
                )
            }
        }
        inviteItems(content.invites, actions)
        if (content.canTransferOwnership) {
            item(key = "transfer") { TransferOwnershipLink(onClick = actions.onTransferOwnershipClicked) }
        }
    }
}

private fun LazyListScope.inviteItems(
    invites: List<InviteRowUiState>,
    actions: MembersActions,
) {
    if (invites.isEmpty()) return
    item(key = "invitesHeading") { SectionHeader(text = stringResource(Res.string.members_pending_invites)) }
    item(key = "invites") {
        RtListCard(items = invites) { invite ->
            InviteRow(invite = invite, onRevokeClicked = { actions.onRevokeClicked(invite.inviteId) })
        }
    }
}

@Composable
private fun TransferOwnershipLink(onClick: () -> Unit) {
    RtListCard(items = listOf(Unit)) {
        RtListItem(
            text = stringResource(Res.string.members_transfer_ownership),
            leadingIcon = Res.drawable.ic_swap_horiz,
            onClick = onClick,
            trailing = {
                AppIcon(
                    icon = Res.drawable.ic_chevron_right,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@Preview
@Composable
private fun MembersContentPreview() {
    RentTrackerTheme {
        MembersContent(
            uiState =
                MembersUiState.Content(
                    groupName = "1st Street",
                    showsViewOnlyInfo = false,
                    members =
                        listOf(
                            MemberRowUiState(
                                membershipId = "m1",
                                initials = "NN",
                                name = "Need_Not",
                                email = "you@example.com",
                                isYou = true,
                                label = MemberPermissionLabel.Owner,
                                action = MemberRowAction.None,
                            ),
                            MemberRowUiState(
                                membershipId = "m2",
                                initials = "SR",
                                name = "Sam Rivera",
                                email = "sam.rivera@example.com",
                                isYou = false,
                                label = MemberPermissionLabel.Manager,
                                action = MemberRowAction.OpenPermissions,
                            ),
                        ),
                    invites =
                        listOf(
                            InviteRowUiState(
                                inviteId = "i1",
                                email = "alex.m@example.com",
                                label = MemberPermissionLabel.Bookkeeper,
                                expiresOn = "Mar 16",
                                canRevoke = true,
                            ),
                        ),
                    canInvite = true,
                    canTransferOwnership = false,
                ),
            actions = MembersActions(),
        )
    }
}
