package net.neednot.renttracker.core.sync

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R, E> syncedFlow(
    cached: Flow<T>,
    serverResults: Flow<Result<R, E>>,
    store: suspend (R) -> Unit,
): Flow<Synced<T, E>> =
    serverResults
        .transform<Result<R, E>, SyncStatus<E>?> { result ->
            // A null stops the cache reader before the store, so rows written by this result are never
            // emitted with the previous status.
            emit(null)
            emit(
                result.fold(
                    success = { value ->
                        store(value)
                        SyncStatus.Synced
                    },
                    failure = { error -> SyncStatus.Failed(error) },
                ),
            )
        }.onStart { emit(SyncStatus.Syncing) }
        .flatMapLatest { status -> if (status == null) emptyFlow() else cached.map { value -> Synced(value, status) } }
        .distinctUntilChanged()
