package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarStoreTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val store = ScreenResultStore(database)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aScreenResultWritesItsYearOfMonthsAndKeepsOtherCachedYears() =
        runTest {
            store.replaceCalendarYear("t1", 2025, listOf(calendarMonthEntity("t1", "2025-12")))

            replaceTenant(
                "t1",
                months = listOf(calendarMonthEntity("t1", "2026-01"), calendarMonthEntity("t1", "2026-02")),
            )

            assertEquals(listOf("2025-12"), months("t1", 2025))
            assertEquals(listOf("2026-01", "2026-02"), months("t1", 2026))
        }

    @Test
    fun aScreenResultReplacesTheMonthsOfItsYear() =
        runTest {
            replaceTenant(
                "t1",
                months = listOf(calendarMonthEntity("t1", "2026-01"), calendarMonthEntity("t1", "2026-02")),
            )

            replaceTenant(
                "t1",
                months = listOf(calendarMonthEntity("t1", "2026-02", state = CalendarMonthState.Owed)),
            )

            assertEquals(listOf("2026-02"), months("t1", 2026))
            assertEquals(
                CalendarMonthState.Owed,
                database
                    .calendarMonthDao()
                    .observeMonth("t1", "2026-02")
                    .first()
                    ?.state,
            )
        }

    @Test
    fun aScreenResultReplacesTheAllocationsOfThatTenantOnly() =
        runTest {
            replaceTenant("t1", allocations = listOf(allocationEntity("t1", "a1"), allocationEntity("t1", "a2")))
            replaceTenant("t2", allocations = listOf(allocationEntity("t2", "a1")))

            replaceTenant("t1", allocations = listOf(allocationEntity("t1", "a2")))

            assertEquals(listOf("a2"), allocationKeys("t1"))
            assertEquals(listOf("a1"), allocationKeys("t2"))
        }

    @Test
    fun aCalendarResultReplacesOnlyThatTenantsMonthsOfThatYear() =
        runTest {
            replaceTenant("t1", months = listOf(calendarMonthEntity("t1", "2026-01")))
            store.replaceCalendarYear("t2", 2025, listOf(calendarMonthEntity("t2", "2025-01")))
            store.replaceCalendarYear(
                "t1",
                2025,
                listOf(calendarMonthEntity("t1", "2025-01"), calendarMonthEntity("t1", "2025-02")),
            )

            store.replaceCalendarYear("t1", 2025, listOf(calendarMonthEntity("t1", "2025-03")))

            assertEquals(listOf("2025-03"), months("t1", 2025))
            assertEquals(listOf("2026-01"), months("t1", 2026))
            assertEquals(listOf("2025-01"), months("t2", 2025))
        }

    @Test
    fun anEmptyCalendarResultClearsThatYear() =
        runTest {
            store.replaceCalendarYear("t1", 2025, listOf(calendarMonthEntity("t1", "2025-01")))

            store.replaceCalendarYear("t1", 2025, emptyList())

            assertEquals(emptyList(), months("t1", 2025))
        }

    private suspend fun months(
        tenantId: String,
        year: Int,
    ): List<String> =
        database
            .calendarMonthDao()
            .observeYear(tenantId, year)
            .first()
            .map { it.month }

    private suspend fun allocationKeys(tenantId: String): List<String> =
        database
            .allocationDao()
            .observeByTenantId(tenantId)
            .first()
            .map { it.key }

    private suspend fun replaceTenant(
        tenantId: String,
        months: List<CalendarMonthEntity> = emptyList(),
        allocations: List<AllocationEntity> = emptyList(),
    ) {
        store.replaceTenant(
            tenantId = tenantId,
            groups = listOf(groupEntity("g1")),
            tenants = listOf(tenantEntity(tenantId, groupId = "g1")),
            tenantSummaries = listOf(tenantSummaryEntity(tenantId)),
            scope =
                TenantScopeEntities(
                    rentTerms = emptyList(),
                    payments = emptyList(),
                    debts = emptyList(),
                    ledgerRows = emptyList(),
                    accountNames = emptyList(),
                    memberships = emptyList(),
                    allocations = allocations,
                    calendarMonths = months,
                ),
        )
    }
}
