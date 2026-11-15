package net.neednot.renttracker.feature.tenants.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.CacheScope
import net.neednot.renttracker.core.data.CacheTimeEntity
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.FakeConnectivity
import net.neednot.renttracker.core.data.FreshnessSource
import net.neednot.renttracker.core.data.groupEntity
import net.neednot.renttracker.core.data.inMemoryDatabase
import net.neednot.renttracker.core.data.tenantEntity
import net.neednot.renttracker.core.sync.Freshness
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class TenantFreshnessRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val connectivity = FakeConnectivity()
    private val fixedClock =
        object : Clock {
            override fun now() = Instant.fromEpochMilliseconds(TEN_O_CLOCK_UTC)
        }
    private val repository =
        TenantFreshnessRepositoryImpl(
            database = database,
            asOfClock = FakeAsOfClock(TEN_O_CLOCK_UTC),
            freshnessSource = FreshnessSource(connectivity, database, fixedClock),
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun onlineEveryScreenIsFresh() =
        runTest {
            cacheTenant()

            assertEquals(Freshness.Online, repository.observeTenantFreshness("t1").first())
            assertEquals(Freshness.Online, repository.observeLedgerFreshness("t1").first())
        }

    @Test
    fun offlineTheTenantScreenAgesByTheOlderOfItsTenantAndGroupData() =
        runTest {
            cacheTenant()
            connectivity.isOnline.value = false

            assertEquals(Freshness.Offline(40.minutes), repository.observeTenantFreshness("t1").first())
        }

    @Test
    fun offlineTheLedgerAgesByTheTenantDataAlone() =
        runTest {
            cacheTenant()
            connectivity.isOnline.value = false

            assertEquals(Freshness.Offline(5.minutes), repository.observeLedgerFreshness("t1").first())
        }

    @Test
    fun offlineAMonthOfTheCurrentYearAgesByTheTenantDataAlone() =
        runTest {
            cacheTenant()
            connectivity.isOnline.value = false

            assertEquals(
                Freshness.Offline(5.minutes),
                repository.observeMonthDetailFreshness("t1", YearMonth(2026, 3)).first(),
            )
        }

    @Test
    fun offlineAMonthOfAnotherYearAlsoAgesByThatYearsCalendar() =
        runTest {
            cacheTenant()
            connectivity.isOnline.value = false

            assertEquals(
                Freshness.Offline(50.minutes),
                repository.observeMonthDetailFreshness("t1", YearMonth(2025, 3)).first(),
            )
        }

    private suspend fun cacheTenant() {
        database.groupDao().upsert(listOf(groupEntity("g1")))
        database.tenantDao().upsert(listOf(tenantEntity("t1", "g1")))
        cachedAgo(CacheScope.tenant("t1"), 5.minutes)
        cachedAgo(CacheScope.group("g1"), 40.minutes)
        cachedAgo(CacheScope.calendarYear("t1", 2025), 50.minutes)
    }

    private suspend fun cachedAgo(
        scope: String,
        age: Duration,
    ) {
        database.cacheTimeDao().upsert(CacheTimeEntity(scope, TEN_O_CLOCK_UTC - age.inWholeMilliseconds))
    }

    private companion object {
        const val TEN_O_CLOCK_UTC = 1_794_736_800_000L
    }
}
