package net.neednot.renttracker.feature.members.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.EmptyState
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.InitialsAvatar
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListRow
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.TopBarNavigation
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.components.TransferConfirmDialog
import net.neednot.renttracker.feature.members.presentation.uistate.TransferCandidateUiState
import net.neednot.renttracker.feature.members.presentation.uistate.TransferOwnershipUiState
import net.neednot.renttracker.feature.members.presentation.viewmodel.TransferOwnershipEvent
import net.neednot.renttracker.feature.members.presentation.viewmodel.TransferOwnershipViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_chevron_right
import net.neednot.renttracker.resources.ic_group
import net.neednot.renttracker.resources.members_error_title
import net.neednot.renttracker.resources.members_invite_title
import net.neednot.renttracker.resources.members_transfer_empty_body
import net.neednot.renttracker.resources.members_transfer_empty_title
import net.neednot.renttracker.resources.members_transfer_intro
import net.neednot.renttracker.resources.members_transfer_ownership
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TransferOwnershipScreen(
    groupId: String,
    onInviteClicked: () -> Unit,
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
        actions =
            TransferOwnershipActions(
                onCandidateClicked = viewModel::onCandidateClicked,
                onTransferConfirmed = viewModel::onTransferConfirmed,
                onConfirmDismissed = viewModel::onConfirmDismissed,
                onInviteClicked = onInviteClicked,
                onCloseClicked = onClosed,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun TransferOwnershipContent(
    uiState: TransferOwnershipUiState,
    actions: TransferOwnershipActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(
                title = stringResource(Res.string.members_transfer_ownership),
                navigation = TopBarNavigation.Close,
                onNavigationClicked = actions.onCloseClicked,
            )
        },
    ) { contentPadding ->
        val bodyModifier = Modifier.padding(contentPadding)
        when {
            uiState.isLoading -> {
                LoadingIndicator(modifier = bodyModifier)
            }

            uiState.candidates.isEmpty() && uiState.error != null -> {
                ErrorState(
                    title = stringResource(Res.string.members_error_title),
                    text = uiState.error.asString(),
                    onRetryClicked = UnimplementedAction,
                    modifier = bodyModifier,
                )
            }

            uiState.candidates.isEmpty() -> {
                NoCandidates(groupName = uiState.groupName, actions = actions, modifier = bodyModifier)
            }

            else -> {
                Candidates(uiState = uiState, actions = actions, modifier = bodyModifier)
            }
        }
    }
    uiState.confirm?.let { confirm ->
        TransferConfirmDialog(
            confirm = confirm,
            onConfirmed = actions.onTransferConfirmed,
            onDismissed = actions.onConfirmDismissed,
        )
    }
}

@Composable
private fun Candidates(
    uiState: TransferOwnershipUiState,
    actions: TransferOwnershipActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ContentLayout.List.padding),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
    ) {
        uiState.error?.let { Banner(text = it.asString(), kind = BannerKind.Error) }
        Text(
            text = stringResource(Res.string.members_transfer_intro, uiState.groupName),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        RtListCard(items = uiState.candidates) { candidate ->
            CandidateRow(candidate = candidate, onClick = { actions.onCandidateClicked(candidate.membershipId) })
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: TransferCandidateUiState,
    onClick: () -> Unit,
) {
    RtListRow(
        title = candidate.name,
        subtitle = candidate.email,
        onClick = onClick,
        leading = { InitialsAvatar(initials = candidate.initials) },
        trailing = {
            AppIcon(
                icon = Res.drawable.ic_chevron_right,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun NoCandidates(
    groupName: String,
    actions: TransferOwnershipActions,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Res.drawable.ic_group,
        title = stringResource(Res.string.members_transfer_empty_title),
        modifier = modifier,
        text = stringResource(Res.string.members_transfer_empty_body, groupName),
        action = {
            RtButton(
                text = stringResource(Res.string.members_invite_title),
                onClick = actions.onInviteClicked,
                variant = ButtonVariant.Outlined,
            )
        },
    )
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
                            TransferCandidateUiState("m2", "SR", "Sam Rivera", "sam.rivera@example.com"),
                            TransferCandidateUiState("m4", "ML", "Maria Lopez", "maria.lopez@example.com"),
                        ),
                ),
            actions = TransferOwnershipActions(),
        )
    }
}
