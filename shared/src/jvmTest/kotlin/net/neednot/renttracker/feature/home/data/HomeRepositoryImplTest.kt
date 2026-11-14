package net.neednot.renttracker.feature.home.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.home.domain.GroupOverview
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.model.home.HomeScreenResult
import net.neednot.renttracker.model.shared.Group
import net.neednot.renttracker.model.shared.GroupPaymentOrder
import net.neednot.renttracker.model.shared.GroupSummary
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRepositoryImplTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()
    private val asOfClock = FakeAsOfClock(TEN_O_CLOCK)

    private val repository =
        HomeRepositoryImpl(
            backend = backend,
            asOfClock = asOfClock,
            database = database,
            screenResultStore = ScreenResultStore(database),
            phoneTimeZone = { TimeZone.of("America/Chicago") },
            telemetry = NoOpTelemetry(),
        )

    private val firstStreet = group(id = "g1", name = "1st Street", creationTime = 1)
    private val mapleCourt = group(id = "g2", name = "Maple Court", creationTime = 2)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun startsWithTheCacheWhileTheServerIsAnswering() =
        runTest {
            repository.observeGroups().test {
                assertEquals(Synced(emptyList(), SyncStatus.Syncing), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aServerResultIsCachedAndShownOldestGroupFirst() =
        runTest {
            repository.observeGroups().test {
                skipItems(1)

                backend.answerHomeScreen(
                    groups = listOf(mapleCourt, firstStreet),
                    summaries = listOf(summary("g2"), summary("g1", totalOwedCents = 390_000, lateTenantCount = 2)),
                )

                val expected =
                    listOf(
                        overview("g1", "1st Street", totalOwedCents = 390_000, lateTenantCount = 2),
                        overview("g2", "Maple Court"),
                    )
                assertEquals(Synced(expected, SyncStatus.Synced), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("g1", "g2"), cachedGroupIds())
        }

    @Test
    fun aLaterResultReplacesTheCacheSoRemovedGroupsDisappear() =
        runTest {
            repository.observeGroups().test {
                skipItems(1)
                backend.answerHomeScreen(listOf(firstStreet, mapleCourt), listOf(summary("g1"), summary("g2")))
                skipItems(1)

                backend.answerHomeScreen(listOf(mapleCourt), listOf(summary("g2")))

                assertEquals(Synced(listOf(overview("g2", "Maple Court")), SyncStatus.Synced), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("g2"), cachedGroupIds())
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
    fun aServerErrorKeepsTheCachedGroupsAndReportsTheFailure() =
        runTest {
            repository.observeGroups().test {
                skipItems(1)
                backend.answerHomeScreen(listOf(firstStreet), listOf(summary("g1")))
                skipItems(1)

                backend.answerSubscription(HOME_SCREEN, Err(BackendError.Server("boom")))

                assertEquals(
                    Synced(listOf(overview("g1", "1st Street")), SyncStatus.Failed(HomeError.Unexpected("boom"))),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun subscribesWithTheCurrentHour() =
        runTest {
            repository.observeGroups().test {
                skipItems(1)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(listOf(HOME_SCREEN to asOfArgs(TEN_O_CLOCK)), backend.subscriptions)
        }

    @Test
    fun resubscribesWhenTheHourRollsOver() =
        runTest {
            repository.observeGroups().test {
                skipItems(1)

                asOfClock.asOf.value = ELEVEN_O_CLOCK
                testScheduler.runCurrent()

                assertEquals(
                    listOf(HOME_SCREEN to asOfArgs(TEN_O_CLOCK), HOME_SCREEN to asOfArgs(ELEVEN_O_CLOCK)),
                    backend.subscriptions,
                )
                assertEquals(1, backend.openSubscriptionCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun theSubscriptionClosesWhenNobodyObserves() =
        runTest {
            repository.observeGroups().test {
                skipItems(1)
                assertEquals(1, backend.openSubscriptionCount)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(0, backend.openSubscriptionCount)
        }

    @Test
    fun creatingAGroupSendsTheNameAndThePhonesTimeZone() =
        runTest {
            backend.mutationAnswers[CREATE_GROUP] = Ok(JsonPrimitive("g9"))

            val result = repository.createGroup("1st Street")

            assertEquals(Ok("g9"), result)
            val expectedArgs =
                buildJsonObject {
                    put("name", "1st Street")
                    put("timeZone", "America/Chicago")
                }
            assertEquals(listOf(CREATE_GROUP to expectedArgs), backend.mutations)
        }

    @Test
    fun aRejectedGroupNameReturnsTheServersMessage() =
        runTest {
            backend.mutationAnswers[CREATE_GROUP] =
                Err(BackendError.Convex(code = "ValidationFailed", message = "Group name is too long.", details = null))

            assertEquals(Err(HomeError.ValidationFailed("Group name is too long.")), repository.createGroup("x"))
        }

    @Test
    fun creatingAGroupWithoutAConnectionReportsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(HomeError.Offline), repository.createGroup("1st Street"))
        }

    private fun FakeBackend.answerHomeScreen(
        groups: List<Group>,
        summaries: List<GroupSummary>,
    ) {
        val result = HomeScreenResult(groupSummaries = summaries, groups = groups)
        answerSubscription(HOME_SCREEN, Ok(Json.encodeToJsonElement(HomeScreenResult.serializer(), result)))
    }

    private suspend fun cachedGroupIds(): List<String> =
        database
            .groupDao()
            .observeAll()
            .first()
            .map { it.id }

    private fun asOfArgs(asOf: Long) = buildJsonObject { put("asOf", asOf) }

    private fun group(
        id: String,
        name: String,
        creationTime: Long,
    ) = Group(
        creationTime = creationTime,
        id = id,
        graceDays = 5,
        name = name,
        ownerId = "owner",
        paymentOrder = GroupPaymentOrder.RentFirst,
        timeZone = "America/Chicago",
        version = 1,
    )

    private fun summary(
        groupId: String,
        totalOwedCents: Long = 0,
        lateTenantCount: Long = 0,
    ) = GroupSummary(
        activeTenantCount = 3,
        archivedOwedCents = 0,
        archivedTenantCount = 0,
        groupId = groupId,
        lateTenantCount = lateTenantCount,
        totalOwedCents = totalOwedCents,
    )

    private fun overview(
        groupId: String,
        name: String,
        totalOwedCents: Long = 0,
        lateTenantCount: Int = 0,
    ) = GroupOverview(
        groupId = groupId,
        name = name,
        totalOwedCents = totalOwedCents,
        activeTenantCount = 3,
        lateTenantCount = lateTenantCount,
        archivedOwedCents = 0,
    )

    private companion object {
        const val HOME_SCREEN = "home:screen"
        const val CREATE_GROUP = "groups:create"
        const val TEN_O_CLOCK = 1_794_736_800_000L
        const val ELEVEN_O_CLOCK = 1_794_740_400_000L
    }
}
