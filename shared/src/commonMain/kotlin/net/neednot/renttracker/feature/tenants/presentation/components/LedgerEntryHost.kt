package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerDialogUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.LedgerEntryViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun LedgerEntryHost(
    tenantId: String,
    onEditClicked: (EntryEditTarget) -> Unit,
    content: @Composable (onRowClicked: (key: String) -> Unit, onRestoreClicked: (key: String) -> Unit) -> Unit,
    viewModel: LedgerEntryViewModel = koinViewModel { parametersOf(tenantId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    content(viewModel::onEntryClicked, viewModel::onRestoreEntryClicked)
    uiState.sheet?.let { sheet ->
        EntrySheet(
            sheet = sheet,
            isWorking = uiState.isWorking,
            onActionClicked = { action ->
                when (action) {
                    LedgerSheetAction.Edit -> {
                        viewModel.onSheetDismissed()
                        onEditClicked(sheet.editTarget)
                    }

                    LedgerSheetAction.MarkBounced -> {
                        viewModel.onMarkBouncedClicked()
                    }

                    LedgerSheetAction.CancelPayment, LedgerSheetAction.CancelCharge -> {
                        viewModel.onCancelClicked(action)
                    }

                    LedgerSheetAction.Restore, LedgerSheetAction.RestoreClearBounce -> {
                        viewModel.onRestoreEntryClicked(sheet.key)
                    }
                }
            },
            onDismissed = viewModel::onSheetDismissed,
        )
    }
    uiState.dialog?.let { dialog ->
        LedgerEntryDialog(
            dialog = dialog,
            onTextChanged = viewModel::onDialogTextChanged,
            onConfirmed = viewModel::onDialogConfirmed,
            onDismissed = viewModel::onDialogDismissed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntrySheet(
    sheet: LedgerSheetUiState,
    isWorking: Boolean,
    onActionClicked: (LedgerSheetAction) -> Unit,
    onDismissed: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissed) {
        LedgerEntrySheet(sheet = sheet, isWorking = isWorking, onActionClicked = onActionClicked)
    }
}
