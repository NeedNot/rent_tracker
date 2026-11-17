package net.neednot.renttracker.feature.account.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RtListCard
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.SectionHeader
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.account.presentation.components.AccountProfileCard
import net.neednot.renttracker.feature.account.presentation.components.EditNameDialog
import net.neednot.renttracker.feature.account.presentation.components.RequestDeletionDialog
import net.neednot.renttracker.feature.account.presentation.components.SignOutDialog
import net.neednot.renttracker.feature.account.presentation.components.UnavailableSettings
import net.neednot.renttracker.feature.account.presentation.uistate.AccountUiState
import net.neednot.renttracker.feature.account.presentation.uistate.DeletionRowUiState
import net.neednot.renttracker.feature.account.presentation.viewmodel.AccountViewModel
import net.neednot.renttracker.feature.account.presentation.viewmodel.SignOutWarningAnswer
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_deletion_requested
import net.neednot.renttracker.resources.account_error_title
import net.neednot.renttracker.resources.account_request_deletion
import net.neednot.renttracker.resources.account_request_deletion_hint
import net.neednot.renttracker.resources.account_sign_out
import net.neednot.renttracker.resources.account_title
import net.neednot.renttracker.resources.ic_chevron_right
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    AccountContent(
        uiState = uiState,
        actions =
            AccountActions(
                onBackClicked = onBackClicked,
                onSignOutClicked = viewModel::onSignOutClicked,
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

@Composable
internal fun AccountContent(
    uiState: AccountUiState,
    actions: AccountActions,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? AccountUiState.Content
    Scaffold(
        modifier = modifier,
        topBar = {
            RtTopAppBar(
                title = stringResource(Res.string.account_title),
                onNavigationClicked = actions.onBackClicked,
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            content?.offlineBanner?.let { OfflineBanner(text = it.asString()) }
            when (uiState) {
                AccountUiState.Loading -> {
                    LoadingIndicator()
                }

                is AccountUiState.Error -> {
                    ErrorState(
                        title = stringResource(Res.string.account_error_title),
                        text = uiState.message.asString(),
                        onRetryClicked = UnimplementedAction,
                    )
                }

                is AccountUiState.Content -> {
                    AccountBody(content = uiState, actions = actions)
                }
            }
        }
    }
    content?.let { AccountDialogs(content = it, actions = actions) }
}

@Composable
private fun AccountBody(
    content: AccountUiState.Content,
    actions: AccountActions,
) {
    val isOffline = content.offlineBanner != null
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ContentLayout.List.padding),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
    ) {
        content.syncError?.let { Banner(text = it.asString(), kind = BannerKind.Error) }
        AccountProfileCard(
            initials = content.initials,
            displayName = content.displayName,
            email = content.email,
            onEditNameClicked = actions.editName.onClicked.takeUnless { isOffline },
        )
        UnavailableSettings()
        SectionHeader(text = stringResource(Res.string.account_title))
        RtListCard(items = listOfNotNull(AccountRow.SignOut, AccountRow.Deletion.takeUnless { isOffline })) { row ->
            when (row) {
                AccountRow.SignOut -> {
                    ChevronItem(
                        text = stringResource(Res.string.account_sign_out),
                        onClick = actions.onSignOutClicked,
                    )
                }

                AccountRow.Deletion -> {
                    DeletionItem(deletion = content.deletion, onClick = actions.deletion.onClicked)
                }
            }
        }
    }
}

private enum class AccountRow { SignOut, Deletion }

@Composable
private fun DeletionItem(
    deletion: DeletionRowUiState,
    onClick: () -> Unit,
) {
    when (deletion) {
        DeletionRowUiState.Available -> {
            ChevronItem(
                text = stringResource(Res.string.account_request_deletion),
                supportingText = stringResource(Res.string.account_request_deletion_hint),
                tone = ActionTone.Destructive,
                onClick = onClick,
            )
        }

        is DeletionRowUiState.Requested -> {
            RtListItem(
                text = stringResource(Res.string.account_deletion_requested),
                supportingText = deletion.supportingText.asString(),
            )
        }
    }
}

@Composable
private fun ChevronItem(
    text: String,
    onClick: () -> Unit,
    supportingText: String? = null,
    tone: ActionTone = ActionTone.Default,
) {
    RtListItem(
        text = text,
        supportingText = supportingText,
        tone = tone,
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

@Composable
private fun AccountDialogs(
    content: AccountUiState.Content,
    actions: AccountActions,
) {
    content.editNameDialog?.let { dialog ->
        EditNameDialog(
            dialog = dialog,
            onNameChanged = actions.editName.onChanged,
            onSaved = actions.editName.onSaved,
            onDismissed = actions.editName.onDismissed,
        )
    }
    content.unsyncedPaymentsOnSignOut?.let { count ->
        SignOutDialog(
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
            actions = AccountActions(),
        )
    }
}
