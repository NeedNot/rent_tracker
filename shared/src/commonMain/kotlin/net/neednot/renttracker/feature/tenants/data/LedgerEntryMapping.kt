package net.neednot.renttracker.feature.tenants.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtEntity
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.data.LedgerRowEntity
import net.neednot.renttracker.core.data.LedgerRowKind
import net.neednot.renttracker.core.data.PaymentEntity
import net.neednot.renttracker.feature.tenants.domain.CallerPermissions
import net.neednot.renttracker.feature.tenants.domain.ChargeDetails
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryEdit
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.LedgerBalance
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.PaymentDetails
import net.neednot.renttracker.feature.tenants.domain.Person
import kotlin.time.Instant

internal fun LedgerSources.toLedgerEntries(
    permissions: CallerPermissions?,
    isArchived: Boolean,
): List<LedgerEntry> {
    val mapper = LedgerEntryMapper(this, permissions, isArchived)
    return rows.mapNotNull(mapper::entryOf)
}

private class LedgerEntryMapper(
    sources: LedgerSources,
    private val permissions: CallerPermissions?,
    private val isArchived: Boolean,
) {
    private val paymentsById = sources.payments.associateBy { it.id }
    private val debtsById = sources.debts.associateBy { it.id }
    private val people = PersonResolver(sources.accountNames, permissions)

    fun entryOf(row: LedgerRowEntity): LedgerEntry? =
        when (row.kind) {
            LedgerRowKind.Rent -> rentEntry(row)
            LedgerRowKind.Payment -> row.entryId?.let(paymentsById::get)?.let { paymentEntry(row, it) }
            LedgerRowKind.Debt -> row.entryId?.let(debtsById::get)?.let { chargeEntry(row, it) }
        }

    private fun rentEntry(row: LedgerRowEntity): LedgerEntry.Rent? =
        row.month?.let { month ->
            LedgerEntry.Rent(
                key = row.key,
                date = LocalDate.parse(row.date),
                amountCents = row.amountCents,
                balance = row.toBalance(),
                month = YearMonth.parse(month),
            )
        }

    private fun paymentEntry(
        row: LedgerRowEntity,
        payment: PaymentEntity,
    ): LedgerEntry.Payment =
        LedgerEntry.Payment(
            key = row.key,
            date = LocalDate.parse(row.date),
            amountCents = row.amountCents,
            balance = row.toBalance(),
            payment =
                PaymentDetails(
                    paymentId = payment.id,
                    version = payment.version,
                    method = payment.method,
                    note = payment.note,
                    dateReceived = LocalDate.parse(payment.dateReceived),
                    loggedAt = Instant.fromEpochMilliseconds(payment.creationTime),
                    loggedBy = personOf(payment.loggedBy),
                    status = payment.status(),
                    edit = editOf(payment.lastEditedBy, payment.lastEditedAt),
                    canChange = !isArchived && permissions?.canChangePayment(payment.loggedBy) == true,
                ),
        )

    private fun chargeEntry(
        row: LedgerRowEntity,
        debt: DebtEntity,
    ): LedgerEntry.Charge? {
        val kind = debt.chargeKind() ?: return null
        return LedgerEntry.Charge(
            key = row.key,
            date = LocalDate.parse(row.date),
            amountCents = row.amountCents,
            balance = row.toBalance(),
            charge =
                ChargeDetails(
                    debtId = debt.id,
                    version = debt.version,
                    kind = kind,
                    note = debt.note,
                    date = LocalDate.parse(debt.date),
                    addedAt = Instant.fromEpochMilliseconds(debt.creationTime),
                    addedBy = personOf(debt.createdBy),
                    status = debt.status(),
                    edit = editOf(debt.lastEditedBy, debt.lastEditedAt),
                    mayNotApply = row.mayNotApply,
                    canChange = !isArchived && permissions?.canChangeCharge(debt.createdBy) == true,
                ),
        )
    }

    private fun PaymentEntity.status(): EntryStatus =
        when {
            deletedAt != null -> {
                cancelled(deletedAt, deletedBy, deleteReason)
            }

            bouncedAt != null -> {
                EntryStatus.Bounced(
                    by = personOf(bouncedBy),
                    at = Instant.fromEpochMilliseconds(bouncedAt),
                )
            }

            else -> {
                EntryStatus.Active
            }
        }

    private fun DebtEntity.status(): EntryStatus =
        if (deletedAt != null) cancelled(deletedAt, deletedBy, deleteReason) else EntryStatus.Active

    private fun cancelled(
        at: Long,
        by: String?,
        reason: String?,
    ): EntryStatus.Cancelled =
        EntryStatus.Cancelled(reason = reason.orEmpty(), by = personOf(by), at = Instant.fromEpochMilliseconds(at))

    private fun editOf(
        by: String?,
        at: Long?,
    ): EntryEdit? = if (at == null) null else EntryEdit(by = personOf(by), at = Instant.fromEpochMilliseconds(at))

    private fun personOf(accountId: String?): Person = people.personOf(accountId)
}

internal fun DebtEntity.chargeKind(): ChargeKind? =
    when (kind) {
        DebtKind.LateFee -> forMonth?.let { ChargeKind.LateFee(YearMonth.parse(it)) }
        DebtKind.Custom -> ChargeKind.Custom(note.orEmpty())
    }

private fun LedgerRowEntity.toBalance(): LedgerBalance? =
    when {
        balanceOwedCents == 0L -> LedgerBalance.PaidUp
        balanceOwedCents != null -> LedgerBalance.Owed(balanceOwedCents)
        balanceCreditCents != null -> LedgerBalance.Credit(balanceCreditCents)
        else -> null
    }
