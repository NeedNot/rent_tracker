package net.neednot.renttracker.feature.account.presentation.viewmodel

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.initialsOf
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.AccountProfile
import net.neednot.renttracker.feature.account.presentation.uistate.AccountUiState
import net.neednot.renttracker.feature.account.presentation.uistate.DeletionRowUiState
import net.neednot.renttracker.feature.account.presentation.uistate.EditNameDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_deletion_requested_detail

internal fun Synced<AccountProfile?, AccountError>.toAccountUiState(
    timeZone: TimeZone,
    editNameDialog: EditNameDialogUiState?,
    isDeleteDialogShown: Boolean,
    unsyncedPaymentsOnSignOut: Int?,
): AccountUiState {
    val profile = value
    val failure = (status as? SyncStatus.Failed)?.error
    return when {
        profile != null -> {
            profile
                .toContent(timeZone, editNameDialog, isDeleteDialogShown, failure?.text())
                .copy(unsyncedPaymentsOnSignOut = unsyncedPaymentsOnSignOut)
        }

        failure != null -> {
            AccountUiState.Error(failure.loadText())
        }

        else -> {
            AccountUiState.Loading
        }
    }
}

private fun AccountProfile.toContent(
    timeZone: TimeZone,
    editNameDialog: EditNameDialogUiState?,
    isDeleteDialogShown: Boolean,
    syncError: UiText?,
): AccountUiState.Content =
    AccountUiState.Content(
        initials = initialsOf(displayName),
        displayName = displayName,
        email = email,
        deletion = deletionRow(timeZone),
        editNameDialog = editNameDialog,
        isDeleteDialogShown = isDeleteDialogShown,
        syncError = syncError,
    )

private fun AccountProfile.deletionRow(timeZone: TimeZone): DeletionRowUiState {
    val requestedAt = deletionRequestedAt ?: return DeletionRowUiState.Available
    val requestedOn = formatMonthDay(requestedAt.toLocalDateTime(timeZone).date)
    return DeletionRowUiState.Requested(
        UiText.Resource(Res.string.account_deletion_requested_detail, listOf(requestedOn)),
    )
}
