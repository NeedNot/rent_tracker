package net.neednot.renttracker.feature.tenants.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.AccountNameEntity
import net.neednot.renttracker.core.data.AllocationChargeKind
import net.neednot.renttracker.core.data.AllocationEntity
import net.neednot.renttracker.core.data.CalendarMonthEntity
import net.neednot.renttracker.core.data.CalendarMonthState
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.TenantScopeEntities
import net.neednot.renttracker.core.data.allocationEntity
import net.neednot.renttracker.core.data.calendarMonthEntity
import net.neednot.renttracker.core.data.debtEntity
import net.neednot.renttracker.core.data.groupEntity
import net.neednot.renttracker.core.data.membershipEntity
import net.neednot.renttracker.core.data.paymentEntity
import net.neednot.renttracker.core.data.tenantEntity
import net.neednot.renttracker.core.data.tenantSummaryEntity
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.tenants.domain.AllocationTarget
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.OtherAllocation
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.model.tenants.CalendarMonth
import net.neednot.renttracker.model.tenants.TenantsCalendarResult
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.neednot.renttracker.model.tenants.CalendarMonthState as ConvexCalendarMonthState

class TenantCalendarRepositoryTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()
    private val store = ScreenResultStore(database)

    private val repository =
        TenantCalendarRepositoryImpl(
            backend = backend,
            asOfClock = FakeAsOfClock(TEN_O_CLOCK_UTC),
            database = database,
            screenResultStore = store,
            telemetry = NoOpTelemetry(),
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun theCurrentYearComesFromTheCacheWithoutAskingForTheCalendar() =
        runTest {
            seedTenant(months = listOf(calendarMonthEntity("t1", "2026-03")))

            repository.observeCalendar("t1", yearOffset = 0).test {
                val calendar = awaitItem()
                assertEquals(2026, calendar.value.year)
                assertEquals(listOf(YearMonth(2026, 3)), calendar.value.months.map { it.month })
                assertEquals(SyncStatus.Synced, calendar.status)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(backend.subscriptions.none { it.first == TENANT_CALENDAR })
        }

    @Test
    fun anotherYearSubscribesToTheCalendarAndStoresTheResult() =
        runTest {
            seedTenant(months = listOf(calendarMonthEntity("t1", "2026-03")))
            backend.answerCalendar(listOf(convexMonth("2025-12")))

            val calendar =
                repository
                    .observeCalendar("t1", yearOffset = -1)
                    .first { it.status == SyncStatus.Synced && it.value.months.isNotEmpty() }
            assertEquals(2025, calendar.value.year)
            assertEquals(listOf(YearMonth(2025, 12)), calendar.value.months.map { it.month })
            val args = backend.subscriptions.single { it.first == TENANT_CALENDAR }.second
            assertEquals(2025, args.getValue("year").jsonPrimitive.int)
            assertEquals("t1", args.getValue("tenantId").jsonPrimitive.content)
            assertEquals(
                1,
                database
                    .calendarMonthDao()
                    .observeYear("t1", 2025)
                    .first()
                    .size,
            )
            assertEquals(
                1,
                database
                    .calendarMonthDao()
                    .observeYear("t1", 2026)
                    .first()
                    .size,
            )
        }

    @Test
    fun anotherYearShowsItsCachedMonthsWhileOffline() =
        runTest {
            seedTenant(months = listOf(calendarMonthEntity("t1", "2025-11")))
            store.replaceCalendarYear("t1", 2025, listOf(calendarMonthEntity("t1", "2025-11")))
            backend.answerSubscription(TENANT_CALENDAR, Err(BackendError.Server("Offline")))

            val offline = repository.observeCalendar("t1", yearOffset = -1).first { it.status is SyncStatus.Failed }
            assertEquals(listOf(YearMonth(2025, 11)), offline.value.months.map { it.month })
        }

    @Test
    fun theMonthDetailJoinsPaymentsOtherAllocationsAndTheLateFee() =
        runTest {
            seedTenant(
                months =
                    listOf(
                        calendarMonthEntity("t1", "2026-03", state = CalendarMonthState.Partial, owedCents = 50_000),
                    ),
                payments =
                    listOf(
                        paymentEntity("p1", "t1", amountCents = 150_000).copy(method = PaymentMethod.Zelle),
                    ),
                debts =
                    listOf(
                        debtEntity("d1", "t1", DebtKind.LateFee).copy(forMonth = "2026-03", createdBy = "sam"),
                    ),
                allocations =
                    listOf(
                        allocationEntity("t1", "a1", paymentId = "p1", month = "2026-03", amountCents = 100_000),
                        allocationEntity("t1", "a2", paymentId = "p1", month = "2026-02", amountCents = 50_000),
                    ),
            )

            val detail = repository.observeMonthDetail("t1", YearMonth(2026, 3)).first()

            val payment = assertNotNull(detail).payments.single()
            assertEquals(LocalDate(2026, 3, 1), payment.dateReceived)
            assertEquals(PaymentMethod.Zelle, payment.method)
            assertEquals(150_000, payment.paymentCents)
            assertEquals(100_000, payment.allocatedToMonthCents)
            assertEquals(
                listOf(OtherAllocation(AllocationTarget.Rent(YearMonth(2026, 2)), 50_000)),
                payment.otherAllocations,
            )
            assertEquals(Person.Named("Sam Rivera"), detail.lateFee?.addedBy)
            assertTrue(detail.canAddCharge)
        }

    @Test
    fun otherAllocationsCanBeLateFees() =
        runTest {
            seedTenant(
                months = listOf(calendarMonthEntity("t1", "2026-03")),
                payments = listOf(paymentEntity("p1", "t1")),
                debts = listOf(debtEntity("d1", "t1", DebtKind.LateFee).copy(forMonth = "2026-02")),
                allocations =
                    listOf(
                        allocationEntity("t1", "a1", paymentId = "p1", month = "2026-03"),
                        AllocationEntity("t1", "a2", "p1", AllocationChargeKind.Debt, null, "d1", 5_000),
                    ),
            )

            val detail = repository.observeMonthDetail("t1", YearMonth(2026, 3)).first()

            assertEquals(
                listOf(OtherAllocation(AllocationTarget.Charge(ChargeKind.LateFee(YearMonth(2026, 2))), 5_000)),
                assertNotNull(detail).payments.single().otherAllocations,
            )
        }

    @Test
    fun bouncedAndCancelledPaymentsAreNotListedAsCoveringTheMonth() =
        runTest {
            seedTenant(
                months = listOf(calendarMonthEntity("t1", "2026-03")),
                payments =
                    listOf(
                        paymentEntity("p1", "t1").copy(bouncedAt = 5),
                        paymentEntity("p2", "t1").copy(deletedAt = 6),
                    ),
                allocations =
                    listOf(
                        allocationEntity("t1", "a1", paymentId = "p1"),
                        allocationEntity("t1", "a2", paymentId = "p2"),
                    ),
            )

            assertEquals(
                emptyList(),
                assertNotNull(repository.observeMonthDetail("t1", YearMonth(2026, 3)).first()).payments,
            )
        }

    @Test
    fun aCancelledLateFeeIsNotShownOnTheMonth() =
        runTest {
            seedTenant(
                months = listOf(calendarMonthEntity("t1", "2026-03")),
                debts = listOf(debtEntity("d1", "t1", DebtKind.LateFee).copy(forMonth = "2026-03", deletedAt = 9)),
            )

            assertNull(assertNotNull(repository.observeMonthDetail("t1", YearMonth(2026, 3)).first()).lateFee)
        }

    @Test
    fun aMonthThatIsNotCachedHasNoDetail() =
        runTest {
            seedTenant(months = emptyList())

            assertNull(repository.observeMonthDetail("t1", YearMonth(2026, 3)).first())
        }

    private suspend fun seedTenant(
        months: List<CalendarMonthEntity>,
        payments: List<net.neednot.renttracker.core.data.PaymentEntity> = emptyList(),
        debts: List<net.neednot.renttracker.core.data.DebtEntity> = emptyList(),
        allocations: List<AllocationEntity> = emptyList(),
    ) {
        store.replaceTenant(
            tenantId = "t1",
            groups = listOf(groupEntity("g1")),
            tenants = listOf(tenantEntity("t1", groupId = "g1")),
            tenantSummaries = listOf(tenantSummaryEntity("t1")),
            scope =
                TenantScopeEntities(
                    rentTerms = emptyList(),
                    payments = payments,
                    debts = debts,
                    ledgerRows = emptyList(),
                    accountNames = listOf(AccountNameEntity("sam", "Sam Rivera")),
                    memberships = listOf(membershipEntity("m1", "g1")),
                    allocations = allocations,
                    calendarMonths = months,
                ),
        )
    }

    private fun FakeBackend.answerCalendar(months: List<CalendarMonth>) {
        answerSubscription(
            TENANT_CALENDAR,
            Ok(Json.encodeToJsonElement(TenantsCalendarResult.serializer(), TenantsCalendarResult(months))),
        )
    }

    private fun convexMonth(month: String) =
        CalendarMonth(
            coveredCents = 150_000,
            hasUnpaidLateFee = false,
            month = month,
            owedCents = 0,
            rentCents = 150_000,
            state = ConvexCalendarMonthState.Paid,
            tenantId = "t1",
            wasLate = false,
        )

    private companion object {
        const val TENANT_CALENDAR = "tenants:calendar"
        const val TEN_O_CLOCK_UTC = 1_794_736_800_000L
    }
}
