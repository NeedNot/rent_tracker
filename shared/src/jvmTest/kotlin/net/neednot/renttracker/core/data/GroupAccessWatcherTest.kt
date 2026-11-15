package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.backend.BackendError
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupAccessWatcherTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val store = ScreenResultStore(database)
    private val watcher = GroupAccessWatcher(database, store)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun permissionDeniedOnAGroupSubscriptionMeansTheCallerWasRemoved() =
        runTest {
            cacheGroup("g1", "1st Street")

            watcher.losses.test {
                watcher.watchGroup("g1", failingWith("PermissionDenied")).toList()

                assertEquals(LostGroupAccess("g1", "1st Street", AccessLossReason.Removed), awaitItem())
            }
            assertEquals(null, database.groupDao().observeById("g1").first())
        }

    @Test
    fun notFoundOnAGroupSubscriptionMeansTheGroupIsGone() =
        runTest {
            cacheGroup("g1", "1st Street")

            watcher.losses.test {
                watcher.watchGroup("g1", failingWith("NotFound")).toList()

                assertEquals(LostGroupAccess("g1", "1st Street", AccessLossReason.GroupGone), awaitItem())
            }
        }

    @Test
    fun otherResultsLeaveTheGroupAlone() =
        runTest {
            cacheGroup("g1", "1st Street")

            watcher.losses.test {
                watcher.watchGroup("g1", flowOf(Ok(Unit), Err(BackendError.Server("boom")))).toList()
                watcher.watchGroup("g1", failingWith("ValidationFailed")).toList()

                expectNoEvents()
            }
            assertEquals(
                "1st Street",
                database
                    .groupDao()
                    .observeById("g1")
                    .first()
                    ?.name,
            )
        }

    @Test
    fun onlyPermissionDeniedOnATenantSubscriptionLosesTheTenantsGroup() =
        runTest {
            cacheGroup("g1", "1st Street", tenantId = "t1")

            watcher.losses.test {
                watcher.watchTenant("t1", failingWith("NotFound")).toList()
                expectNoEvents()

                watcher.watchTenant("t1", failingWith("PermissionDenied")).toList()

                assertEquals(LostGroupAccess("g1", "1st Street", AccessLossReason.Removed), awaitItem())
            }
            assertEquals(null, database.tenantDao().observeById("t1").first())
        }

    @Test
    fun aGroupIsReportedOnceEvenWhenSeveralScreensFail() =
        runTest {
            cacheGroup("g1", "1st Street")

            watcher.losses.test {
                watcher.report("g1", AccessLossReason.Removed)
                watcher.report("g1", AccessLossReason.Removed)

                assertEquals(LostGroupAccess("g1", "1st Street", AccessLossReason.Removed), awaitItem())
                expectNoEvents()
            }
        }

    private suspend fun cacheGroup(
        groupId: String,
        name: String,
        tenantId: String = "t1",
    ) {
        store.replaceGroup(
            groupId = groupId,
            scope =
                GroupScopeEntities(
                    groups = listOf(groupEntity(groupId, name = name)),
                    tenants = listOf(tenantEntity(tenantId, groupId = groupId)),
                    tenantSummaries = listOf(tenantSummaryEntity(tenantId)),
                    groupSummaries = listOf(groupSummaryEntity(groupId)),
                    callerMemberships = listOf(membershipEntity("m1", groupId)),
                ),
        )
    }

    private fun failingWith(code: String) =
        flowOf<Result<Unit, BackendError>>(Err(BackendError.Convex(code = code, message = code, details = null)))
}
