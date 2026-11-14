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
    lateFeeSuggestionMonth = null,
    lateFeeSuggestionCents = null,
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

fun paymentEntity(
    id: String,
    tenantId: String,
    amountCents: Long = 150_000,
) = PaymentEntity(
    id = id,
    creationTime = 0,
    tenantId = tenantId,
    amountCents = amountCents,
    dateReceived = "2026-03-01",
    method = null,
    note = null,
    loggedBy = "account",
    bouncedAt = null,
    bouncedBy = null,
    deletedAt = null,
    deletedBy = null,
    deleteReason = null,
    lastEditedAt = null,
    lastEditedBy = null,
    version = 1,
)

fun debtEntity(
    id: String,
    tenantId: String,
    kind: DebtKind = DebtKind.Custom,
) = DebtEntity(
    id = id,
    creationTime = 0,
    tenantId = tenantId,
    kind = kind,
    amountCents = 5_000,
    date = "2026-03-10",
    forMonth = null,
    note = null,
    createdBy = "account",
    deletedAt = null,
    deletedBy = null,
    deleteReason = null,
    lastEditedAt = null,
    lastEditedBy = null,
    version = 1,
)

fun ledgerRowEntity(
    tenantId: String,
    key: String,
    position: Long,
    kind: LedgerRowKind = LedgerRowKind.Rent,
) = LedgerRowEntity(
    tenantId = tenantId,
    key = key,
    kind = kind,
    entryId = null,
    month = null,
    date = "2026-03-01",
    amountCents = 150_000,
    position = position,
    balanceOwedCents = null,
    balanceCreditCents = null,
    mayNotApply = false,
)

fun accountNameEntity(
    accountId: String,
    displayName: String = "Name $accountId",
) = AccountNameEntity(accountId = accountId, displayName = displayName)

fun membershipEntity(
    id: String,
    groupId: String,
    role: MembershipRole = MembershipRole.Owner,
) = MembershipEntity(
    id = id,
    groupId = groupId,
    accountId = "account",
    role = role,
    tenants = AccessLevel.Full,
    rentTerms = AccessLevel.Full,
    payments = AccessLevel.Full,
    debts = AccessLevel.Full,
    members = AccessLevel.Full,
    settings = SettingsAccessLevel.Full,
    editOthers = true,
)

fun calendarMonthEntity(
    tenantId: String,
    month: String,
    state: CalendarMonthState = CalendarMonthState.Paid,
    rentCents: Long = 150_000,
    coveredCents: Long = 150_000,
    owedCents: Long = 0,
    hasUnpaidLateFee: Boolean = false,
) = CalendarMonthEntity(
    tenantId = tenantId,
    month = month,
    state = state,
    rentCents = rentCents,
    coveredCents = coveredCents,
    owedCents = owedCents,
    hasUnpaidLateFee = hasUnpaidLateFee,
    dueDate = "$month-01",
    graceEnd = "$month-06",
    wasLate = false,
    paidByGraceEndCents = null,
    lateFeeCents = null,
    lateFeeUnpaidCents = null,
    suggestedLateFeeCents = null,
)

fun allocationEntity(
    tenantId: String,
    key: String,
    paymentId: String = "p1",
    month: String? = "2026-03",
    amountCents: Long = 100_000,
) = AllocationEntity(
    tenantId = tenantId,
    key = key,
    paymentId = paymentId,
    chargeKind = AllocationChargeKind.Rent,
    month = month,
    debtId = null,
    amountCents = amountCents,
)
