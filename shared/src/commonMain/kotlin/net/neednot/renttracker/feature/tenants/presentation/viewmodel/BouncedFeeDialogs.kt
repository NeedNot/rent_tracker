package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.designsystem.format.positiveAmountFormatError
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.LedgerEntryRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_message_fee_added

internal class BouncedFeeDialogs(
    private val scope: CoroutineScope,
    private val tenantId: String,
    private val ledgerEntries: LedgerEntryRepository,
    private val messageDispatcher: MessageDispatcher,
    private val dialog: MutableStateFlow<LedgerDialogUiState?>,
) {
    fun open(payment: LedgerEntry.Payment) {
        dialog.value = LedgerDialogUiState.ConfirmBounced(payment.bouncedConfirmation())
    }

    fun confirm(payment: LedgerEntry.Payment) {
        val confirmation = dialog.value as? LedgerDialogUiState.ConfirmBounced
        if (confirmation == null || confirmation.isWorking) return
        dialog.updateAs<LedgerDialogUiState.ConfirmBounced> { it.copy(isWorking = true, error = null) }
        scope.launch {
            ledgerEntries
                .markPaymentBounced(payment.payment.paymentId, payment.payment.version)
                .onOk { dialog.value = LedgerDialogUiState.BouncedFee() }
                .onErr { error ->
                    dialog.updateAs<LedgerDialogUiState.ConfirmBounced> {
                        it.copy(
                            isWorking = false,
                            error = error.text(),
                        )
                    }
                }
        }
    }

    fun onAmountChanged(text: String) {
        val error = if (text.isBlank()) null else positiveAmountFormatError(text)
        dialog.updateAs<LedgerDialogUiState.BouncedFee> { it.copy(amount = text, error = error) }
    }

    fun confirmFee(today: LocalDate) {
        val fee = dialog.value as? LedgerDialogUiState.BouncedFee
        if (fee == null || fee.isWorking) return
        val cents = parseDollarsToCents(fee.amount)
        val formatError = positiveAmountFormatError(fee.amount)
        if (cents == null || formatError != null) {
            dialog.updateAs<LedgerDialogUiState.BouncedFee> { it.copy(error = formatError) }
        } else {
            addFee(cents, today)
        }
    }

    private fun addFee(
        cents: Long,
        today: LocalDate,
    ) {
        dialog.updateAs<LedgerDialogUiState.BouncedFee> { it.copy(isWorking = true, error = null) }
        scope.launch {
            ledgerEntries
                .addBouncedCheckFee(tenantId, cents, today)
                .onOk {
                    dialog.value = null
                    messageDispatcher.show(UiMessage(UiText.Resource(Res.string.ledger_message_fee_added)))
                }.onErr { error ->
                    dialog.updateAs<LedgerDialogUiState.BouncedFee> { it.copy(isWorking = false, error = error.text()) }
                }
        }
    }
}
