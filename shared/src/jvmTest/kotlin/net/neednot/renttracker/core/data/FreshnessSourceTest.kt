package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.sync.Freshness
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class FreshnessSourceTest {
    private val database = inMemoryDatabase()
    private val connectivity = FakeConnectivity()
    private val now = Instant.fromEpochMilliseconds(NOW)
    private val source =
        FreshnessSource(
            connectivity,
            database,
            clock =
                object : Clock {
                    override fun now() = now
                },
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun onlineDataIsFresh() =
        runTest {
            assertEquals(Freshness.Online, source.observe(listOf(CacheScope.HOME)).first())
        }

    @Test
    fun offlineDataIsAsOldAsTheOldestScopeOnTheScreen() =
        runTest {
            connectivity.isOnline.value = false
            cachedAgo(CacheScope.tenant("t1"), 10.minutes)
            cachedAgo(CacheScope.group("g1"), 40.minutes)

            val freshness = source.observe(listOf(CacheScope.tenant("t1"), CacheScope.group("g1"))).first()

            assertEquals(Freshness.Offline(40.minutes), freshness)
        }

    @Test
    fun offlineWithNothingCachedHasNoAge() =
        runTest {
            connectivity.isOnline.value = false

            assertEquals(Freshness.Offline(null), source.observe(listOf(CacheScope.HOME)).first())
        }

    private suspend fun cachedAgo(
        scope: String,
        age: Duration,
    ) {
        database.cacheTimeDao().upsert(CacheTimeEntity(scope, NOW - age.inWholeMilliseconds))
    }

    private companion object {
        const val NOW = 1_794_736_800_000L
    }
}
