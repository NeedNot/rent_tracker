package net.neednot.renttracker.core.data

fun groupEntity(
    id: String,
    name: String = "Group $id",
    creationTime: Long = 0,
) = GroupEntity(
    id = id,
    creationTime = creationTime,
    name = name,
    timeZone = "America/Chicago",
    paymentOrder = PaymentOrder.RentFirst,
    graceDays = 5,
    lateFeeFlatCents = null,
    ownerId = "owner",
    archivedAt = null,
    lastEditedAt = null,
    lastEditedBy = null,
    version = 1,
)

fun groupSummaryEntity(
    groupId: String,
    totalOwedCents: Long = 0,
    activeTenantCount: Long = 0,
    lateTenantCount: Long = 0,
    archivedOwedCents: Long = 0,
) = GroupSummaryEntity(
    groupId = groupId,
    activeTenantCount = activeTenantCount,
    lateTenantCount = lateTenantCount,
    totalOwedCents = totalOwedCents,
    archivedTenantCount = 0,
    archivedOwedCents = archivedOwedCents,
)

fun tenantEntity(
    id: String,
    groupId: String,
    name: String = "Tenant $id",
    status: TenantStatus = TenantStatus.Active,
) = TenantEntity(
    id = id,
    creationTime = 0,
    groupId = groupId,
    name = name,
    status = status,
    moveIn = "2026-01-01",
    moveOut = null,
    depositCents = null,
    contactName = null,
    contactPhone = null,
    contactEmail = null,
    notes = null,
    graceDaysOverride = null,
    lateFeeFlatOverrideCents = null,
    lastPaymentMethod = null,
    lastEditedAt = null,
    lastEditedBy = null,
    version = 1,
)

fun tenantSummaryEntity(
    tenantId: String,
    state: TenantBalanceState = TenantBalanceState.PaidUp,
    rentOwedCents: Long = 0,
) = TenantSummaryEntity(
    tenantId = tenantId,
    state = state,
    rentOwedCents = rentOwedCents,
    lateFeesOwedCents = 0,
    otherOwedCents = 0,
    creditCents = 0,
    nextDueDate = null,
    partialMoveOutStart = null,
    partialMoveOutEnd = null,
)

fun rentTermEntity(
    id: String,
    tenantId: String,
    startMonth: String = "2026-01",
    amountCents: Long = 150_000,
) = RentTermEntity(
    id = id,
    creationTime = 0,
    tenantId = tenantId,
    startMonth = startMonth,
    amountCents = amountCents,
    dueDay = 1,
    deletedAt = null,
    deletedBy = null,
    deleteReason = null,
    lastEditedAt = null,
    lastEditedBy = null,
    version = 1,
)
