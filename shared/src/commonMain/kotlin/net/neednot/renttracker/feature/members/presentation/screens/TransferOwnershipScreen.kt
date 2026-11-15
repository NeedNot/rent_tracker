package net.neednot.renttracker.feature.members.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.components.MembersEditorTopBar
import net.neednot.renttracker.feature.members.presentation.components.MembersListCard
import net.neednot.renttracker.feature.members.presentation.components.TransferCandidateRow
import net.neednot.renttracker.feature.members.presentation.components.TransferConfirmDialog
import net.neednot.renttracker.feature.members.presentation.uistate.TransferCandidateUiState
import net.neednot.renttracker.feature.members.presentation.uistate.TransferOwnershipUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.TransferOwnershipEvent
import net.neednot.renttracker.feature.members.presentation.viewmodel.TransferOwnershipViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_loading
import net.neednot.renttracker.resources.members_transfer_intro
import net.neednot.renttracker.resources.members_transfer_next
import net.neednot.renttracker.resources.members_transfer_ownership
import net.neednot.renttracker.resources.members_transfer_working
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TransferOwnershipScreen(
    groupId: String,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransferOwnershipViewModel = koinViewModel { parametersOf(groupId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TransferOwnershipEvent.OwnershipTransferred -> onClosed()
            }
        }
    }
    TransferOwnershipContent(
        uiState = uiState,
        onCandidateSelected = viewModel::onCandidateSelected,
        onNextClicked = viewModel::onNextClicked,
        onTransferConfirmed = viewModel::onTransferConfirmed,
        onConfirmDismissed = viewModel::onConfirmDismissed,
        onCloseClicked = onClosed,
        modifier = modifier,
    )
}

@Composable
private fun TransferOwnershipContent(
    uiState: TransferOwnershipUiState,
    onCandidateSelected: (membershipId: String) -> Unit,
    onNextClicked: () -> Unit,
    onTransferConfirmed: () -> Unit,
    onConfirmDismissed: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWorking = uiState.confirm?.isWorking == true
    Scaffold(
        modifier = modifier,
        topBar = {
            MembersEditorTopBar(
                title = stringResource(Res.string.members_transfer_ownership),
                actionLabel = stringResource(Res.string.members_transfer_next),
                workingLabel = stringResource(Res.string.members_transfer_working),
                isWorking = isWorking,
                isActionEnabled = uiState.selectedMembershipId != null && !isWorking,
                onActionClicked = onNextClicked,
                onCloseClicked = onCloseClicked,
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            val description = stringResource(Res.string.members_loading)
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.error?.let { error ->
                    Text(
                        text = error.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(Res.string.members_transfer_intro, uiState.groupName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                MembersListCard(items = uiState.candidates, modifier = Modifier.selectableGroup()) { candidate ->
                    TransferCandidateRow(
                        candidate = candidate,
                        isSelected = candidate.membershipId == uiState.selectedMembershipId,
                        onSelected = { onCandidateSelected(candidate.membershipId) },
                        enabled = !isWorking,
                    )
                }
            }
        }
    }
    uiState.confirm?.let { confirm ->
        TransferConfirmDialog(confirm = confirm, onConfirmed = onTransferConfirmed, onDismissed = onConfirmDismissed)
    }
}

@Preview
@Composable
private fun TransferOwnershipContentPreview() {
    RentTrackerTheme {
        TransferOwnershipContent(
            uiState =
                TransferOwnershipUiState(
                    isLoading = false,
                    groupName = "1st Street",
                    candidates =
                        listOf(
                            TransferCandidateUiState("m2", "SR", "Sam Rivera", MemberPermissionLabel.Manager),
                            TransferCandidateUiState("m3", "KC", "Kevin Cho", MemberPermissionLabel.Custom),
                        ),
                    selectedMembershipId = "m2",
                ),
            onCandidateSelected = {},
            onNextClicked = {},
            onTransferConfirmed = {},
            onConfirmDismissed = {},
            onCloseClicked = {},
        )
    }
}
