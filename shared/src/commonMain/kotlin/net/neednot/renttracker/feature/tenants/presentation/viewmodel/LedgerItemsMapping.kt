package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.yearMonth
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.PendingPayment
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerItemUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSectionUiState

private data class DatedLedgerItem(
    val date: LocalDate,
    val item: LedgerItemUiState,
)

internal fun ledgerItems(
    entries: List<LedgerEntry>,
    pending: List<PendingPayment>,
    freshness: Freshness,
    timeZone: TimeZone,
): List<LedgerItemUiState> = mergedByDate(entries, pending, freshness, timeZone).map { it.item }

internal fun ledgerSections(
    entries: List<LedgerEntry>,
    pending: List<PendingPayment>,
    freshness: Freshness,
    timeZone: TimeZone,
): List<LedgerSectionUiState> =
    mergedByDate(entries, pending, freshness, timeZone)
        .groupBy { it.date.yearMonth }
        .map { (month, items) -> LedgerSectionUiState(header = fullMonthAndYear(month), rows = items.map { it.item }) }

private fun mergedByDate(
    entries: List<LedgerEntry>,
    pending: List<PendingPayment>,
    freshness: Freshness,
    timeZone: TimeZone,
): List<DatedLedgerItem> {
    val pendingNewestFirst =
        pending
            .reversed()
            .sortedByDescending { it.dateReceived }
            .map { DatedLedgerItem(it.dateReceived, LedgerItemUiState.Pending(it.toRowUiState())) }
            .toMutableList()
    val merged = mutableListOf<DatedLedgerItem>()
    entries.forEach { entry ->
        while (pendingNewestFirst.firstOrNull()?.let { it.date >= entry.date } == true) {
            merged += pendingNewestFirst.removeFirst()
        }
        merged += DatedLedgerItem(entry.date, LedgerItemUiState.Entry(entry.toRowUiState(timeZone, freshness)))
    }
    return merged + pendingNewestFirst
}

private fun LedgerEntry.toRowUiState(
    timeZone: TimeZone,
    freshness: Freshness,
): LedgerRowUiState =
    toRowUiState(timeZone).let { row ->
        when (freshness) {
            Freshness.Online -> row
            is Freshness.Offline -> row.copy(canRestore = false)
        }
    }
