package net.neednot.renttracker.feature.tenants.presentation.uistate

sealed interface LedgerItemUiState {
    val key: String

    data class Entry(
        val row: LedgerRowUiState,
    ) : LedgerItemUiState {
        override val key: String get() = row.key
    }

    data class Pending(
        val row: PendingRowUiState,
    ) : LedgerItemUiState {
        override val key: String get() = "pending:${row.clientId}"
    }
}
