package net.neednot.renttracker.feature.members.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.components.InviteRow
import net.neednot.renttracker.feature.members.presentation.components.MemberRow
import net.neednot.renttracker.feature.members.presentation.components.MembersListCard
import net.neednot.renttracker.feature.members.presentation.components.RemoveMemberDialog
import net.neednot.renttracker.feature.members.presentation.uistate.InviteRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowAction
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MembersUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.MembersViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_back
import net.neednot.renttracker.resources.members_invite
import net.neednot.renttracker.resources.members_loading
import net.neednot.renttracker.resources.members_pending_invites
import net.neednot.renttracker.resources.members_title
import net.neednot.renttracker.resources.members_transfer_ownership
import net.neednot.renttracker.resources.members_view_only_info
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val FAB_CLEARANCE = 96

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
        navigation =
            MembersNavigation(
                onBackClicked = onBackClicked,
                onInviteClicked = onInviteClicked,
                onMemberClicked = onMemberClicked,
                onTransferOwnershipClicked = onTransferOwnershipClicked,
            ),
        actions =
            MembersActions(
                onRemoveClicked = viewModel::onRemoveClicked,
                onRemoveConfirmed = viewModel::onRemoveConfirmed,
                onRemoveDismissed = viewModel::onRemoveDismissed,
                onRevokeClicked = viewModel::onRevokeClicked,
            ),
        modifier = modifier,
    )
}

private class MembersNavigation(
    val onBackClicked: () -> Unit,
    val onInviteClicked: () -> Unit,
    val onMemberClicked: (membershipId: String) -> Unit,
    val onTransferOwnershipClicked: () -> Unit,
)

private class MembersActions(
    val onRemoveClicked: (membershipId: String) -> Unit,
    val onRemoveConfirmed: () -> Unit,
    val onRemoveDismissed: () -> Unit,
    val onRevokeClicked: (inviteId: String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersContent(
    uiState: MembersUiState,
    navigation: MembersNavigation,
    actions: MembersActions,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? MembersUiState.Content
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.members_title))
                        content?.let { Text(text = it.groupName, style = MaterialTheme.typography.bodySmall) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigation.onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.members_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (content?.canInvite == true) {
                ExtendedFloatingActionButton(
                    onClick = navigation.onInviteClicked,
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.members_invite)) },
                )
            }
        },
    ) { contentPadding ->
        when (uiState) {
            MembersUiState.Loading -> {
                val description = stringResource(Res.string.members_loading)
                Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
                }
            }

            is MembersUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding).padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.message.asString(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            is MembersUiState.Content -> {
                MembersList(
                    content = uiState,
                    navigation = navigation,
                    actions = actions,
                    contentPadding = contentPadding,
                )
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
    navigation: MembersNavigation,
    actions: MembersActions,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = FAB_CLEARANCE.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
        if (content.showsViewOnlyInfo) {
            item(key = "viewOnlyInfo") { ViewOnlyInfo() }
        }
        item(key = "members") {
            MembersListCard(items = content.members) { member ->
                MemberRow(
                    member = member,
                    onClick = { navigation.onMemberClicked(member.membershipId) },
                    onRemoveClicked = { actions.onRemoveClicked(member.membershipId) },
                )
            }
        }
        if (content.invites.isNotEmpty()) {
            item(key = "invitesHeading") {
                Text(
                    text = stringResource(Res.string.members_pending_invites),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item(key = "invites") {
                MembersListCard(items = content.invites) { invite ->
                    InviteRow(invite = invite, onRevokeClicked = { actions.onRevokeClicked(invite.inviteId) })
                }
            }
        }
        if (content.canTransferOwnership) {
            item(key = "transfer") {
                MembersListCard(items = listOf(Unit), modifier = Modifier.padding(top = 4.dp)) {
                    TransferOwnershipLink(onClick = navigation.onTransferOwnershipClicked)
                }
            }
        }
    }
}

@Composable
private fun ViewOnlyInfo() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.members_view_only_info),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TransferOwnershipLink(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                ).heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = Icons.Outlined.WorkspacePremium, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(Res.string.members_transfer_ownership),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                "m1",
                                "NN",
                                "Need_Not",
                                "you@example.com",
                                true,
                                MemberPermissionLabel.Owner,
                                MemberRowAction.None,
                            ),
                            MemberRowUiState(
                                "m2",
                                "SR",
                                "Sam Rivera",
                                "sam.rivera@example.com",
                                false,
                                MemberPermissionLabel.Manager,
                                MemberRowAction.OpenPermissions,
                            ),
                        ),
                    invites =
                        listOf(
                            InviteRowUiState(
                                "i1",
                                "alex.m@example.com",
                                MemberPermissionLabel.Bookkeeper,
                                "Mar 16",
                                true,
                            ),
                        ),
                    canInvite = true,
                    canTransferOwnership = true,
                ),
            navigation = MembersNavigation({}, {}, {}, {}),
            actions = MembersActions({}, {}, {}, {}),
        )
    }
}
