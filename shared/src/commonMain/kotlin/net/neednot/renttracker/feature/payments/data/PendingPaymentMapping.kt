package net.neednot.renttracker.feature.payments.data

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PendingPaymentEntity
import net.neednot.renttracker.core.data.PendingQueueError
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PendingPayment

internal fun PendingPaymentEntity.toPendingPayment(): PendingPayment =
    PendingPayment(
        tenantName = tenantName,
        details =
            PaymentDraft(
                amountCents = amountCents,
                dateReceived = LocalDate.parse(dateReceived),
                method = method,
                note = note,
            ),
    )

internal fun PendingQueueError.toPaymentError(): PaymentError =
    when (this) {
        PendingQueueError.NotFound -> PaymentError.NotFound
        PendingQueueError.NotEditable -> PaymentError.NotFound
        PendingQueueError.TenantNotCached -> PaymentError.Unexpected("The tenant is not on this phone.")
    }
