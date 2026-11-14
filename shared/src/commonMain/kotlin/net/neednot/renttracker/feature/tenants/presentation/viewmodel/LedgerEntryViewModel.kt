package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.LedgerEntryRepository
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerDialogUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerEntryUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction

class LedgerEntryViewModel(
    tenantId: String,
    tenantRepository: TenantRepository,
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val messageDispatcher: MessageDispatcher,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val selectedKey = MutableStateFlow<String?>(null)
    private val dialog = MutableStateFlow<LedgerDialogUiState?>(null)
    private val isWorking = MutableStateFlow(false)
    private var shownOverview: TenantOverview? = null

    private val cancelDialog =
        CancelEntryDialog(viewModelScope, ledgerEntryRepository, messageDispatcher, dialog, ::onSheetDismissed)
    private val bouncedFeeDialogs =
        BouncedFeeDialogs(viewModelScope, tenantId, ledgerEntryRepository, messageDispatcher, dialog)

    val uiState: StateFlow<LedgerEntryUiState> =
        combine(
            tenantRepository.observeTenant(tenantId).onEach { shownOverview = it.value },
            selectedKey,
            dialog,
            isWorking,
        ) { synced, key, currentDialog, working ->
            val sheet =
                synced.value
                    ?.ledger
                    ?.firstOrNull { it.key == key }
                    ?.toSheetUiState(timeZone)
            LedgerEntryUiState(sheet = sheet, dialog = currentDialog.takeIf { sheet != null }, isWorking = working)
        }.stateIn(viewModelScope, WhileScreenIsVisible, LedgerEntryUiState(null, null, false))

    fun onEntryClicked(key: String) {
        dialog.value = null
        selectedKey.value = key
    }

    fun onSheetDismissed() {
        dialog.value = null
        selectedKey.value = null
    }

    fun onCancelClicked(action: LedgerSheetAction) {
        cancelDialog.open(action)
    }

    fun onMarkBouncedClicked() {
        (selectedEntry() as? LedgerEntry.Payment)?.let(bouncedFeeDialogs::open)
    }

    fun onDialogTextChanged(text: String) {
        when (dialog.value) {
            is LedgerDialogUiState.Cancel -> cancelDialog.onReasonChanged(text)
            is LedgerDialogUiState.BouncedFee -> bouncedFeeDialogs.onAmountChanged(text)
            is LedgerDialogUiState.ConfirmBounced, null -> Unit
        }
    }

    fun onDialogConfirmed() {
        val entry = selectedEntry()
        when (dialog.value) {
            is LedgerDialogUiState.Cancel -> entry?.let(cancelDialog::confirm)
            is LedgerDialogUiState.ConfirmBounced -> (entry as? LedgerEntry.Payment)?.let(bouncedFeeDialogs::confirm)
            is LedgerDialogUiState.BouncedFee -> shownOverview?.let { bouncedFeeDialogs.confirmFee(it.today) }
            null -> Unit
        }
    }

    fun onDialogDismissed() {
        dialog.value = null
    }

    fun onRestoreEntryClicked(key: String) {
        val entry = shownOverview?.ledger?.firstOrNull { it.key == key }
        if (entry == null || isWorking.value) return
        isWorking.value = true
        viewModelScope.launch {
            val result = ledgerEntryRepository.restoreEntry(entry)
            isWorking.value = false
            result
                .onOk {
                    if (selectedKey.value == key) onSheetDismissed()
                    messageDispatcher.show(UiMessage(entry.restoredMessage()))
                }.onErr { error -> messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error)) }
        }
    }

    private fun selectedEntry(): LedgerEntry? = shownOverview?.ledger?.firstOrNull { it.key == selectedKey.value }
}
