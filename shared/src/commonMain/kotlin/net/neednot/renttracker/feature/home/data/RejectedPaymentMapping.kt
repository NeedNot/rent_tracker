package net.neednot.renttracker.feature.home.data

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PendingPaymentEntity
import net.neednot.renttracker.core.data.PendingRejectionReason
import net.neednot.renttracker.feature.home.domain.RejectedPayment
import net.neednot.renttracker.feature.home.domain.RejectedPaymentReason

internal fun PendingPaymentEntity.toRejectedPayment(): RejectedPayment =
    RejectedPayment(
        clientId = clientId,
        tenantName = tenantName,
        amountCents = amountCents,
        dateReceived = LocalDate.parse(dateReceived),
        reason = rejectionReason.toRejectedPaymentReason(),
        message = rejectionMessage,
    )

private fun PendingRejectionReason?.toRejectedPaymentReason(): RejectedPaymentReason =
    when (this) {
        PendingRejectionReason.NoPermission -> RejectedPaymentReason.NoPermission
        PendingRejectionReason.TenantArchived -> RejectedPaymentReason.TenantArchived
        PendingRejectionReason.TenantMoved -> RejectedPaymentReason.TenantMoved
        PendingRejectionReason.Other, null -> RejectedPaymentReason.Other
    }
