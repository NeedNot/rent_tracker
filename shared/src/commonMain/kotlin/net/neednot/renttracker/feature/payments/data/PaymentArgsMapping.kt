package net.neednot.renttracker.feature.payments.data

import net.neednot.renttracker.feature.payments.domain.DuplicatePolicy
import net.neednot.renttracker.feature.payments.domain.LateFeeDraft
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.model.payments.PaymentsEditArgs
import net.neednot.renttracker.model.payments.PaymentsEditMethod
import net.neednot.renttracker.model.payments.PaymentsLogArgs
import net.neednot.renttracker.model.payments.PaymentsLogLateFee
import net.neednot.renttracker.model.payments.PaymentsLogMethod

internal fun PaymentDraft.toLogArgs(
    clientId: String,
    tenantId: String,
    lateFee: LateFeeDraft?,
    duplicatePolicy: DuplicatePolicy,
): PaymentsLogArgs =
    PaymentsLogArgs(
        tenantId = tenantId,
        clientId = clientId,
        amountCents = amountCents,
        dateReceived = dateReceived.toString(),
        method = method?.let { PaymentsLogMethod.valueOf(it.name) },
        note = note,
        lateFee = lateFee?.let { PaymentsLogLateFee(amountCents = it.amountCents, forMonth = it.forMonth.toString()) },
        confirmed = if (duplicatePolicy == DuplicatePolicy.SaveAnyway) true else null,
    )

internal fun PaymentDraft.toEditArgs(
    paymentId: String,
    expectedVersion: Long,
): PaymentsEditArgs =
    PaymentsEditArgs(
        paymentId = paymentId,
        expectedVersion = expectedVersion,
        amountCents = amountCents,
        dateReceived = dateReceived.toString(),
        method = method?.let { PaymentsEditMethod.valueOf(it.name) },
        note = note,
    )
