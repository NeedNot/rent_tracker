package net.neednot.renttracker.feature.debts.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

fun chargeOverview(
    balance: DebtBalance? = DebtBalance(owedCents = 55_000, creditCents = 0),
    today: LocalDate = LocalDate(2026, 3, 10),
    callerAccountId: String? = "account",
    charge: SavedCharge? = null,
) = ChargeEntryOverview(
    tenantName = "Apt 3B",
    groupName = "1st Street",
    balance = balance,
    today = today,
    callerAccountId = callerAccountId,
    charge = charge,
)

fun savedLateFee(
    version: Long = 2,
    amountCents: Long = 5_000,
    date: LocalDate = LocalDate(2026, 2, 6),
    forMonth: YearMonth = YearMonth(2026, 2),
    note: String? = "Paid Feb 4, after grace",
) = SavedCharge("d1", version, ChargeDraft.LateFee(amountCents, date, forMonth, note))

fun savedCustom(
    version: Long = 2,
    note: String = "Water bill, Feb",
) = SavedCharge("d1", version, ChargeDraft.Custom(4_250, LocalDate(2026, 3, 1), note))

fun synced(overview: ChargeEntryOverview?): Synced<ChargeEntryOverview?, DebtError> =
    Synced(overview, SyncStatus.Synced)
