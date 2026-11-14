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

internal fun PaymentDetails.statusDetail(timeZone: TimeZone): UiText =
    when (val status = status) {
        is EntryStatus.Cancelled -> {
            status.rowText(timeZone)
        }

        is EntryStatus.Bounced -> {
            status.by.text(
                Res.string.ledger_date_by_you,
                Res.string.ledger_date_by_named,
                formatMonthDay(status.at.localDate(timeZone)),
            )
        }

        EntryStatus.Active -> {
            loggedText(timeZone)
        }
    }

internal fun ChargeDetails.statusDetail(timeZone: TimeZone): UiText =
    when (val status = status) {
        is EntryStatus.Cancelled -> status.rowText(timeZone)
        else -> addedText()
    }

internal fun PaymentDetails.loggedText(timeZone: TimeZone): UiText {
    val loggedDate = formatMonthDay(loggedAt.localDate(timeZone))
    val receivedDate = formatMonthDay(dateReceived)
    return if (receivedDate == loggedDate) {
        loggedBy.text(Res.string.ledger_payment_logged_you, Res.string.ledger_payment_logged_named, loggedDate)
    } else {
        loggedBy.text(
            Res.string.ledger_payment_received_you,
            Res.string.ledger_payment_received_named,
            receivedDate,
            loggedDate,
        )
    }
}

internal fun ChargeDetails.addedText(): UiText =
    when (kind) {
        is ChargeKind.LateFee -> {
            addedBy.text(Res.string.ledger_late_fee_added_you, Res.string.ledger_late_fee_added_named)
        }

        is ChargeKind.Custom -> {
            addedBy.text(Res.string.ledger_custom_added_you, Res.string.ledger_custom_added_named)
        }
    }

internal fun EntryStatus.Cancelled.rowText(timeZone: TimeZone): UiText =
    if (reason.isBlank()) {
        by.text(Res.string.ledger_date_by_you, Res.string.ledger_date_by_named, formatMonthDay(at.localDate(timeZone)))
    } else {
        by.text(Res.string.ledger_cancelled_you, Res.string.ledger_cancelled_named, reason)
    }

internal fun EntryEdit.rowText(timeZone: TimeZone): UiText =
    by.text(Res.string.ledger_edited_you, Res.string.ledger_edited_named, formatMonthDay(at.localDate(timeZone)))

internal fun ChargeDetails.title(): UiText =
    when (val kind = kind) {
        is ChargeKind.LateFee -> UiText.Resource(Res.string.ledger_late_fee_title, listOf(fullMonthName(kind.forMonth)))
        is ChargeKind.Custom -> UiText.Plain(kind.note)
    }
