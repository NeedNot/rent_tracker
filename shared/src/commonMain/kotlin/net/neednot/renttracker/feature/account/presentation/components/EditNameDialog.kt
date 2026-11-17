package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.runtime.Composable
import net.neednot.renttracker.core.designsystem.components.DialogButton
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.RtDialog
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
    RtDialog(
        title = stringResource(Res.string.account_edit_name_title),
        onDismissed = onDismissed,
        confirmButton = {
            DialogButton(
                text = stringResource(Res.string.common_save),
                onClick = onSaved,
                isWorking = dialog.isSaving,
            )
        },
        dismissButton = {
            DialogButton(
                text = stringResource(Res.string.common_cancel),
                onClick = onDismissed,
                enabled = !dialog.isSaving,
            )
        },
    ) {
        FormTextField(
            label = stringResource(Res.string.account_edit_name_label),
            text = dialog.name,
            onTextChanged = onNameChanged,
            error = dialog.error,
            enabled = !dialog.isSaving,
        )
    }
}
