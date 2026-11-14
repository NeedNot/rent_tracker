package net.neednot.renttracker.feature.payments.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.feature.payments.domain.MonthState
import net.neednot.renttracker.feature.payments.domain.MonthStateChange
import net.neednot.renttracker.model.payments.Change
import net.neednot.renttracker.model.payments.PaymentsPreviewProposed
import net.neednot.renttracker.model.payments.PaymentsPreviewResult

internal fun toProposed(
    paymentId: String?,
    amountCents: Long,
    dateReceived: LocalDate,
): PaymentsPreviewProposed =
    PaymentsPreviewProposed(amountCents = amountCents, dateReceived = dateReceived.toString(), paymentId = paymentId)

internal fun PaymentsPreviewResult.toMonthStateChanges(): List<MonthStateChange> =
    changes.map {
        it.toMonthStateChange()
    }

private fun Change.toMonthStateChange(): MonthStateChange =
    MonthStateChange(
        month = YearMonth.parse(month),
        before = MonthState.valueOf(before.name),
        after = MonthState.valueOf(after.name),
    )
