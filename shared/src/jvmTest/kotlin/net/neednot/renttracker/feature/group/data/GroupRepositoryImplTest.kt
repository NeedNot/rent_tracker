package net.neednot.renttracker.feature.group.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.groupEntity
import net.neednot.renttracker.core.data.groupSummaryEntity
import net.neednot.renttracker.core.data.tenantEntity
import net.neednot.renttracker.core.data.tenantSummaryEntity
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.GroupTenant
import net.neednot.renttracker.feature.group.domain.GroupTenantState
import net.neednot.renttracker.feature.group.domain.TenantBalance
import net.neednot.renttracker.model.groups.GroupsScreenResult
import net.neednot.renttracker.model.shared.Group
import net.neednot.renttracker.model.shared.GroupPaymentOrder
import net.neednot.renttracker.model.shared.GroupSummary
import net.neednot.renttracker.model.shared.Tenant
import net.neednot.renttracker.model.shared.TenantStatus
import net.neednot.renttracker.model.shared.TenantSummary
import net.neednot.renttracker.model.shared.TenantSummaryState
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.neednot.renttracker.core.data.TenantStatus as CachedTenantStatus

class GroupRepositoryImplTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()
    private val asOfClock = FakeAsOfClock(TEN_O_CLOCK)
    private val screenResultStore = ScreenResultStore(database)

    private val repository =
        GroupRepositoryImpl(
            backend = backend,
            asOfClock = asOfClock,
            database = database,
            screenResultStore = screenResultStore,
            telemetry = NoOpTelemetry(),
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun startsWithNothingWhileTheServerIsAnswering() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                assertEquals(Synced(null, SyncStatus.Syncing), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aServerResultIsCachedAndSplitIntoActiveAndArchivedTenants() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)

                backend.answerGroupScreen(
                    tenants = listOf(tenant("t1", "Apt 1A"), tenant("t2", "Apt 9Z", status = TenantStatus.Archived)),
                    tenantSummaries = listOf(summary("t1"), summary("t2", rentOwedCents = 240_000)),
                    groupSummary = groupSummary(archivedOwedCents = 240_000),
                )

                val expected =
                    GroupDetails(
                        groupId = GROUP_ID,
                        name = "1st Street",
                        totalOwedCents = 0,
                        lateTenantCount = 0,
                        archivedOwedCents = 240_000,
                        activeTenants = listOf(groupTenant("t1", "Apt 1A")),
                        archivedTenants = listOf(groupTenant("t2", "Apt 9Z", balance = TenantBalance.Owes(240_000))),
                    )
                assertEquals(Synced(expected, SyncStatus.Synced), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aTenantCarriesContactDueDateStateAndEverythingItOwes() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)

                backend.answerGroupScreen(
                    tenants = listOf(tenant("t1", "Apt 1A", contactName = "Marcus Lee")),
                    tenantSummaries =
                        listOf(
                            summary(
                                tenantId = "t1",
                                state = TenantSummaryState.Late,
                                rentOwedCents = 150_000,
                                lateFeesOwedCents = 5_000,
                                otherOwedCents = 10_000,
                                nextDueDate = "2026-04-01",
                            ),
                        ),
                )

                val expected =
                    GroupTenant(
                        tenantId = "t1",
                        name = "Apt 1A",
                        contactName = "Marcus Lee",
                        nextDueDate = LocalDate(2026, 4, 1),
                        state = GroupTenantState.Late,
                        balance = TenantBalance.Owes(165_000),
                    )
                assertEquals(listOf(expected), awaitItem().value?.activeTenants)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aTenantThatOwesNothingIsInCreditOrPaidUp() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)

                backend.answerGroupScreen(
                    tenants = listOf(tenant("t1", "Apt 1A"), tenant("t2", "Apt 1B")),
                    tenantSummaries = listOf(summary("t1", creditCents = 40_000), summary("t2")),
                )

                assertEquals(
                    listOf(TenantBalance.Credit(40_000), TenantBalance.PaidUp),
                    awaitItem().value?.activeTenants?.map { it.balance },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun activeTenantsAreListedLateFirstThenOwingThenPaidUpEachByName() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)

                backend.answerGroupScreen(
                    tenants =
                        listOf(
                            tenant("t1", "Apt 1A"),
                            tenant("t2", "Apt 2A"),
                            tenant("t3", "Apt 3A"),
                            tenant("t4", "apt 0a"),
                        ),
                    tenantSummaries =
                        listOf(
                            summary("t1"),
                            summary("t2", state = TenantSummaryState.Owes, rentOwedCents = 100),
                            summary("t3", state = TenantSummaryState.Late, rentOwedCents = 100),
                            summary("t4"),
                        ),
                )

                assertEquals(listOf("t3", "t2", "t4", "t1"), awaitItem().value?.activeTenants?.map { it.tenantId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aLaterResultReplacesTheGroupsTenantsButLeavesOtherGroupsAlone() =
        runTest {
            cacheAnotherGroupWithATenant()

            repository.observeGroup(GROUP_ID).test {
                skipItems(1)
                backend.answerGroupScreen(
                    tenants = listOf(tenant("t1", "Apt 1A"), tenant("t2", "Apt 1B")),
                    tenantSummaries = listOf(summary("t1"), summary("t2")),
                )
                skipItems(1)

                backend.answerGroupScreen(
                    tenants = listOf(tenant("t2", "Apt 1B")),
                    tenantSummaries = listOf(summary("t2")),
                )

                assertEquals(listOf("t2"), awaitItem().value?.activeTenants?.map { it.tenantId })
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("t2"), cachedActiveTenantIds(GROUP_ID))
            assertEquals(listOf("other-tenant"), cachedActiveTenantIds(OTHER_GROUP_ID))
        }

    @Test
    fun aGroupThatNoLongerExistsIsReportedAsNotFound() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)

                backend.answerSubscription(
                    GROUP_SCREEN,
                    Err(
                        BackendError.Convex(
                            code = "NotFound",
                            message = "This group no longer exists.",
                            details = null,
                        ),
                    ),
                )

                assertEquals(Synced(null, SyncStatus.Failed(GroupError.NotFound)), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aServerErrorKeepsTheCachedGroupAndReportsTheFailure() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)
                backend.answerGroupScreen(
                    tenants = listOf(tenant("t1", "Apt 1A")),
                    tenantSummaries = listOf(summary("t1")),
                )
                skipItems(1)

                backend.answerSubscription(GROUP_SCREEN, Err(BackendError.Server("boom")))

                val failed = awaitItem()
                assertEquals(SyncStatus.Failed(GroupError.Unexpected("boom")), failed.status)
                assertEquals(listOf("t1"), failed.value?.activeTenants?.map { it.tenantId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun subscribesToTheGroupWithTheCurrentHour() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)
                cancelAndIgnoreRemainingEvents()
            }

            val expectedArgs =
                buildJsonObject {
                    put("asOf", TEN_O_CLOCK)
                    put("groupId", GROUP_ID)
                }
            assertEquals(listOf(GROUP_SCREEN to expectedArgs), backend.subscriptions)
            assertEquals(0, backend.openSubscriptionCount)
        }

    private fun FakeBackend.answerGroupScreen(
        tenants: List<Tenant>,
        tenantSummaries: List<TenantSummary>,
        groupSummary: GroupSummary = groupSummary(),
    ) {
        val result =
            GroupsScreenResult(
                groupSummaries = listOf(groupSummary),
                groups = listOf(group()),
                tenantSummaries = tenantSummaries,
                tenants = tenants,
            )
        answerSubscription(GROUP_SCREEN, Ok(Json.encodeToJsonElement(GroupsScreenResult.serializer(), result)))
    }

    private suspend fun cacheAnotherGroupWithATenant() {
        screenResultStore.replaceGroup(
            groupId = OTHER_GROUP_ID,
            groups = listOf(groupEntity(OTHER_GROUP_ID)),
            tenants = listOf(tenantEntity(id = "other-tenant", groupId = OTHER_GROUP_ID)),
            tenantSummaries = listOf(tenantSummaryEntity("other-tenant")),
            groupSummaries = listOf(groupSummaryEntity(OTHER_GROUP_ID)),
        )
    }

    private suspend fun cachedActiveTenantIds(groupId: String): List<String> =
        database
            .tenantDao()
            .observeByGroupAndStatus(groupId, CachedTenantStatus.Active)
            .first()
            .map { it.id }

    private fun group() =
        Group(
            creationTime = 1.25,
            id = GROUP_ID,
            graceDays = 5,
            name = "1st Street",
            ownerId = "owner",
            paymentOrder = GroupPaymentOrder.RentFirst,
            timeZone = "America/Chicago",
            version = 1,
        )

    private fun groupSummary(archivedOwedCents: Long = 0) =
        GroupSummary(
            activeTenantCount = 0,
            archivedOwedCents = archivedOwedCents,
            archivedTenantCount = 0,
            groupId = GROUP_ID,
            lateTenantCount = 0,
            totalOwedCents = 0,
        )

    private fun tenant(
        id: String,
        name: String,
        status: TenantStatus = TenantStatus.Active,
        contactName: String? = null,
    ) = Tenant(
        creationTime = 1.25,
        id = id,
        contactName = contactName,
        groupId = GROUP_ID,
        moveIn = "2026-01-01",
        name = name,
        status = status,
        version = 1,
    )

    private fun summary(
        tenantId: String,
        state: TenantSummaryState = TenantSummaryState.PaidUp,
        rentOwedCents: Long = 0,
        lateFeesOwedCents: Long = 0,
        otherOwedCents: Long = 0,
        creditCents: Long = 0,
        nextDueDate: String? = null,
    ) = TenantSummary(
        creditCents = creditCents,
        lateFeesOwedCents = lateFeesOwedCents,
        nextDueDate = nextDueDate,
        otherOwedCents = otherOwedCents,
        rentOwedCents = rentOwedCents,
        state = state,
        tenantId = tenantId,
    )

    private fun groupTenant(
        tenantId: String,
        name: String,
        balance: TenantBalance = TenantBalance.PaidUp,
    ) = GroupTenant(
        tenantId = tenantId,
        name = name,
        contactName = null,
        nextDueDate = null,
        state = GroupTenantState.PaidUp,
        balance = balance,
    )

    private companion object {
        const val GROUP_SCREEN = "groups:screen"
        const val GROUP_ID = "g1"
        const val OTHER_GROUP_ID = "g2"
        const val TEN_O_CLOCK = 1_794_736_800_000L
    }
}
