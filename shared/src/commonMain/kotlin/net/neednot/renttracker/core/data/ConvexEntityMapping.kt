package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.shared.Group
import net.neednot.renttracker.model.shared.GroupPaymentOrder
import net.neednot.renttracker.model.shared.GroupSummary
import net.neednot.renttracker.model.shared.Tenant
import net.neednot.renttracker.model.shared.TenantLastPaymentMethod
import net.neednot.renttracker.model.shared.TenantSummary
import net.neednot.renttracker.model.shared.TenantSummaryState
import net.neednot.renttracker.model.tenants.RentTerm
import net.neednot.renttracker.model.shared.TenantStatus as ConvexTenantStatus

fun Group.toEntity(): GroupEntity =
    GroupEntity(
        id = id,
        creationTime = creationTime.toLong(),
        name = name,
        timeZone = timeZone,
        paymentOrder = paymentOrder.toEntity(),
        graceDays = graceDays,
        lateFeeFlatCents = lateFeeFlatCents,
        ownerId = ownerId,
        archivedAt = archivedAt,
        lastEditedAt = lastEditedAt,
        lastEditedBy = lastEditedBy,
        version = version,
    )

fun GroupSummary.toEntity(): GroupSummaryEntity =
    GroupSummaryEntity(
        groupId = groupId,
        activeTenantCount = activeTenantCount,
        lateTenantCount = lateTenantCount,
        totalOwedCents = totalOwedCents,
        archivedTenantCount = archivedTenantCount,
        archivedOwedCents = archivedOwedCents,
    )

fun Tenant.toEntity(): TenantEntity =
    TenantEntity(
        id = id,
        creationTime = creationTime.toLong(),
        groupId = groupId,
        name = name,
        status = status.toEntity(),
        moveIn = moveIn,
        moveOut = moveOut,
        depositCents = depositCents,
        contactName = contactName,
        contactPhone = contactPhone,
        contactEmail = contactEmail,
        notes = notes,
        graceDaysOverride = graceDaysOverride,
        lateFeeFlatOverrideCents = lateFeeFlatOverrideCents,
        lastPaymentMethod = lastPaymentMethod?.toEntity(),
        lastEditedAt = lastEditedAt,
        lastEditedBy = lastEditedBy,
        version = version,
    )

fun TenantSummary.toEntity(): TenantSummaryEntity =
    TenantSummaryEntity(
        tenantId = tenantId,
        state = state.toEntity(),
        rentOwedCents = rentOwedCents,
        lateFeesOwedCents = lateFeesOwedCents,
        otherOwedCents = otherOwedCents,
        creditCents = creditCents,
        nextDueDate = nextDueDate,
        partialMoveOutStart = partialMoveOutPeriod?.start,
        partialMoveOutEnd = partialMoveOutPeriod?.end,
    )

fun RentTerm.toEntity(): RentTermEntity =
    RentTermEntity(
        id = id,
        creationTime = creationTime.toLong(),
        tenantId = tenantId,
        startMonth = startMonth,
        amountCents = amountCents,
        dueDay = dueDay,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
        deleteReason = deleteReason,
        lastEditedAt = lastEditedAt,
        lastEditedBy = lastEditedBy,
        version = version,
    )

private fun GroupPaymentOrder.toEntity(): PaymentOrder =
    when (this) {
        GroupPaymentOrder.RentFirst -> PaymentOrder.RentFirst
        GroupPaymentOrder.ByDate -> PaymentOrder.ByDate
    }

private fun ConvexTenantStatus.toEntity(): TenantStatus =
    when (this) {
        ConvexTenantStatus.Active -> TenantStatus.Active
        ConvexTenantStatus.Archived -> TenantStatus.Archived
    }

private fun TenantSummaryState.toEntity(): TenantBalanceState =
    when (this) {
        TenantSummaryState.Late -> TenantBalanceState.Late
        TenantSummaryState.Owes -> TenantBalanceState.Owes
        TenantSummaryState.PaidUp -> TenantBalanceState.PaidUp
    }

private fun TenantLastPaymentMethod.toEntity(): PaymentMethod =
    when (this) {
        TenantLastPaymentMethod.Cash -> PaymentMethod.Cash
        TenantLastPaymentMethod.Check -> PaymentMethod.Check
        TenantLastPaymentMethod.Zelle -> PaymentMethod.Zelle
        TenantLastPaymentMethod.Venmo -> PaymentMethod.Venmo
        TenantLastPaymentMethod.BankTransfer -> PaymentMethod.BankTransfer
        TenantLastPaymentMethod.Other -> PaymentMethod.Other
    }
