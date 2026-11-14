package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.LedgerEntryRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.CANCEL_REASON_MAX_LENGTH
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerDialogUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_cancel_reason_required

internal class CancelEntryDialog(
    private val scope: CoroutineScope,
    private val ledgerEntries: LedgerEntryRepository,
    private val messageDispatcher: MessageDispatcher,
    private val dialog: MutableStateFlow<LedgerDialogUiState?>,
    private val onCancelled: () -> Unit,
) {
    fun open(action: LedgerSheetAction) {
        dialog.value = LedgerDialogUiState.Cancel(action)
    }

    fun onReasonChanged(text: String) {
        val reason = text.take(CANCEL_REASON_MAX_LENGTH)
        dialog.updateAs<LedgerDialogUiState.Cancel> { it.copy(reason = reason, error = reasonError(reason)) }
    }

    fun confirm(entry: LedgerEntry) {
        val cancel = dialog.value as? LedgerDialogUiState.Cancel
        val reason = cancel?.reason?.trim().orEmpty()
        if (cancel == null || cancel.isWorking) return
        if (reason.isEmpty()) {
            dialog.updateAs<LedgerDialogUiState.Cancel> { it.copy(error = reasonError(reason)) }
        } else {
            send(entry, reason)
        }
    }

    private fun send(
        entry: LedgerEntry,
        reason: String,
    ) {
        dialog.updateAs<LedgerDialogUiState.Cancel> { it.copy(isWorking = true, error = null) }
        scope.launch {
            ledgerEntries
                .cancelEntry(entry, reason)
                .onOk {
                    messageDispatcher.show(UiMessage(entry.cancelledMessage()))
                    onCancelled()
                }.onErr { error ->
                    dialog.updateAs<LedgerDialogUiState.Cancel> { it.copy(isWorking = false, error = error.text()) }
                }
        }
    }

    private fun reasonError(reason: String): UiText? =
        if (reason.isBlank()) UiText.Resource(Res.string.ledger_cancel_reason_required) else null
}
