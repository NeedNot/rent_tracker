package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.LedgerEntryRepository
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_bounced_message
import net.neednot.renttracker.resources.ledger_bounced_message_check
import net.neednot.renttracker.resources.ledger_message_custom_cancelled
import net.neednot.renttracker.resources.ledger_message_custom_restored
import net.neednot.renttracker.resources.ledger_message_late_fee_cancelled
import net.neednot.renttracker.resources.ledger_message_late_fee_restored
import net.neednot.renttracker.resources.ledger_message_payment_cancelled
import net.neednot.renttracker.resources.ledger_message_payment_restored
import org.jetbrains.compose.resources.StringResource

internal suspend fun LedgerEntryRepository.cancelEntry(
    entry: LedgerEntry,
    reason: String,
): Result<Unit, TenantError> =
    when (entry) {
        is LedgerEntry.Payment -> cancelPayment(entry.payment.paymentId, entry.payment.version, reason)
        is LedgerEntry.Charge -> cancelDebt(entry.charge.debtId, entry.charge.version, reason)
        is LedgerEntry.Rent -> Err(TenantError.NotFound)
    }

internal suspend fun LedgerEntryRepository.restoreEntry(entry: LedgerEntry): Result<Unit, TenantError> =
    when (entry) {
        is LedgerEntry.Payment -> restorePayment(entry.payment.paymentId, entry.payment.version)
        is LedgerEntry.Charge -> restoreDebt(entry.charge.debtId, entry.charge.version)
        is LedgerEntry.Rent -> Err(TenantError.NotFound)
    }

internal fun LedgerEntry.cancelledMessage(): UiText =
    when (this) {
        is LedgerEntry.Payment -> {
            UiText.Resource(Res.string.ledger_message_payment_cancelled)
        }

        is LedgerEntry.Charge -> {
            charge.kind.message(
                Res.string.ledger_message_late_fee_cancelled,
                Res.string.ledger_message_custom_cancelled,
            )
        }

        is LedgerEntry.Rent -> {
            UiText.Resource(Res.string.ledger_message_payment_cancelled)
        }
    }

internal fun LedgerEntry.restoredMessage(): UiText =
    when (this) {
        is LedgerEntry.Payment -> {
            UiText.Resource(Res.string.ledger_message_payment_restored)
        }

        is LedgerEntry.Charge -> {
            charge.kind.message(
                Res.string.ledger_message_late_fee_restored,
                Res.string.ledger_message_custom_restored,
            )
        }

        is LedgerEntry.Rent -> {
            UiText.Resource(Res.string.ledger_message_payment_restored)
        }
    }

internal fun LedgerEntry.Payment.bouncedConfirmation(): UiText {
    val date = formatMonthDay(payment.dateReceived)
    val amount = formatMoney(amountCents)
    val resource =
        if (payment.method ==
            PaymentMethod.Check
        ) {
            Res.string.ledger_bounced_message_check
        } else {
            Res.string.ledger_bounced_message
        }
    return UiText.Resource(resource, listOf(date, amount))
}

private fun ChargeKind.message(
    lateFee: StringResource,
    custom: StringResource,
): UiText =
    when (this) {
        is ChargeKind.LateFee -> UiText.Resource(lateFee, listOf(fullMonthName(forMonth)))
        is ChargeKind.Custom -> UiText.Resource(custom, listOf(note))
    }
