package net.neednot.renttracker.core.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import net.neednot.renttracker.core.sync.Freshness
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class FreshnessSource(
    private val connectivity: Connectivity,
    database: RentTrackerDatabase,
    private val clock: Clock,
) {
    private val cacheTimeDao = database.cacheTimeDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(scopes: List<String>): Flow<Freshness> =
        connectivity.isOnline
            .flatMapLatest { online ->
                if (online) {
                    flowOf(Freshness.Online)
                } else {
                    combine(cacheTimeDao.observeOldest(scopes), everyMinute()) { syncedAt, now ->
                        Freshness.Offline(syncedAt?.let { (now - it).coerceAtLeast(0).milliseconds })
                    }
                }
            }.distinctUntilChanged()

    private fun everyMinute(): Flow<Long> =
        flow {
            while (true) {
                emit(clock.now().toEpochMilliseconds())
                delay(1.minutes)
            }
        }
}
