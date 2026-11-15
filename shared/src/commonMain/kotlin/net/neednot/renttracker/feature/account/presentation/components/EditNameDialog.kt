package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.feature.account.presentation.uistate.EditNameDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_edit_name_label
import net.neednot.renttracker.resources.account_edit_name_title
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.common_save
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditNameDialog(
    dialog: EditNameDialogUiState,
    onNameChanged: (String) -> Unit,
    onSaved: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        title = { Text(stringResource(Res.string.account_edit_name_title)) },
        text = {
            FormTextField(
                label = stringResource(Res.string.account_edit_name_label),
                text = dialog.name,
                onTextChanged = onNameChanged,
                error = dialog.error,
                enabled = !dialog.isSaving,
            )
        },
        confirmButton = {
            TextButton(onClick = onSaved, enabled = !dialog.isSaving) { Text(stringResource(Res.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissed, enabled = !dialog.isSaving) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
