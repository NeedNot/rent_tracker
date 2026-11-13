package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

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
    nextDueDate: LocalDate? = LocalDate(2026, 4, 1),
    partialMoveOutPeriod: ClosedRange<LocalDate>? = null,
) = TenantBalance(
    state = state,
    total = BalanceTotal(owedCents = owedCents, creditCents = creditCents),
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
) = TenantOverview(
    tenant = Tenant(tenantId = "t1", groupId = "g1", isArchived = isArchived, version = version, details = details),
    group = group,
    balance = balance,
    activeRentTerms = activeRentTerms,
    today = today,
)

fun synced(overview: TenantOverview?): Synced<TenantOverview?, TenantError> = Synced(overview, SyncStatus.Synced)
