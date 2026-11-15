package net.neednot.renttracker.feature.group.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.data.CacheScope
import net.neednot.renttracker.core.data.CacheTimeEntity
import net.neednot.renttracker.core.data.FakeConnectivity
import net.neednot.renttracker.core.data.FreshnessSource
import net.neednot.renttracker.core.data.inMemoryDatabase
import net.neednot.renttracker.core.sync.Freshness
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class GroupFreshnessRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val connectivity = FakeConnectivity()
    private val now = Instant.fromEpochMilliseconds(NOW)
    private val repository =
        GroupFreshnessRepositoryImpl(
            FreshnessSource(
                connectivity,
                database,
                clock =
                    object : Clock {
                        override fun now() = now
                    },
            ),
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun freshnessIsOnlineWhileConnected() =
        runTest {
            assertEquals(Freshness.Online, repository.observeFreshness("g1").first())
        }

    @Test
    fun offlineFreshnessIsTheAgeOfThatGroupsCache() =
        runTest {
            connectivity.isOnline.value = false
            database.cacheTimeDao().upsert(
                CacheTimeEntity(CacheScope.group("g1"), NOW - 15.minutes.inWholeMilliseconds),
            )
            database.cacheTimeDao().upsert(
                CacheTimeEntity(CacheScope.group("g2"), NOW - 90.minutes.inWholeMilliseconds),
            )

            assertEquals(Freshness.Offline(15.minutes), repository.observeFreshness("g1").first())
        }

    @Test
    fun offlineFreshnessHasNoAgeWhenTheGroupWasNeverCached() =
        runTest {
            connectivity.isOnline.value = false

            assertEquals(Freshness.Offline(null), repository.observeFreshness("g1").first())
        }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
