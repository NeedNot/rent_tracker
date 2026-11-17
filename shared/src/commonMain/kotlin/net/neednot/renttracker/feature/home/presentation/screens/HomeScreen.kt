package net.neednot.renttracker.feature.home.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.ArchivedRow
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.ConfirmDialog
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EmptyState
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtFab
import net.neednot.renttracker.core.designsystem.components.RtSearchBar
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.SectionHeader
import net.neednot.renttracker.core.designsystem.components.TopBarNavigation
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.plus
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.components.AccountButton
import net.neednot.renttracker.feature.home.presentation.components.GroupCard
import net.neednot.renttracker.feature.home.presentation.components.InvitationActions
import net.neednot.renttracker.feature.home.presentation.components.InvitationCard
import net.neednot.renttracker.feature.home.presentation.components.RejectedPaymentActions
import net.neednot.renttracker.feature.home.presentation.components.RejectedPaymentsCard
import net.neednot.renttracker.feature.home.presentation.components.RejectedPaymentsSheet
import net.neednot.renttracker.feature.home.presentation.uistate.GroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState
import net.neednot.renttracker.feature.home.presentation.uistate.InvitationCardUiState
import net.neednot.renttracker.feature.home.presentation.viewmodel.HomeViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.app_name
import net.neednot.renttracker.resources.home_empty_body
import net.neednot.renttracker.resources.home_empty_title
import net.neednot.renttracker.resources.home_error_title
import net.neednot.renttracker.resources.home_groups_heading
import net.neednot.renttracker.resources.home_invitation_decline
import net.neednot.renttracker.resources.home_invitations_heading
import net.neednot.renttracker.resources.home_new_group
import net.neednot.renttracker.resources.home_search_placeholder
import net.neednot.renttracker.resources.ic_add
import net.neednot.renttracker.resources.ic_home_work
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val FabClearance = 72.dp

@Composable
fun HomeScreen(
    onGroupClicked: (groupId: String) -> Unit,
    onNewGroupClicked: () -> Unit,
    onArchivedGroupsClicked: () -> Unit,
    onAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeContent(
        uiState = uiState,
        invitationActions =
            InvitationActions(
                onAcceptClicked = viewModel::onAcceptClicked,
                onDeclineClicked = viewModel::onDeclineClicked,
                onDeclineConfirmed = viewModel::onDeclineConfirmed,
                onDeclineDismissed = viewModel::onDeclineDismissed,
            ),
        rejectedPaymentActions =
            RejectedPaymentActions(
                onCardClicked = viewModel::onRejectedPaymentsClicked,
                onPaymentDismissed = viewModel::onRejectedPaymentDismissed,
                onSheetDismissed = viewModel::onRejectedPaymentsDismissed,
            ),
        onGroupClicked = onGroupClicked,
        onNewGroupClicked = onNewGroupClicked,
        onArchivedGroupsClicked = onArchivedGroupsClicked,
        onAccountClicked = onAccountClicked,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    invitationActions: InvitationActions,
    rejectedPaymentActions: RejectedPaymentActions,
    onGroupClicked: (groupId: String) -> Unit,
    onNewGroupClicked: () -> Unit,
    onArchivedGroupsClicked: () -> Unit,
    onAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(
                title = stringResource(Res.string.app_name),
                navigation = TopBarNavigation.None,
                actions = { AccountButton(initial = uiState.accountInitial, onClick = onAccountClicked) },
            )
        },
        floatingActionButton = {
            if (uiState is HomeUiState.Content && uiState.canCreateGroup) {
                RtFab(
                    text = stringResource(Res.string.home_new_group),
                    icon = Res.drawable.ic_add,
                    onClick = onNewGroupClicked,
                )
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            uiState.offlineBanner?.let { OfflineBanner(text = it.asString()) }
            HomeList(
                uiState = uiState,
                invitationActions = invitationActions,
                onRejectedPaymentsClicked = rejectedPaymentActions.onCardClicked,
                onGroupClicked = onGroupClicked,
                onNewGroupClicked = onNewGroupClicked,
                onArchivedGroupsClicked = onArchivedGroupsClicked,
            )
        }
    }
    uiState.rejectedPayments?.takeIf { it.isSheetOpen }?.let { rejected ->
        RejectedPaymentsSheet(
            rejectedPayments = rejected,
            onPaymentDismissed = rejectedPaymentActions.onPaymentDismissed,
            onSheetDismissed = rejectedPaymentActions.onSheetDismissed,
        )
    }
    (uiState as? HomeUiState.Content)?.declineDialog?.let { dialog ->
        ConfirmDialog(
            title = dialog.title.asString(),
            text = dialog.message.asString(),
            confirmLabel = stringResource(Res.string.home_invitation_decline),
            onConfirmed = invitationActions.onDeclineConfirmed,
            onDismissed = invitationActions.onDeclineDismissed,
            confirmTone = ActionTone.Destructive,
        )
    }
}

