package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.tenants.Debt
import net.neednot.renttracker.model.tenants.LedgerRow
import net.neednot.renttracker.model.tenants.Payment
import net.neednot.renttracker.model.tenants.DebtKind as ConvexDebtKind
import net.neednot.renttracker.model.tenants.LedgerRowKind as ConvexLedgerRowKind
import net.neednot.renttracker.model.tenants.PaymentMethod as ConvexPaymentMethod

fun Payment.toEntity(): PaymentEntity =
    PaymentEntity(
        id = id,
        creationTime = creationTime.toLong(),
        tenantId = tenantId,
        amountCents = amountCents,
        dateReceived = dateReceived,
        method = method?.toEntity(),
        note = note,
        loggedBy = loggedBy,
        bouncedAt = bouncedAt,
        bouncedBy = bouncedBy,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
        deleteReason = deleteReason,
        lastEditedAt = lastEditedAt,
        lastEditedBy = lastEditedBy,
        version = version,
    )

fun Debt.toEntity(): DebtEntity =
    DebtEntity(
        id = id,
        creationTime = creationTime.toLong(),
        tenantId = tenantId,
        kind = kind.toEntity(),
        amountCents = amountCents,
        date = date,
        forMonth = forMonth,
        note = note,
        createdBy = createdBy,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
        deleteReason = deleteReason,
        lastEditedAt = lastEditedAt,
        lastEditedBy = lastEditedBy,
        version = version,
    )

fun LedgerRow.toEntity(): LedgerRowEntity =
    LedgerRowEntity(
        tenantId = tenantId,
        key = key,
        kind = kind.toEntity(),
        entryId = entryId,
        month = month,
        date = date,
        amountCents = amountCents,
        position = position,
        balanceOwedCents = balanceOwedCents,
        balanceCreditCents = balanceCreditCents,
        mayNotApply = mayNotApply,
    )

private fun ConvexPaymentMethod.toEntity(): PaymentMethod =
    when (this) {
        ConvexPaymentMethod.Cash -> PaymentMethod.Cash
        ConvexPaymentMethod.Check -> PaymentMethod.Check
        ConvexPaymentMethod.Zelle -> PaymentMethod.Zelle
        ConvexPaymentMethod.Venmo -> PaymentMethod.Venmo
        ConvexPaymentMethod.BankTransfer -> PaymentMethod.BankTransfer
        ConvexPaymentMethod.Other -> PaymentMethod.Other
    }

private fun ConvexDebtKind.toEntity(): DebtKind =
    when (this) {
        ConvexDebtKind.LateFee -> DebtKind.LateFee
        ConvexDebtKind.Custom -> DebtKind.Custom
    }

private fun ConvexLedgerRowKind.toEntity(): LedgerRowKind =
    when (this) {
        ConvexLedgerRowKind.Rent -> LedgerRowKind.Rent
        ConvexLedgerRowKind.Debt -> LedgerRowKind.Debt
        ConvexLedgerRowKind.Payment -> LedgerRowKind.Payment
    }
