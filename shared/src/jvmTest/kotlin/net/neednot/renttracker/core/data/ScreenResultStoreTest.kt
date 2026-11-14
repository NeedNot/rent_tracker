package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenResultStoreTest {
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
    fun aHomeResultIsCachedWhole() =
        runTest {
            val groups = listOf(groupEntity("g1", creationTime = 1), groupEntity("g2", creationTime = 2))
            val summaries = listOf(groupSummaryEntity("g1", totalOwedCents = 390_000), groupSummaryEntity("g2"))

            store.replaceHome(groups, summaries)

            assertEquals(groups, database.groupDao().observeAll().first())
            assertEquals(summaries, database.groupSummaryDao().observeAll().first())
        }

    @Test
    fun aHomeResultRemovesGroupsAndSummariesMissingFromIt() =
        runTest {
            store.replaceHome(
                groups = listOf(groupEntity("g1"), groupEntity("g2")),
                groupSummaries = listOf(groupSummaryEntity("g1"), groupSummaryEntity("g2")),
            )

            store.replaceHome(groups = listOf(groupEntity("g2")), groupSummaries = listOf(groupSummaryEntity("g2")))

            assertEquals(
                listOf("g2"),
                database
                    .groupDao()
                    .observeAll()
                    .first()
                    .map { it.id },
            )
            assertEquals(
                listOf("g2"),
                database
                    .groupSummaryDao()
                    .observeAll()
                    .first()
                    .map { it.groupId },
            )
        }

    @Test
    fun aHomeResultUpdatesAGroupItAlreadyHas() =
        runTest {
            store.replaceHome(listOf(groupEntity("g1", name = "1st Street")), listOf(groupSummaryEntity("g1")))

            store.replaceHome(
                groups = listOf(groupEntity("g1", name = "First Street")),
                groupSummaries = listOf(groupSummaryEntity("g1", totalOwedCents = 120_000)),
            )

            assertEquals(
                listOf("First Street"),
                database
                    .groupDao()
                    .observeAll()
                    .first()
                    .map { it.name },
            )
            assertEquals(
                120_000,
                database
                    .groupSummaryDao()
                    .observeByGroupId("g1")
                    .first()
                    ?.totalOwedCents,
            )
        }

    @Test
    fun anEmptyHomeResultClearsEveryGroup() =
        runTest {
            store.replaceHome(listOf(groupEntity("g1")), listOf(groupSummaryEntity("g1")))

            store.replaceHome(groups = emptyList(), groupSummaries = emptyList())

            assertEquals(emptyList(), database.groupDao().observeAll().first())
            assertEquals(emptyList(), database.groupSummaryDao().observeAll().first())
        }

    @Test
    fun aGroupResultRemovesThatGroupsTenantsAndSummariesMissingFromIt() =
        runTest {
            replaceGroup("g1", tenantIds = listOf("t1", "t2"))

            replaceGroup("g1", tenantIds = listOf("t2"))

            assertEquals(listOf("t2"), activeTenantIds("g1"))
            assertEquals(null, database.tenantSummaryDao().observeByTenantId("t1").first())
            assertEquals(tenantSummaryEntity("t2"), database.tenantSummaryDao().observeByTenantId("t2").first())
        }

    @Test
    fun aGroupResultLeavesOtherGroupsUntouched() =
        runTest {
            replaceGroup("g1", tenantIds = listOf("t1"))
            replaceGroup("g2", tenantIds = listOf("t2"))

            replaceGroup("g1", tenantIds = emptyList())

            assertEquals(emptyList(), activeTenantIds("g1"))
            assertEquals(listOf("t2"), activeTenantIds("g2"))
            assertEquals(tenantSummaryEntity("t2"), database.tenantSummaryDao().observeByTenantId("t2").first())
            assertEquals(groupEntity("g2"), database.groupDao().observeById("g2").first())
        }

    @Test
    fun aGroupResultCachesItsGroupAndGroupSummary() =
        runTest {
            replaceGroup("g1", tenantIds = listOf("t1"))

            assertEquals(groupEntity("g1"), database.groupDao().observeById("g1").first())
            assertEquals(groupSummaryEntity("g1"), database.groupSummaryDao().observeByGroupId("g1").first())
        }

    @Test
    fun aTenantResultRemovesThatTenantsRentTermsMissingFromIt() =
        runTest {
            replaceTenant("t1", rentTermIds = listOf("r1", "r2"))

            replaceTenant("t1", rentTermIds = listOf("r2"))

            assertEquals(listOf("r2"), rentTermIds("t1"))
        }

    @Test
    fun aTenantResultLeavesOtherTenantsUntouched() =
        runTest {
            replaceTenant("t1", rentTermIds = listOf("r1"))
            replaceTenant("t2", rentTermIds = listOf("r2"))

            replaceTenant("t1", rentTermIds = emptyList())

            assertEquals(emptyList(), rentTermIds("t1"))
            assertEquals(listOf("r2"), rentTermIds("t2"))
            assertEquals(tenantEntity("t2", groupId = "g1"), database.tenantDao().observeById("t2").first())
        }

    @Test
    fun aTenantResultCachesItsTenantSummaryAndGroup() =
        runTest {
            replaceTenant("t1", rentTermIds = listOf("r1"))

            assertEquals(tenantEntity("t1", groupId = "g1"), database.tenantDao().observeById("t1").first())
            assertEquals(tenantSummaryEntity("t1"), database.tenantSummaryDao().observeByTenantId("t1").first())
            assertEquals(groupEntity("g1"), database.groupDao().observeById("g1").first())
        }

    private suspend fun replaceGroup(
        groupId: String,
        tenantIds: List<String>,
    ) {
        store.replaceGroup(
            groupId = groupId,
            groups = listOf(groupEntity(groupId)),
            tenants = tenantIds.map { tenantEntity(it, groupId = groupId) },
            tenantSummaries = tenantIds.map { tenantSummaryEntity(it) },
            groupSummaries = listOf(groupSummaryEntity(groupId)),
        )
    }

    private suspend fun replaceTenant(
        tenantId: String,
        rentTermIds: List<String>,
    ) {
        store.replaceTenant(
            tenantId = tenantId,
            groups = listOf(groupEntity("g1")),
            tenants = listOf(tenantEntity(tenantId, groupId = "g1")),
            tenantSummaries = listOf(tenantSummaryEntity(tenantId)),
            scope =
                TenantScopeEntities(
                    rentTerms = rentTermIds.map { rentTermEntity(it, tenantId = tenantId) },
                    payments = emptyList(),
                    debts = emptyList(),
                    ledgerRows = emptyList(),
                    accountNames = emptyList(),
                    memberships = emptyList(),
                    allocations = emptyList(),
                    calendarMonths = emptyList(),
                ),
        )
    }

    private suspend fun activeTenantIds(groupId: String): List<String> =
        database
            .tenantDao()
            .observeByGroupAndStatus(groupId, TenantStatus.Active)
            .first()
            .map { it.id }

    private suspend fun rentTermIds(tenantId: String): List<String> =
        database
            .rentTermDao()
            .observeByTenantId(tenantId)
            .first()
            .map { it.id }
}
