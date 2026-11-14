package net.neednot.renttracker.feature.payments.presentation.viewmodel

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState

internal data class ProposedPaymentChange(
    val amountCents: Long,
    val dateReceived: LocalDate,
)

internal fun PaymentEditorUiState.proposedChange(saved: PaymentDraft?): ProposedPaymentChange? {
    val amountCents = parseDollarsToCents(amount)?.takeIf { it > 0 }
    val date = date?.takeIf { dateError() == null }
    if (saved == null || amountCents == null || date == null) return null
    val isUnchanged = amountCents == saved.amountCents && date == saved.dateReceived
    return if (isUnchanged) null else ProposedPaymentChange(amountCents, date)
}
