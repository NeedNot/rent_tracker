package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeDetails
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryEdit
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.PaymentDetails
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_amount_paid
import net.neednot.renttracker.resources.ledger_cancelled_named
import net.neednot.renttracker.resources.ledger_cancelled_you
import net.neednot.renttracker.resources.ledger_custom_added_named
import net.neednot.renttracker.resources.ledger_custom_added_you
import net.neednot.renttracker.resources.ledger_date_by_named
import net.neednot.renttracker.resources.ledger_date_by_you
import net.neednot.renttracker.resources.ledger_edited_named
import net.neednot.renttracker.resources.ledger_edited_you
import net.neednot.renttracker.resources.ledger_late_fee_added_named
import net.neednot.renttracker.resources.ledger_late_fee_added_you
import net.neednot.renttracker.resources.ledger_late_fee_title
import net.neednot.renttracker.resources.ledger_payment_logged_named
import net.neednot.renttracker.resources.ledger_payment_logged_you
import net.neednot.renttracker.resources.ledger_payment_received_named
import net.neednot.renttracker.resources.ledger_payment_received_you
import net.neednot.renttracker.resources.ledger_payment_title
import net.neednot.renttracker.resources.ledger_rent_title

internal fun LedgerEntry.toRowUiState(timeZone: TimeZone): LedgerRowUiState =
    when (this) {
        is LedgerEntry.Rent -> rentRow()
        is LedgerEntry.Payment -> paymentRow(timeZone)
        is LedgerEntry.Charge -> chargeRow(timeZone)
    }

private fun LedgerEntry.Rent.rentRow(): LedgerRowUiState =
    LedgerRowUiState(
        key = key,
        date = formatMonthDay(date),
        title = UiText.Resource(Res.string.ledger_rent_title, listOf(fullMonthName(month))),
        details = emptyList(),
        amount = UiText.Plain(formatMoney(amountCents)),
        balance = balance?.inWords(),
        chips = emptyList(),
        isStruckThrough = false,
        mayNotApply = false,
        canRestore = false,
        isTappable = false,
    )

private fun LedgerEntry.Payment.paymentRow(timeZone: TimeZone): LedgerRowUiState {
    val status = payment.status
    return LedgerRowUiState(
        key = key,
        date = formatMonthDay(date),
        title =
            payment.method?.let { UiText.Resource(it.paymentTitle()) }
                ?: UiText.Resource(Res.string.ledger_payment_title),
        details = listOfNotNull(payment.statusDetail(timeZone), payment.edit?.rowText(timeZone)),
        amount = if (status == EntryStatus.Active) paidText(amountCents) else UiText.Plain(formatMoney(amountCents)),
        balance = balance?.inWords(),
        chips = status.chips() + listOfNotNull(payment.edit?.let { EntryChip.Edited }),
        isStruckThrough = status is EntryStatus.Cancelled,
        mayNotApply = false,
        canRestore = payment.canChange && status != EntryStatus.Active,
        isTappable = true,
    )
}

private fun LedgerEntry.Charge.chargeRow(timeZone: TimeZone): LedgerRowUiState {
    val status = charge.status
    return LedgerRowUiState(
        key = key,
        date = formatMonthDay(date),
        title = charge.title(),
        details = listOfNotNull(charge.statusDetail(timeZone), charge.edit?.rowText(timeZone)),
        amount = UiText.Plain(formatMoney(amountCents)),
        balance = balance?.inWords(),
        chips = status.chips() + listOfNotNull(charge.edit?.let { EntryChip.Edited }),
        isStruckThrough = status is EntryStatus.Cancelled,
        mayNotApply = charge.mayNotApply && status == EntryStatus.Active,
        canRestore = charge.canChange && status != EntryStatus.Active,
        isTappable = true,
    )
}

private fun paidText(cents: Long): UiText = UiText.Resource(Res.string.ledger_amount_paid, listOf(formatMoney(cents)))
