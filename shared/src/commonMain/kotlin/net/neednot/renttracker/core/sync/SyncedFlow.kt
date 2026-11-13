package net.neednot.renttracker.core.sync

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R, E> syncedFlow(
    cached: Flow<T>,
    serverResults: Flow<Result<R, E>>,
    store: suspend (R) -> Unit,
): Flow<Synced<T, E>> =
    serverResults
        .map { result ->
            result.fold(
                success = { value ->
                    store(value)
                    SyncStatus.Synced
                },
                failure = { error -> SyncStatus.Failed(error) },
            )
        }.onStart { emit(SyncStatus.Syncing) }
        // Reading the cache again after each store keeps "Synced" from ever being paired with the rows it replaced.
        .flatMapLatest { status -> cached.map { value -> Synced(value, status) } }
        .distinctUntilChanged()
