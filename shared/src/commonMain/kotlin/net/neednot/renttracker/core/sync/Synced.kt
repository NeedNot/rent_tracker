package net.neednot.renttracker.core.sync

data class Synced<out T, out E>(
    val value: T,
    val status: SyncStatus<E>,
)
