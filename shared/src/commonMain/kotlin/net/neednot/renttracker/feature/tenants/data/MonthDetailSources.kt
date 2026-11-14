package net.neednot.renttracker.feature.tenants.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.AllocationChargeKind
import net.neednot.renttracker.core.data.AllocationEntity
import net.neednot.renttracker.core.data.CalendarMonthEntity
import net.neednot.renttracker.core.data.DebtEntity
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.LedgerRowKind
import net.neednot.renttracker.core.data.PaymentEntity
import net.neednot.renttracker.feature.tenants.domain.AllocationTarget
import net.neednot.renttracker.feature.tenants.domain.MonthDetail
import net.neednot.renttracker.feature.tenants.domain.MonthLateFee
import net.neednot.renttracker.feature.tenants.domain.MonthPayment
import net.neednot.renttracker.feature.tenants.domain.OtherAllocation

internal data class MonthDetailSources(
    val calendarMonth: CalendarMonthEntity,
    val allocations: List<AllocationEntity>,
    val ledger: LedgerSources,
    val group: GroupEntity,
    val isArchived: Boolean,
)

internal fun MonthDetailSources.toMonthDetail(): MonthDetail {
    val permissions = ledger.membership?.toCallerPermissions()
    return MonthDetail(
        month = calendarMonth.toCalendarMonth(),
        payments = monthPayments(),
        lateFee = lateFee(PersonResolver(ledger.accountNames, permissions)),
        paymentOrder = group.paymentOrder,
        canAddCharge = !isArchived && permissions?.canAddCharges == true,
    )
}

private fun MonthDetailSources.monthPayments(): List<MonthPayment> {
    val countedPayments = ledger.payments.filter { it.deletedAt == null && it.bouncedAt == null }.associateBy { it.id }
    val debtsById = ledger.debts.associateBy { it.id }
    return allocations
        .filter { it.chargeKind == AllocationChargeKind.Rent && it.month == calendarMonth.month }
        .mapNotNull { allocation ->
            countedPayments[allocation.paymentId]?.let { payment ->
                payment.toMonthPayment(allocation, otherAllocationsOf(allocation, debtsById))
            }
        }.sortedWith(compareBy({ it.dateReceived }, { it.paymentId }))
}

private fun MonthDetailSources.otherAllocationsOf(
    allocation: AllocationEntity,
    debtsById: Map<String, DebtEntity>,
): List<OtherAllocation> =
    allocations
        .filter { it.paymentId == allocation.paymentId && it.key != allocation.key }
        .mapNotNull { other -> other.target(debtsById)?.let { OtherAllocation(it, other.amountCents) } }

private fun AllocationEntity.target(debtsById: Map<String, DebtEntity>): AllocationTarget? =
    when (chargeKind) {
        AllocationChargeKind.Rent -> month?.let { AllocationTarget.Rent(YearMonth.parse(it)) }
        AllocationChargeKind.Debt -> debtId?.let(debtsById::get)?.chargeKind()?.let { AllocationTarget.Charge(it) }
    }

private fun PaymentEntity.toMonthPayment(
    allocation: AllocationEntity,
    others: List<OtherAllocation>,
): MonthPayment =
    MonthPayment(
        paymentId = id,
        dateReceived = LocalDate.parse(dateReceived),
        method = method,
        paymentCents = amountCents,
        allocatedToMonthCents = allocation.amountCents,
        otherAllocations = others,
    )

private fun MonthDetailSources.lateFee(people: PersonResolver): MonthLateFee? {
    val debt =
        ledger.debts.firstOrNull {
            it.kind == DebtKind.LateFee && it.forMonth == calendarMonth.month && it.deletedAt == null
        } ?: return null
    val mayNotApply = ledger.rows.any { it.kind == LedgerRowKind.Debt && it.entryId == debt.id && it.mayNotApply }
    return MonthLateFee(
        amountCents = debt.amountCents,
        isUnpaid = calendarMonth.hasUnpaidLateFee,
        addedOn = LocalDate.parse(debt.date),
        addedBy = people.personOf(debt.createdBy),
        mayNotApply = mayNotApply,
    )
}
