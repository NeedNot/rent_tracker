package net.neednot.renttracker.core.sync

sealed interface SyncStatus<out E> {
    data object Syncing : SyncStatus<Nothing>

    data object Synced : SyncStatus<Nothing>

    data class Failed<out E>(
        val error: E,
    ) : SyncStatus<E>
}
