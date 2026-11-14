package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.AllocationTarget
import net.neednot.renttracker.feature.tenants.domain.CalendarMonth
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.MonthPayment
import net.neednot.renttracker.feature.tenants.domain.OtherAllocation
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthPaymentUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_other_finished
import net.neednot.renttracker.resources.calendar_other_many
import net.neednot.renttracker.resources.calendar_other_many_late_fee
import net.neednot.renttracker.resources.calendar_other_single_late_fee
import net.neednot.renttracker.resources.calendar_other_single_to
import net.neednot.renttracker.resources.calendar_paid_after_grace
import net.neednot.renttracker.resources.calendar_payment_bank_transfer
import net.neednot.renttracker.resources.calendar_payment_cash
import net.neednot.renttracker.resources.calendar_payment_check
import net.neednot.renttracker.resources.calendar_payment_no_method
import net.neednot.renttracker.resources.calendar_payment_other
import net.neednot.renttracker.resources.calendar_payment_venmo
import net.neednot.renttracker.resources.calendar_payment_zelle
import org.jetbrains.compose.resources.StringResource

internal fun MonthPayment.toUiState(month: CalendarMonth): MonthPaymentUiState =
    MonthPaymentUiState(
        title = UiText.Resource(method.title(), listOf(formatMonthDay(dateReceived))),
        amount = formatMoney(allocatedToMonthCents),
        notes = otherAllocationNotes(month.month) + listOfNotNull(lateNote(month)),
    )

private fun PaymentMethod?.title(): StringResource =
    when (this) {
        PaymentMethod.Cash -> Res.string.calendar_payment_cash
        PaymentMethod.Check -> Res.string.calendar_payment_check
        PaymentMethod.Zelle -> Res.string.calendar_payment_zelle
        PaymentMethod.Venmo -> Res.string.calendar_payment_venmo
        PaymentMethod.BankTransfer -> Res.string.calendar_payment_bank_transfer
        PaymentMethod.Other -> Res.string.calendar_payment_other
        null -> Res.string.calendar_payment_no_method
    }

private fun MonthPayment.lateNote(month: CalendarMonth): UiText? {
    val graceEnd = month.graceEnd
    val wasPaidLate = graceEnd != null && month.wasLate && dateReceived > graceEnd
    return graceEnd.takeIf { wasPaidLate }?.let {
        UiText.Resource(Res.string.calendar_paid_after_grace, listOf(formatMonthDay(it), fullMonthName(month.month)))
    }
}

private fun MonthPayment.otherAllocationNotes(month: YearMonth): List<UiText> =
    when (otherAllocations.size) {
        0 -> emptyList()
        1 -> listOf(otherAllocations.single().singleSentence(formatMoney(paymentCents), month))
        else -> otherAllocations.map { it.listedSentence() }
    }

private fun OtherAllocation.singleSentence(
    paymentAmount: String,
    month: YearMonth,
): UiText {
    val amount = formatMoney(amountCents)
    return when (val target = target) {
        is AllocationTarget.Rent -> {
            val sentence =
                if (target.month < month) Res.string.calendar_other_finished else Res.string.calendar_other_single_to
            UiText.Resource(sentence, listOf(amount, paymentAmount, fullMonthName(target.month)))
        }

        is AllocationTarget.Charge -> {
            when (val kind = target.kind) {
                is ChargeKind.LateFee -> {
                    UiText.Resource(
                        Res.string.calendar_other_single_late_fee,
                        listOf(amount, paymentAmount, fullMonthName(kind.forMonth)),
                    )
                }

                is ChargeKind.Custom -> {
                    UiText.Resource(Res.string.calendar_other_single_to, listOf(amount, paymentAmount, kind.note))
                }
            }
        }
    }
}

private fun OtherAllocation.listedSentence(): UiText {
    val amount = formatMoney(amountCents)
    return when (val target = target) {
        is AllocationTarget.Rent -> {
            UiText.Resource(Res.string.calendar_other_many, listOf(amount, fullMonthName(target.month)))
        }

        is AllocationTarget.Charge -> {
            when (val kind = target.kind) {
                is ChargeKind.LateFee -> {
                    UiText.Resource(
                        Res.string.calendar_other_many_late_fee,
                        listOf(amount, fullMonthName(kind.forMonth)),
                    )
                }

                is ChargeKind.Custom -> {
                    UiText.Resource(Res.string.calendar_other_many, listOf(amount, kind.note))
                }
            }
        }
    }
}
