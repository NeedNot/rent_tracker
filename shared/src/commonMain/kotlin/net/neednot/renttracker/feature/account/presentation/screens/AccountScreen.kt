package net.neednot.renttracker.feature.account.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.account.presentation.components.AccountActionsCard
import net.neednot.renttracker.feature.account.presentation.components.AccountHeaderCard
import net.neednot.renttracker.feature.account.presentation.components.AccountNotificationsCard
import net.neednot.renttracker.feature.account.presentation.components.EditNameDialog
import net.neednot.renttracker.feature.account.presentation.components.RequestDeletionDialog
import net.neednot.renttracker.feature.account.presentation.components.SignOutPendingDialog
import net.neednot.renttracker.feature.account.presentation.uistate.AccountUiState
import net.neednot.renttracker.feature.account.presentation.uistate.DeletionRowUiState
import net.neednot.renttracker.feature.account.presentation.uistate.GroupNotificationsRowUiState
import net.neednot.renttracker.feature.account.presentation.uistate.NotificationsSectionUiState
import net.neednot.renttracker.feature.account.presentation.viewmodel.AccountNotificationsViewModel
import net.neednot.renttracker.feature.account.presentation.viewmodel.AccountViewModel
import net.neednot.renttracker.feature.account.presentation.viewmodel.SignOutWarningAnswer
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_back
import net.neednot.renttracker.resources.account_loading
import net.neednot.renttracker.resources.account_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinViewModel(),
    notificationsViewModel: AccountNotificationsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifications by notificationsViewModel.uiState.collectAsState()
    AccountContent(
        uiState = uiState,
        notifications = notifications,
        actions =
            AccountActions(
                onBackClicked = onBackClicked,
                onSignOutClicked = viewModel::onSignOutClicked,
                onGroupNotificationsToggled = notificationsViewModel::onGroupToggled,
                signOutWarning =
                    SignOutWarningActions(
                        onConfirmed = { viewModel.onSignOutWarningAnswered(SignOutWarningAnswer.SignOutAnyway) },
                        onDismissed = { viewModel.onSignOutWarningAnswered(SignOutWarningAnswer.StaySignedIn) },
                    ),
                editName =
                    EditNameActions(
                        onClicked = viewModel::onEditNameClicked,
                        onChanged = viewModel::onNameChanged,
                        onSaved = viewModel::onEditNameSaved,
                        onDismissed = viewModel::onEditNameDismissed,
                    ),
                deletion =
                    DeletionActions(
                        onClicked = viewModel::onRequestDeletionClicked,
                        onConfirmed = viewModel::onRequestDeletionConfirmed,
                        onDismissed = viewModel::onRequestDeletionDismissed,
                    ),
            ),
        modifier = modifier,
    )
}

private class AccountActions(
    val onBackClicked: () -> Unit,
    val onSignOutClicked: () -> Unit,
    val onGroupNotificationsToggled: (groupId: String) -> Unit,
    val signOutWarning: SignOutWarningActions,
    val editName: EditNameActions,
    val deletion: DeletionActions,
)

private class SignOutWarningActions(
    val onConfirmed: () -> Unit,
    val onDismissed: () -> Unit,
)

private class EditNameActions(
    val onClicked: () -> Unit,
    val onChanged: (String) -> Unit,
    val onSaved: () -> Unit,
    val onDismissed: () -> Unit,
)

private class DeletionActions(
    val onClicked: () -> Unit,
    val onConfirmed: () -> Unit,
    val onDismissed: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountContent(
    uiState: AccountUiState,
    notifications: NotificationsSectionUiState,
    actions: AccountActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.account_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        when (uiState) {
            AccountUiState.Loading -> {
                val description = stringResource(Res.string.account_loading)
                Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
                }
            }

            is AccountUiState.Error -> {
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

            is AccountUiState.Content -> {
                AccountBody(
                    content = uiState,
                    notifications = notifications,
                    actions = actions,
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }
}

@Composable
private fun AccountBody(
    content: AccountUiState.Content,
    notifications: NotificationsSectionUiState,
    actions: AccountActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content.syncError?.let { error ->
            Text(
                text = error.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        AccountHeaderCard(
            initials = content.initials,
            displayName = content.displayName,
            email = content.email,
            onEditNameClicked = actions.editName.onClicked,
        )
        (notifications as? NotificationsSectionUiState.Shown)?.let { section ->
            AccountNotificationsCard(section = section, onGroupToggled = actions.onGroupNotificationsToggled)
        }
        AccountActionsCard(
            deletion = content.deletion,
            onSignOutClicked = actions.onSignOutClicked,
            onRequestDeletionClicked = actions.deletion.onClicked,
        )
    }
    content.editNameDialog?.let { dialog ->
        EditNameDialog(
            dialog = dialog,
            onNameChanged = actions.editName.onChanged,
            onSaved = actions.editName.onSaved,
            onDismissed = actions.editName.onDismissed,
        )
    }
    content.unsyncedPaymentsOnSignOut?.let { count ->
        SignOutPendingDialog(
            unsyncedCount = count,
            onConfirmed = actions.signOutWarning.onConfirmed,
            onDismissed = actions.signOutWarning.onDismissed,
        )
    }
    if (content.isDeleteDialogShown) {
        RequestDeletionDialog(
            onConfirmed = actions.deletion.onConfirmed,
            onDismissed = actions.deletion.onDismissed,
        )
    }
}

@Preview
@Composable
private fun AccountContentPreview() {
    RentTrackerTheme {
        AccountContent(
            uiState =
                AccountUiState.Content(
                    initials = "SR",
                    displayName = "Sam Rivera",
                    email = "sam@example.com",
                    deletion = DeletionRowUiState.Requested(UiText.Plain("Requested Nov 14. Our team will handle it.")),
                ),
            notifications =
                NotificationsSectionUiState.Shown(
                    groups = listOf(GroupNotificationsRowUiState("g1", "1st Street", isOn = true)),
                    isBlockedOnPhone = false,
                ),
            actions =
                AccountActions(
                    onBackClicked = {},
                    onSignOutClicked = {},
                    onGroupNotificationsToggled = {},
                    signOutWarning = SignOutWarningActions({}, {}),
                    editName = EditNameActions({}, {}, {}, {}),
                    deletion = DeletionActions({}, {}, {}),
                ),
        )
    }
}
