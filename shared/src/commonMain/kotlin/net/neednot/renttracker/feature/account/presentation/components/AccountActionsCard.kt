package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.account.presentation.uistate.DeletionRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_deletion_requested
import net.neednot.renttracker.resources.account_request_deletion
import net.neednot.renttracker.resources.account_request_deletion_hint
import net.neednot.renttracker.resources.account_sign_out
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AccountActionsCard(
    deletion: DeletionRowUiState,
    onSignOutClicked: () -> Unit,
    onRequestDeletionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountCard(modifier = modifier) {
        AccountActionRow(label = stringResource(Res.string.account_sign_out), onClick = onSignOutClicked)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when (deletion) {
            DeletionRowUiState.Available -> {
                AccountActionRow(
                    label = stringResource(Res.string.account_request_deletion),
                    supportingText = stringResource(Res.string.account_request_deletion_hint),
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = onRequestDeletionClicked,
                )
            }

            is DeletionRowUiState.Requested -> {
                AccountActionRow(
                    label = stringResource(Res.string.account_deletion_requested),
                    supportingText = deletion.supportingText.asString(),
                )
            }
        }
    }
}
