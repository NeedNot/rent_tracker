package net.neednot.renttracker.feature.home.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.components.AccountButton
import net.neednot.renttracker.feature.home.presentation.components.ArchivedGroupsRow
import net.neednot.renttracker.feature.home.presentation.components.GroupCard
import net.neednot.renttracker.feature.home.presentation.components.InvitationActions
import net.neednot.renttracker.feature.home.presentation.components.InvitationCard
import net.neednot.renttracker.feature.home.presentation.components.LateRentAlertsDialog
import net.neednot.renttracker.feature.home.presentation.components.RejectedPaymentActions
import net.neednot.renttracker.feature.home.presentation.components.RejectedPaymentsCard
import net.neednot.renttracker.feature.home.presentation.components.RejectedPaymentsSheet
import net.neednot.renttracker.feature.home.presentation.uistate.DeclineInviteUiState
import net.neednot.renttracker.feature.home.presentation.uistate.GroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState
import net.neednot.renttracker.feature.home.presentation.uistate.InvitationCardUiState
import net.neednot.renttracker.feature.home.presentation.viewmodel.HomeViewModel
import net.neednot.renttracker.feature.home.presentation.viewmodel.LateRentAlertsAnswer
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.app_name
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.home_empty_body
import net.neednot.renttracker.resources.home_empty_title
import net.neednot.renttracker.resources.home_groups_heading
import net.neednot.renttracker.resources.home_invitation_decline
import net.neednot.renttracker.resources.home_invitations_heading
import net.neednot.renttracker.resources.home_loading
import net.neednot.renttracker.resources.home_new_group
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
        onLateRentAlertsAnswered = viewModel::onLateRentAlertsAnswered,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    invitationActions: InvitationActions,
    rejectedPaymentActions: RejectedPaymentActions,
    onGroupClicked: (groupId: String) -> Unit,
    onNewGroupClicked: () -> Unit,
    onArchivedGroupsClicked: () -> Unit,
    onAccountClicked: () -> Unit,
    onLateRentAlertsAnswered: (LateRentAlertsAnswer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = { AccountButton(initial = uiState.accountInitial, onClick = onAccountClicked) },
            )
        },
        floatingActionButton = {
            if (uiState.canCreateGroup) {
                ExtendedFloatingActionButton(
                    onClick = onNewGroupClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.home_new_group)) },
                )
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            uiState.offlineBanner?.let { OfflineBanner(text = it.asString()) }
            uiState.rejectedPayments?.let { rejected ->
                RejectedPaymentsCard(
                    summary = rejected.summary,
                    onClick = rejectedPaymentActions.onCardClicked,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                )
            }
            HomeBody(
                uiState = uiState,
                invitationActions = invitationActions,
                onGroupClicked = onGroupClicked,
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
    if ((uiState as? HomeUiState.Content)?.isLateRentPromptShown == true) {
        LateRentAlertsDialog(
            onAllowed = { onLateRentAlertsAnswered(LateRentAlertsAnswer.Allow) },
            onDeclined = { onLateRentAlertsAnswered(LateRentAlertsAnswer.NotNow) },
        )
    }
    (uiState as? HomeUiState.Content)?.declineDialog?.let { dialog ->
        DeclineInviteDialog(
            dialog = dialog,
            onConfirmed = invitationActions.onDeclineConfirmed,
            onDismissed = invitationActions.onDeclineDismissed,
        )
    }
}

@Composable
private fun HomeBody(
    uiState: HomeUiState,
    invitationActions: InvitationActions,
    onGroupClicked: (groupId: String) -> Unit,
    onArchivedGroupsClicked: () -> Unit,
) {
    when (uiState) {
        is HomeUiState.Loading -> {
            Loading()
        }

        is HomeUiState.Empty -> {
            Message(
                title = stringResource(Res.string.home_empty_title),
                body = stringResource(Res.string.home_empty_body),
            )
        }

        is HomeUiState.Error -> {
            Message(title = uiState.message.asString(), body = null)
        }

        is HomeUiState.Content -> {
            HomeList(
                content = uiState,
                invitationActions = invitationActions,
                onGroupClicked = onGroupClicked,
                onArchivedGroupsClicked = onArchivedGroupsClicked,
            )
        }
    }
}

@Composable
private fun HomeList(
    content: HomeUiState.Content,
    invitationActions: InvitationActions,
    onGroupClicked: (groupId: String) -> Unit,
    onArchivedGroupsClicked: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content.syncError?.let { error ->
            item(key = "syncError") {
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (content.invitations.isNotEmpty()) {
            item(key = "invitationsHeading") { SectionHeading(stringResource(Res.string.home_invitations_heading)) }
            items(content.invitations, key = { "invitation:${it.inviteId}" }) { invitation ->
                InvitationCard(
                    invitation = invitation,
                    onAcceptClicked = { invitationActions.onAcceptClicked(invitation.inviteId) },
                    onDeclineClicked = { invitationActions.onDeclineClicked(invitation.inviteId) },
                )
            }
        }
        if (content.groups.isNotEmpty()) {
            item(key = "groupsHeading") { SectionHeading(stringResource(Res.string.home_groups_heading)) }
            items(content.groups, key = { it.groupId }) { group ->
                GroupCard(group = group, onClick = { onGroupClicked(group.groupId) })
            }
        }
        content.archivedGroupsRow?.let { row ->
            item(key = "archivedGroups") { ArchivedGroupsRow(text = row, onClick = onArchivedGroupsClicked) }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun DeclineInviteDialog(
    dialog: DeclineInviteUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed) {
                Text(text = stringResource(Res.string.home_invitation_decline), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.common_cancel)) } },
        title = { Text(dialog.title.asString()) },
        text = { Text(dialog.message.asString()) },
    )
}

@Composable
private fun Loading() {
    val description = stringResource(Res.string.home_loading)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun Message(
    title: String,
    body: String?,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
        body?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
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
            onLateRentAlertsAnswered = {},
        )
    }
}
