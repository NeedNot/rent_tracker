package net.neednot.renttracker.feature.debts.data

import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.domain.DuplicatePolicy
import net.neednot.renttracker.model.debts.DebtsAddCustomArgs
import net.neednot.renttracker.model.debts.DebtsAddLateFeeArgs
import net.neednot.renttracker.model.debts.DebtsEditArgs

internal fun ChargeDraft.LateFee.toAddArgs(
    clientId: String,
    tenantId: String,
    duplicatePolicy: DuplicatePolicy,
): DebtsAddLateFeeArgs =
    DebtsAddLateFeeArgs(
        tenantId = tenantId,
        clientId = clientId,
        amountCents = amountCents,
        date = date.toString(),
        forMonth = forMonth.toString(),
        note = note,
        confirmed = if (duplicatePolicy == DuplicatePolicy.AddAnyway) true else null,
    )

internal fun ChargeDraft.Custom.toAddArgs(
    clientId: String,
    tenantId: String,
): DebtsAddCustomArgs =
    DebtsAddCustomArgs(
        tenantId = tenantId,
        clientId = clientId,
        amountCents = amountCents,
        date = date.toString(),
        note = note,
    )

internal fun ChargeDraft.toEditArgs(
    debtId: String,
    expectedVersion: Long,
): DebtsEditArgs =
    when (this) {
        is ChargeDraft.LateFee -> {
            DebtsEditArgs(
                debtId = debtId,
                expectedVersion = expectedVersion,
                amountCents = amountCents,
                date = date.toString(),
                forMonth = forMonth.toString(),
                note = note,
            )
        }

        is ChargeDraft.Custom -> {
            DebtsEditArgs(
                debtId = debtId,
                expectedVersion = expectedVersion,
                amountCents = amountCents,
                date = date.toString(),
                note = note,
            )
        }
    }
