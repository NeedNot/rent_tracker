package net.neednot.renttracker.feature.tenants.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.data.rentTermEntity
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.tenants.domain.BalanceTotal
import net.neednot.renttracker.feature.tenants.domain.FirstRentTerm
import net.neednot.renttracker.feature.tenants.domain.RentTermDraft
import net.neednot.renttracker.feature.tenants.domain.RentTermPreview
import net.neednot.renttracker.feature.tenants.domain.TenantBalance
import net.neednot.renttracker.feature.tenants.domain.TenantDetails
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantGroup
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.model.rentterms.RentTermsPreviewAfter
import net.neednot.renttracker.model.rentterms.RentTermsPreviewAfterState
import net.neednot.renttracker.model.rentterms.RentTermsPreviewBefore
import net.neednot.renttracker.model.rentterms.RentTermsPreviewBeforeState
import net.neednot.renttracker.model.rentterms.RentTermsPreviewResult
import net.neednot.renttracker.model.shared.Group
import net.neednot.renttracker.model.shared.GroupPaymentOrder
import net.neednot.renttracker.model.shared.Tenant
import net.neednot.renttracker.model.shared.TenantStatus
import net.neednot.renttracker.model.shared.TenantSummary
import net.neednot.renttracker.model.shared.TenantSummaryPartialMoveOutPeriod
import net.neednot.renttracker.model.shared.TenantSummaryState
import net.neednot.renttracker.model.tenants.RentTerm
import net.neednot.renttracker.model.tenants.TenantsScreenResult
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TenantRepositoryImplTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()
    private val asOfClock = FakeAsOfClock(TEN_O_CLOCK_UTC)

    private val repository =
        TenantRepositoryImpl(
            backend = backend,
            asOfClock = asOfClock,
            database = database,
            screenResultStore = ScreenResultStore(database),
            telemetry = NoOpTelemetry(),
        )

    private val details =
        TenantDetails(
            name = "Apt 3B",
            contactName = "Dana Whitfield",
            contactPhone = null,
            contactEmail = null,
            notes = null,
            depositCents = 150_000,
            moveIn = LocalDate(2026, 1, 17),
            moveOut = null,
            graceDaysOverride = 3,
            lateFeeFlatOverrideCents = null,
        )

    private val draft = RentTermDraft(startMonth = YearMonth(2026, 6), amountCents = 160_000, dueDay = 31)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun startsWithNothingCachedWhileTheServerIsAnswering() =
        runTest {
            repository.observeTenant("t1").test {
                assertEquals(Synced(null, SyncStatus.Syncing), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aServerResultIsCachedAndShownAsTheTenantOverview() =
        runTest {
            repository.observeTenant("t1").test {
                skipItems(1)

                backend.answerTenantScreen(
                    rentTerms = listOf(rentTerm("r2", "2025-01", amountCents = 150_000), rentTerm("r1", "2024-01")),
                    summary =
                        summary().copy(
                            state = TenantSummaryState.Late,
                            rentOwedCents = 50_000,
                            lateFeesOwedCents = 5_000,
                            otherOwedCents = 1_000,
                            nextDueDate = "2026-12-01",
                            partialMoveOutPeriod =
                                TenantSummaryPartialMoveOutPeriod(
                                    end = "2026-03-31",
                                    start = "2026-03-01",
                                ),
                        ),
                )

                val overview = awaitSynced()
                assertEquals("Apt 3B", overview.tenant.details.name)
                assertEquals(LocalDate(2024, 1, 1), overview.tenant.details.moveIn)
                assertEquals(7, overview.tenant.version)
                assertEquals(false, overview.tenant.isArchived)
                assertEquals(TenantGroup(name = "1st Street", graceDays = 5, lateFeeFlatCents = 5_000), overview.group)
                assertEquals(
                    TenantBalance(
                        state = TenantBalanceState.Late,
                        total = BalanceTotal(owedCents = 56_000, creditCents = 0),
                        nextDueDate = LocalDate(2026, 12, 1),
                        partialMoveOutPeriod = LocalDate(2026, 3, 1)..LocalDate(2026, 3, 31),
                    ),
                    overview.balance,
                )
                assertEquals(listOf("r1", "r2"), overview.activeRentTerms.map { it.rentTermId })
                assertEquals(YearMonth(2025, 1), overview.activeRentTerms.last().startMonth)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aLaterResultReplacesTheTenantsRentTermsAndLeavesOtherTenantsAlone() =
        runTest {
            database.rentTermDao().upsert(listOf(rentTermEntity("stale", "t1"), rentTermEntity("other", "t2")))

            repository.observeTenant("t1").test {
                skipItems(1)
                backend.answerTenantScreen(rentTerms = listOf(rentTerm("r1", "2024-01"), rentTerm("r2", "2025-01")))
                awaitSynced()

                backend.answerTenantScreen(rentTerms = listOf(rentTerm("r1", "2024-01")))

                assertEquals(listOf("r1"), awaitSynced().activeRentTerms.map { it.rentTermId })
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("r1"), cachedRentTermIds("t1"))
            assertEquals(listOf("other"), cachedRentTermIds("t2"))
        }

    @Test
    fun deletedRentTermsStayCachedButAreNotActive() =
        runTest {
            repository.observeTenant("t1").test {
                skipItems(1)

                backend.answerTenantScreen(
                    rentTerms = listOf(rentTerm("r1", "2024-01"), rentTerm("r2", "2025-01").copy(deletedAt = 99)),
                )

                assertEquals(listOf("r1"), awaitSynced().activeRentTerms.map { it.rentTermId })
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("r1", "r2"), cachedRentTermIds("t1"))
        }

    @Test
    fun todayIsTheDateInTheGroupsTimeZone() =
        runTest {
            asOfClock.asOf.value = THREE_O_CLOCK_UTC

            repository.observeTenant("t1").test {
                skipItems(1)
                backend.answerTenantScreen(rentTerms = emptyList())

                assertEquals(LocalDate(2026, 11, 14), awaitSynced().today)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aServerErrorKeepsTheCachedTenantAndReportsTheFailure() =
        runTest {
            repository.observeTenant("t1").test {
                skipItems(1)
                backend.answerTenantScreen(rentTerms = emptyList())
                awaitSynced()

                backend.answerSubscription(
                    TENANT_SCREEN,
                    Err(BackendError.Convex("NotFound", "This tenant no longer exists.", null)),
                )

                val failed = awaitItem()
                assertEquals(SyncStatus.Failed(TenantError.NotFound), failed.status)
                assertEquals(
                    "Apt 3B",
                    failed.value
                        ?.tenant
                        ?.details
                        ?.name,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun subscribesWithTheTenantAndTheCurrentHour() =
        runTest {
            repository.observeTenant("t1").test {
                skipItems(1)
                cancelAndIgnoreRemainingEvents()
            }

            val expectedArgs =
                buildJsonObject {
                    put("asOf", TEN_O_CLOCK_UTC)
                    put("tenantId", "t1")
                }
            assertEquals(listOf(TENANT_SCREEN to expectedArgs), backend.subscriptions)
            assertEquals(0, backend.openSubscriptionCount)
        }

    @Test
    fun addingATenantSendsItsDetailsAndFirstRentTermWithoutEmptyFields() =
        runTest {
            backend.mutationAnswers["tenants:add"] = Ok(JsonPrimitive("t9"))

            val result = repository.addTenant("g1", details, FirstRentTerm(amountCents = 123_456, dueDay = 15))

            assertEquals(Ok("t9"), result)
            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 123_456)
                    put("contactName", "Dana Whitfield")
                    put("depositCents", 150_000)
                    put("dueDay", 15)
                    put("graceDaysOverride", 3)
                    put("groupId", "g1")
                    put("moveIn", "2026-01-17")
                    put("name", "Apt 3B")
                }
            assertEquals(listOf("tenants:add" to expectedArgs), backend.mutations)
        }

    @Test
    fun editingATenantSendsTheExpectedVersion() =
        runTest {
            backend.mutationAnswers["tenants:edit"] = Ok(JsonNull)

            val result =
                repository.editTenant(
                    "t1",
                    expectedVersion = 7,
                    details = details.copy(moveOut = LocalDate(2026, 3, 31)),
                )

            assertEquals(Ok(Unit), result)
            val expectedArgs =
                buildJsonObject {
                    put("contactName", "Dana Whitfield")
                    put("depositCents", 150_000)
                    put("expectedVersion", 7)
                    put("graceDaysOverride", 3)
                    put("moveIn", "2026-01-17")
                    put("moveOut", "2026-03-31")
                    put("name", "Apt 3B")
                    put("tenantId", "t1")
                }
            assertEquals(listOf("tenants:edit" to expectedArgs), backend.mutations)
        }

    @Test
    fun archivingAndRestoringSendTheExpectedVersion() =
        runTest {
            backend.mutationAnswers["tenants:archive"] = Ok(JsonNull)
            backend.mutationAnswers["tenants:restore"] = Ok(JsonNull)

            assertEquals(Ok(Unit), repository.archiveTenant("t1", expectedVersion = 7))
            assertEquals(Ok(Unit), repository.restoreTenant("t1", expectedVersion = 8))

            assertEquals(
                listOf(
                    "tenants:archive" to versionArgs("tenantId", "t1", 7),
                    "tenants:restore" to versionArgs("tenantId", "t1", 8),
                ),
                backend.mutations,
            )
        }

    @Test
    fun addingARentTermSendsItsMonthAmountAndDueDay() =
        runTest {
            backend.mutationAnswers["rentTerms:add"] = Ok(JsonPrimitive("r9"))

            assertEquals(Ok("r9"), repository.addRentTerm("t1", draft))

            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 160_000)
                    put("dueDay", 31)
                    put("startMonth", "2026-06")
                    put("tenantId", "t1")
                }
            assertEquals(listOf("rentTerms:add" to expectedArgs), backend.mutations)
        }

    @Test
    fun editingARentTermSendsTheExpectedVersion() =
        runTest {
            backend.mutationAnswers["rentTerms:edit"] = Ok(JsonNull)

            assertEquals(Ok(Unit), repository.editRentTerm("r2", expectedVersion = 4, draft = draft))

            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 160_000)
                    put("dueDay", 31)
                    put("expectedVersion", 4)
                    put("rentTermId", "r2")
                    put("startMonth", "2026-06")
                }
            assertEquals(listOf("rentTerms:edit" to expectedArgs), backend.mutations)
        }

    @Test
    fun deletingARentTermSendsTheReasonAndExpectedVersion() =
        runTest {
            backend.mutationAnswers["rentTerms:delete"] = Ok(JsonNull)

            assertEquals(Ok(Unit), repository.deleteRentTerm("r2", expectedVersion = 4, reason = "Entered by mistake"))

            val expectedArgs =
                buildJsonObject {
                    put("expectedVersion", 4)
                    put("reason", "Entered by mistake")
                    put("rentTermId", "r2")
                }
            assertEquals(listOf("rentTerms:delete" to expectedArgs), backend.mutations)
        }

    @Test
    fun serverErrorCodesBecomeTenantErrors() =
        runTest {
            val cases =
                listOf(
                    "ValidationFailed" to TenantError.ValidationFailed(MESSAGE),
                    "EditConflict" to TenantError.EditConflict(MESSAGE),
                    "NotFound" to TenantError.NotFound,
                    "TenantArchived" to TenantError.TenantArchived(MESSAGE),
                )

            cases.forEach { (code, expected) ->
                backend.mutationAnswers["rentTerms:edit"] = Err(BackendError.Convex(code, MESSAGE, null))

                assertEquals(
                    Err(expected),
                    repository.editRentTerm("r2", expectedVersion = 4, draft = draft),
                    "code = $code",
                )
            }
        }

    @Test
    fun aChangeWithoutAConnectionReportsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(TenantError.Offline), repository.archiveTenant("t1", expectedVersion = 7))
        }

    @Test
    fun previewingARentTermAsksOnceAndReturnsTheBalanceBeforeAndAfter() =
        runTest {
            backend.answerSubscription(
                PREVIEW_RENT_TERM,
                Ok(Json.encodeToJsonElement(RentTermsPreviewResult.serializer(), previewResult())),
            )

            val result = repository.previewRentTerm("t1", rentTermId = "r2", draft = draft)

            assertEquals(
                Ok(
                    RentTermPreview(
                        before = BalanceTotal(owedCents = 55_000, creditCents = 0),
                        after = BalanceTotal(owedCents = 0, creditCents = 40_000),
                    ),
                ),
                result,
            )
            val expectedArgs =
                buildJsonObject {
                    put("asOf", TEN_O_CLOCK_UTC)
                    putJsonObject("proposed") {
                        put("amountCents", 160_000)
                        put("dueDay", 31)
                        put("rentTermId", "r2")
                        put("startMonth", "2026-06")
                    }
                    put("tenantId", "t1")
                }
            assertEquals(listOf(PREVIEW_RENT_TERM to expectedArgs), backend.subscriptions)
            assertEquals(0, backend.openSubscriptionCount)
        }

    @Test
    fun aPreviewWithoutAConnectionReportsOffline() =
        runTest {
            assertEquals(Err(TenantError.Offline), repository.previewRentTerm("t1", rentTermId = null, draft = draft))
        }

    private suspend fun ReceiveTurbine<Synced<TenantOverview?, TenantError>>.awaitSynced(): TenantOverview =
        awaitItem().also { assertEquals(SyncStatus.Synced, it.status) }.value.let(::checkNotNull)

    private fun FakeBackend.answerTenantScreen(
        rentTerms: List<RentTerm>,
        summary: TenantSummary = summary(),
    ) {
        val result =
            TenantsScreenResult(
                groups = listOf(group()),
                rentTerms = rentTerms,
                tenantSummaries = listOf(summary),
                tenants = listOf(tenant()),
            )
        answerSubscription(TENANT_SCREEN, Ok(Json.encodeToJsonElement(TenantsScreenResult.serializer(), result)))
    }

    private suspend fun cachedRentTermIds(tenantId: String): List<String> =
        database
            .rentTermDao()
            .observeByTenantId(tenantId)
            .first()
            .map { it.id }

    private fun versionArgs(
        idName: String,
        id: String,
        expectedVersion: Long,
    ): JsonObject =
        buildJsonObject {
            put("expectedVersion", expectedVersion)
            put(idName, id)
        }

    private fun group() =
        Group(
            creationTime = 1,
            id = "g1",
            graceDays = 5,
            lateFeeFlatCents = 5_000,
            name = "1st Street",
            ownerId = "owner",
            paymentOrder = GroupPaymentOrder.RentFirst,
            timeZone = "America/Chicago",
            version = 1,
        )

    private fun tenant() =
        Tenant(
            creationTime = 2,
            id = "t1",
            groupId = "g1",
            moveIn = "2024-01-01",
            name = "Apt 3B",
            status = TenantStatus.Active,
            version = 7,
        )

    private fun rentTerm(
        id: String,
        startMonth: String,
        amountCents: Long = 145_000,
    ) = RentTerm(
        creationTime = 3,
        id = id,
        amountCents = amountCents,
        dueDay = 1,
        startMonth = startMonth,
        tenantId = "t1",
        version = 1,
    )

    private fun summary() =
        TenantSummary(
            creditCents = 0,
            lateFeesOwedCents = 0,
            otherOwedCents = 0,
            rentOwedCents = 0,
            state = TenantSummaryState.PaidUp,
            tenantId = "t1",
        )

    private fun previewResult() =
        RentTermsPreviewResult(
            after =
                RentTermsPreviewAfter(
                    creditCents = 40_000,
                    lateFeesOwedCents = 0,
                    otherOwedCents = 0,
                    rentOwedCents = 0,
                    state = RentTermsPreviewAfterState.PaidUp,
                    tenantId = "t1",
                ),
            before =
                RentTermsPreviewBefore(
                    creditCents = 0,
                    lateFeesOwedCents = 5_000,
                    otherOwedCents = 0,
                    rentOwedCents = 50_000,
                    state = RentTermsPreviewBeforeState.Late,
                    tenantId = "t1",
                ),
        )

    private companion object {
        const val TENANT_SCREEN = "tenants:screen"
        const val PREVIEW_RENT_TERM = "rentTerms:preview"
        const val MESSAGE = "Sam changed this rent term while you were editing."
        const val TEN_O_CLOCK_UTC = 1_794_736_800_000L
        const val THREE_O_CLOCK_UTC = 1_794_711_600_000L
    }
}
