package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeDetails
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryEdit
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.PaymentDetails
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerFieldUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_date_by_named
import net.neednot.renttracker.resources.ledger_date_by_you
import net.neednot.renttracker.resources.ledger_edited_named
import net.neednot.renttracker.resources.ledger_edited_you
import net.neednot.renttracker.resources.ledger_field_added
import net.neednot.renttracker.resources.ledger_field_cancelled
import net.neednot.renttracker.resources.ledger_field_date
import net.neednot.renttracker.resources.ledger_field_date_received
import net.neednot.renttracker.resources.ledger_field_edited
import net.neednot.renttracker.resources.ledger_field_for_month
import net.neednot.renttracker.resources.ledger_field_logged
import net.neednot.renttracker.resources.ledger_field_marked_bounced
import net.neednot.renttracker.resources.ledger_field_method
import net.neednot.renttracker.resources.ledger_field_note
import net.neednot.renttracker.resources.ledger_field_reason
import net.neednot.renttracker.resources.ledger_field_status
import net.neednot.renttracker.resources.ledger_sheet_late_fee
import net.neednot.renttracker.resources.ledger_sheet_not_counted
import net.neednot.renttracker.resources.ledger_sheet_payment
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Instant

internal fun LedgerEntry.toSheetUiState(timeZone: TimeZone): LedgerSheetUiState? =
    when (this) {
        is LedgerEntry.Rent -> null
        is LedgerEntry.Payment -> paymentSheet(timeZone)
        is LedgerEntry.Charge -> chargeSheet(timeZone)
    }

private fun LedgerEntry.Payment.paymentSheet(timeZone: TimeZone): LedgerSheetUiState =
    LedgerSheetUiState(
        key = key,
        title = UiText.Resource(Res.string.ledger_sheet_payment),
        amount = formatMoney(amountCents),
        chips = payment.status.chips() + listOfNotNull(payment.edit?.let { EntryChip.Edited }),
        fields = payment.fields(timeZone),
        actions = payment.actions(),
        editTarget = EntryEditTarget.Payment(payment.paymentId),
    )

private fun LedgerEntry.Charge.chargeSheet(timeZone: TimeZone): LedgerSheetUiState =
    LedgerSheetUiState(
        key = key,
        title = charge.sheetTitle(),
        amount = formatMoney(amountCents),
        chips = charge.status.chips() + listOfNotNull(charge.edit?.let { EntryChip.Edited }),
        fields = charge.fields(timeZone),
        actions = charge.actions(),
        editTarget = EntryEditTarget.Charge(charge.debtId),
    )

private fun ChargeDetails.sheetTitle(): UiText =
    when (val kind = kind) {
        is ChargeKind.LateFee -> UiText.Resource(Res.string.ledger_sheet_late_fee)
        is ChargeKind.Custom -> UiText.Plain(kind.note)
    }

private fun PaymentDetails.actions(): List<LedgerSheetAction> =
    when {
        !canChange -> emptyList()
        status is EntryStatus.Cancelled -> listOf(LedgerSheetAction.Restore)
        status is EntryStatus.Bounced -> listOf(LedgerSheetAction.RestoreClearBounce, LedgerSheetAction.CancelPayment)
        else -> listOf(LedgerSheetAction.Edit, LedgerSheetAction.MarkBounced, LedgerSheetAction.CancelPayment)
    }

private fun ChargeDetails.actions(): List<LedgerSheetAction> =
    when {
        !canChange -> emptyList()
        status is EntryStatus.Cancelled -> listOf(LedgerSheetAction.Restore)
        else -> listOf(LedgerSheetAction.Edit, LedgerSheetAction.CancelCharge)
    }