@Composable
private fun HomeList(
    uiState: HomeUiState,
    invitationActions: InvitationActions,
    onRejectedPaymentsClicked: () -> Unit,
    onGroupClicked: (groupId: String) -> Unit,
    onNewGroupClicked: () -> Unit,
    onArchivedGroupsClicked: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ContentLayout.List.padding + PaddingValues(bottom = FabClearance),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
    ) {
        item(key = "search") {
            RtSearchBar(
                placeholder = stringResource(Res.string.home_search_placeholder),
                onClick = UnimplementedAction,
            )
        }
        uiState.rejectedPayments?.let { rejected ->
            item(key = "rejectedPayments") {
                RejectedPaymentsCard(summary = rejected.summary, onClick = onRejectedPaymentsClicked)
            }
        }
        when (uiState) {
            is HomeUiState.Loading -> item(key = "loading") { LoadingIndicator(Modifier.fillParentMaxHeight(0.6f)) }
            is HomeUiState.Empty -> item(key = "empty") { NoGroups(uiState.canCreateGroup, onNewGroupClicked) }
            is HomeUiState.Error -> item(key = "error") { GroupsNotLoaded(uiState.message) }
            is HomeUiState.Content -> groupItems(uiState, invitationActions, onGroupClicked, onArchivedGroupsClicked)
        }
    }
}

private fun LazyListScope.groupItems(
    content: HomeUiState.Content,
    invitationActions: InvitationActions,
    onGroupClicked: (groupId: String) -> Unit,
    onArchivedGroupsClicked: () -> Unit,
) {
    content.syncError?.let { error ->
        item(key = "syncError") { Banner(text = error.asString(), kind = BannerKind.Error) }
    }
    if (content.invitations.isNotEmpty()) {
        item(key = "invitationsHeading") { SectionHeader(stringResource(Res.string.home_invitations_heading)) }
        items(content.invitations, key = { "invitation:${it.inviteId}" }) { invitation ->
            InvitationCard(
                invitation = invitation,
                onAcceptClicked = { invitationActions.onAcceptClicked(invitation.inviteId) },
                onDeclineClicked = { invitationActions.onDeclineClicked(invitation.inviteId) },
            )
        }
    }
    if (content.groups.isNotEmpty()) {
        item(key = "groupsHeading") { SectionHeader(stringResource(Res.string.home_groups_heading)) }
        items(content.groups, key = { it.groupId }) { group ->
            GroupCard(group = group, onClick = { onGroupClicked(group.groupId) })
        }
    }
    content.archivedGroupsRow?.let { row ->
        item(key = "archivedGroups") { ArchivedRow(text = row.asString(), onClick = onArchivedGroupsClicked) }
    }
}

@Composable
private fun NoGroups(
    canCreateGroup: Boolean,
    onNewGroupClicked: () -> Unit,
) {
    EmptyState(
        icon = Res.drawable.ic_home_work,
        title = stringResource(Res.string.home_empty_title),
        text = stringResource(Res.string.home_empty_body),
        action = {
            RtButton(
                text = stringResource(Res.string.home_new_group),
                onClick = onNewGroupClicked,
                variant = ButtonVariant.Outlined,
                enabled = canCreateGroup,
            )
        },
    )
}

@Composable
private fun GroupsNotLoaded(message: UiText) {
    ErrorState(
        title = stringResource(Res.string.home_error_title),
        text = message.asString(),
        onRetryClicked = UnimplementedAction,
    )
}

@Preview
@Composable
private fun HomeContentPreview() {
    RentTrackerTheme {
        HomeContent(
            uiState =
                HomeUiState.Content(
                    accountInitial = "S",
                    invitations =
                        listOf(
                            InvitationCardUiState(
                                inviteId = "i1",
                                groupName = "Oak Ave Duplex",
                                summary = UiText.Plain("Sam Rivera invited you as Bookkeeper"),
                                description = UiText.Plain("Invitation to Oak Ave Duplex"),
                                isWorking = false,
                            ),
                        ),
                    archivedGroupsRow = UiText.Plain("Archived groups (1)"),
                    groups =
                        listOf(
                            GroupCardUiState(
                                groupId = "g1",
                                name = "Maple Court",
                                balance = UiText.Plain("Paid up"),
                                isPaidUp = true,
                                tenantCount = UiText.Plain("4 tenants · none late"),
                                lateCount = null,
                                archivedOwed = null,
                            ),
                        ),
                ),
            invitationActions = InvitationActions.None,
            rejectedPaymentActions = RejectedPaymentActions.None,
            onGroupClicked = {},
            onNewGroupClicked = {},
            onArchivedGroupsClicked = {},
            onAccountClicked = {},
        )
    }
}
