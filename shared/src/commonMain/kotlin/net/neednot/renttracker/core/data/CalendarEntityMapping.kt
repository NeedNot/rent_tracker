package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.tenants.Allocation
import net.neednot.renttracker.model.tenants.CalendarMonth
import net.neednot.renttracker.model.tenants.AllocationChargeKind as ConvexAllocationChargeKind
import net.neednot.renttracker.model.tenants.CalendarMonthState as ConvexCalendarMonthState

fun CalendarMonth.toEntity(): CalendarMonthEntity =
    CalendarMonthEntity(
        tenantId = tenantId,
        month = month,
        state = state.toEntity(),
        rentCents = rentCents,
        coveredCents = coveredCents,
        owedCents = owedCents,
        hasUnpaidLateFee = hasUnpaidLateFee,
        dueDate = dueDate,
        graceEnd = graceEnd,
        wasLate = wasLate,
        paidByGraceEndCents = paidByGraceEndCents,
        lateFeeCents = lateFeeCents,
        lateFeeUnpaidCents = lateFeeUnpaidCents,
        suggestedLateFeeCents = suggestedLateFeeCents,
    )

fun Allocation.toEntity(): AllocationEntity =
    AllocationEntity(
        tenantId = tenantId,
        key = key,
        paymentId = paymentId,
        chargeKind = chargeKind.toEntity(),
        month = month,
        debtId = debtId,
        amountCents = amountCents,
    )

private fun ConvexCalendarMonthState.toEntity(): CalendarMonthState =
    when (this) {
        ConvexCalendarMonthState.Paid -> CalendarMonthState.Paid
        ConvexCalendarMonthState.Partial -> CalendarMonthState.Partial
        ConvexCalendarMonthState.Owed -> CalendarMonthState.Owed
        ConvexCalendarMonthState.DueNotLate -> CalendarMonthState.DueNotLate
        ConvexCalendarMonthState.Upcoming -> CalendarMonthState.Upcoming
        ConvexCalendarMonthState.Prepaid -> CalendarMonthState.Prepaid
        ConvexCalendarMonthState.NotATenant -> CalendarMonthState.NotATenant
    }

private fun ConvexAllocationChargeKind.toEntity(): AllocationChargeKind =
    when (this) {
        ConvexAllocationChargeKind.Rent -> AllocationChargeKind.Rent
        ConvexAllocationChargeKind.Debt -> AllocationChargeKind.Debt
    }
