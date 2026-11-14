package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import kotlin.time.Instant

fun tenantDetails(
    name: String = "Apt 3B",
    moveIn: LocalDate = LocalDate(2024, 1, 1),
    moveOut: LocalDate? = null,
    graceDaysOverride: Int? = null,
    lateFeeFlatOverrideCents: Long? = null,
) = TenantDetails(
    name = name,
    contactName = "Dana Whitfield",
    contactPhone = "(555) 014-2290",
    contactEmail = "dana.w@example.com",
    notes = "Two roommates on the lease.",
    depositCents = 150_000,
    moveIn = moveIn,
    moveOut = moveOut,
    graceDaysOverride = graceDaysOverride,
    lateFeeFlatOverrideCents = lateFeeFlatOverrideCents,
)

fun rentTerm(
    rentTermId: String,
    startMonth: String,
    amountCents: Long = 150_000,
    dueDay: Int = 1,
    version: Long = 1,
) = RentTerm(
    rentTermId = rentTermId,
    startMonth = YearMonth.parse(startMonth),
    amountCents = amountCents,
    dueDay = dueDay,
    version = version,
)

fun tenantBalance(
    state: TenantBalanceState = TenantBalanceState.PaidUp,
    owedCents: Long = 0,
    creditCents: Long = 0,
    owedBreakdown: OwedBreakdown = OwedBreakdown(rentCents = owedCents, lateFeesCents = 0, otherCents = 0),
    nextDueDate: LocalDate? = LocalDate(2026, 4, 1),
    partialMoveOutPeriod: ClosedRange<LocalDate>? = null,
) = TenantBalance(
    state = state,
    total = BalanceTotal(owedCents = owedCents, creditCents = creditCents),
    owedBreakdown = owedBreakdown,
    nextDueDate = nextDueDate,
    partialMoveOutPeriod = partialMoveOutPeriod,
)

fun tenantOverview(
    details: TenantDetails = tenantDetails(),
    isArchived: Boolean = false,
    version: Long = 3,
    group: TenantGroup = TenantGroup(name = "1st Street", graceDays = 5, lateFeeFlatCents = null),
    balance: TenantBalance? = tenantBalance(),
    activeRentTerms: List<RentTerm> = listOf(rentTerm("r1", "2024-01")),
    today: LocalDate = LocalDate(2026, 3, 10),
    ledger: List<LedgerEntry> = emptyList(),
    permissions: CallerPermissions? = callerPermissions(),
    lateFeeSuggestion: LateFeeSuggestion? = null,
) = TenantOverview(
    tenant = Tenant(tenantId = "t1", groupId = "g1", isArchived = isArchived, version = version, details = details),
    group = group,
    balance = balance,
    activeRentTerms = activeRentTerms,
    today = today,
    ledger = ledger,
    permissions = permissions,
    lateFeeSuggestion = lateFeeSuggestion,
)

fun synced(overview: TenantOverview?): Synced<TenantOverview?, TenantError> = Synced(overview, SyncStatus.Synced)

fun callerPermissions(
    accountId: String = "me",
    isOwner: Boolean = true,
    payments: AccessLevel = AccessLevel.Full,
    debts: AccessLevel = AccessLevel.Full,
    editOthers: Boolean = false,
) = CallerPermissions(
    accountId = accountId,
    isOwner = isOwner,
    payments = payments,
    debts = debts,
    editOthers = editOthers,
)

fun paymentEntry(
    key: String = "payment:p1",
    date: LocalDate = LocalDate(2026, 3, 2),
    amountCents: Long = 150_000,
    balance: LedgerBalance? = LedgerBalance.Owed(67_000),
    paymentId: String = "p1",
    version: Long = 4,
    method: PaymentMethod? = PaymentMethod.Zelle,
    loggedAt: Instant = Instant.parse("2026-03-02T18:00:00Z"),
    loggedBy: Person = Person.You,
    status: EntryStatus = EntryStatus.Active,
    edit: EntryEdit? = null,
    canChange: Boolean = true,
) = LedgerEntry.Payment(
    key = key,
    date = date,
    amountCents = amountCents,
    balance = balance,
    payment =
        PaymentDetails(
            paymentId = paymentId,
            version = version,
            method = method,
            note = null,
            dateReceived = date,
            loggedAt = loggedAt,
            loggedBy = loggedBy,
            status = status,
            edit = edit,
            canChange = canChange,
        ),
)

fun chargeEntry(
    key: String = "debt:d1",
    date: LocalDate = LocalDate(2026, 3, 1),
    amountCents: Long = 12_000,
    balance: LedgerBalance? = LedgerBalance.Owed(217_000),
    debtId: String = "d1",
    version: Long = 2,
    kind: ChargeKind = ChargeKind.Custom("Water bill, Feb"),
    addedBy: Person = Person.You,
    status: EntryStatus = EntryStatus.Active,
    edit: EntryEdit? = null,
    mayNotApply: Boolean = false,
    canChange: Boolean = true,
) = LedgerEntry.Charge(
    key = key,
    date = date,
    amountCents = amountCents,
    balance = balance,
    charge =
        ChargeDetails(
            debtId = debtId,
            version = version,
            kind = kind,
            note = null,
            date = date,
            addedAt = Instant.parse("2026-03-01T18:00:00Z"),
            addedBy = addedBy,
            status = status,
            edit = edit,
            mayNotApply = mayNotApply,
            canChange = canChange,
        ),
)

fun rentEntry(
    month: String = "2026-03",
    date: LocalDate = LocalDate(2026, 3, 1),
    amountCents: Long = 150_000,
    balance: LedgerBalance? = LedgerBalance.Owed(205_000),
) = LedgerEntry.Rent(
    key = "rent:$month",
    date = date,
    amountCents = amountCents,
    balance = balance,
    month = YearMonth.parse(month),
)
