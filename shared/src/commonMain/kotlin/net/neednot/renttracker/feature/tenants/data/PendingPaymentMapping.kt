package net.neednot.renttracker.feature.tenants.data

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PendingConfirmationReason
import net.neednot.renttracker.core.data.PendingDuplicateMatchEntity
import net.neednot.renttracker.core.data.PendingPaymentEntity
import net.neednot.renttracker.core.data.PendingPaymentStatus
import net.neednot.renttracker.core.data.PendingRejectionReason
import net.neednot.renttracker.core.data.TenantSummaryEntity
import net.neednot.renttracker.feature.tenants.domain.PendingDuplicateMatch
import net.neednot.renttracker.feature.tenants.domain.PendingPayment
import net.neednot.renttracker.feature.tenants.domain.PendingRejection
import net.neednot.renttracker.feature.tenants.domain.PendingState

internal fun PendingPaymentEntity.toPendingPayment(): PendingPayment =
    PendingPayment(
        clientId = clientId,
        tenantName = tenantName,
        amountCents = amountCents,
        dateReceived = LocalDate.parse(dateReceived),
        method = method,
        state = pendingState(),
    )

private fun PendingPaymentEntity.pendingState(): PendingState =
    when (status) {
        PendingPaymentStatus.Pending -> PendingState.Waiting
        PendingPaymentStatus.NeedsConfirmation -> confirmationState()
        PendingPaymentStatus.Rejected -> PendingState.Rejected(rejection())
    }

private fun PendingPaymentEntity.confirmationState(): PendingState =
    when (confirmationReason) {
        PendingConfirmationReason.PossibleDuplicate -> PendingState.PossibleDuplicate
        PendingConfirmationReason.TenantChanged, null -> PendingState.TenantChanged
    }

private fun PendingPaymentEntity.rejection(): PendingRejection =
    when (rejectionReason) {
        PendingRejectionReason.NoPermission -> PendingRejection.NoPermission
        PendingRejectionReason.TenantArchived -> PendingRejection.TenantArchived
        PendingRejectionReason.TenantMoved -> PendingRejection.TenantMoved
        PendingRejectionReason.Other, null -> PendingRejection.Other(rejectionMessage.orEmpty())
    }

internal fun PendingDuplicateMatchEntity.toDuplicateMatch(): PendingDuplicateMatch =
    PendingDuplicateMatch(
        kind = kind,
        date = LocalDate.parse(date),
        amountCents = amountCents,
        method = method,
        byName = byName,
    )

internal fun TenantSummaryEntity.balanceCents(): Long = rentOwedCents + lateFeesOwedCents + otherOwedCents - creditCents
