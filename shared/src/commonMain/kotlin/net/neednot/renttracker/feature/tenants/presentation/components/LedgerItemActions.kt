package net.neednot.renttracker.feature.tenants.presentation.components

internal data class LedgerItemActions(
    val onEntryClicked: (key: String) -> Unit,
    val onRestoreClicked: (key: String) -> Unit,
    val onPendingClicked: (clientId: String) -> Unit,
) {
    companion object {
        val None = LedgerItemActions({}, {}, {})
    }
}
