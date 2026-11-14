package net.neednot.renttracker.feature.debts.data

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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.data.accountNameEntity
import net.neednot.renttracker.core.data.debtEntity
import net.neednot.renttracker.core.data.groupEntity
import net.neednot.renttracker.core.data.membershipEntity
import net.neednot.renttracker.core.data.tenantEntity
import net.neednot.renttracker.core.data.tenantSummaryEntity
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.domain.ChargeEntryOverview
import net.neednot.renttracker.feature.debts.domain.DebtBalance
import net.neednot.renttracker.feature.debts.domain.DebtError
import net.neednot.renttracker.feature.debts.domain.DuplicateLateFee
import net.neednot.renttracker.feature.debts.domain.DuplicatePolicy
import net.neednot.renttracker.feature.debts.domain.SavedCharge
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DebtRepositoryImplTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()

    private val repository =
        DebtRepositoryImpl(
            backend = backend,
            asOfClock = FakeAsOfClock(TEN_O_CLOCK_UTC),
            database = database,
            screenResultStore = ScreenResultStore(database),
            telemetry = NoOpTelemetry(),
        )

    private val lateFee =
        ChargeDraft.LateFee(
            amountCents = 5_000,
            date = LocalDate(2026, 3, 10),
            forMonth = YearMonth(2026, 3),
            note = "Paid Mar 8",
        )
    private val custom =
        ChargeDraft.Custom(
            amountCents = 4_250,
            date = LocalDate(2026, 3, 10),
            note = "Water bill, Feb",
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun addingALateFeeCallsAddLateFeeWithTheMonthAndNote() =
        runTest {
            backend.mutationAnswers[ADD_LATE_FEE] = Ok(JsonPrimitive("d9"))

            val result = repository.addCharge("c1", "t1", lateFee, DuplicatePolicy.AskFirst)

            assertEquals(Ok("d9"), result)
            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 5_000)
                    put("clientId", "c1")
                    put("date", "2026-03-10")
                    put("forMonth", "2026-03")
                    put("note", "Paid Mar 8")
                    put("tenantId", "t1")
                }
            assertEquals(listOf(ADD_LATE_FEE to expectedArgs), backend.mutations)
        }

    @Test
    fun addingALateFeeWithoutANoteOmitsIt() =
        runTest {
            backend.mutationAnswers[ADD_LATE_FEE] = Ok(JsonPrimitive("d9"))

            repository.addCharge("c1", "t1", lateFee.copy(note = null), DuplicatePolicy.AskFirst)

            assertEquals(null, backend.mutations.single().second["note"])
        }

    @Test
    fun addingAnywayResendsTheSameRequestWithConfirmed() =
        runTest {
            backend.mutationAnswers[ADD_LATE_FEE] = Ok(JsonPrimitive("d9"))

            repository.addCharge("c1", "t1", lateFee, DuplicatePolicy.AskFirst)
            repository.addCharge("c1", "t1", lateFee, DuplicatePolicy.AddAnyway)

            val (first, second) = backend.mutations.map { it.second }
            assertEquals(null, first["confirmed"])
            assertEquals(JsonPrimitive(true), second["confirmed"])
            assertEquals(first - "confirmed", second - "confirmed")
        }

    @Test
    fun addingACustomDebtCallsAddCustomWithTheNoteAndNoMonth() =
        runTest {
            backend.mutationAnswers[ADD_CUSTOM] = Ok(JsonPrimitive("d9"))

            val result = repository.addCharge("c1", "t1", custom, DuplicatePolicy.AskFirst)

            assertEquals(Ok("d9"), result)
            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 4_250)
                    put("clientId", "c1")
                    put("date", "2026-03-10")
                    put("note", "Water bill, Feb")
                    put("tenantId", "t1")
                }
            assertEquals(listOf(ADD_CUSTOM to expectedArgs), backend.mutations)
        }

    @Test
    fun editingALateFeeSendsTheExpectedVersionAndTheMonth() =
        runTest {
            backend.mutationAnswers[EDIT] = Ok(JsonNull)

            val result = repository.editCharge("d1", expectedVersion = 3, draft = lateFee)

            assertEquals(Ok(Unit), result)
            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 5_000)
                    put("date", "2026-03-10")
                    put("debtId", "d1")
                    put("expectedVersion", 3)
                    put("forMonth", "2026-03")
                    put("note", "Paid Mar 8")
                }
            assertEquals(listOf(EDIT to expectedArgs), backend.mutations)
        }

    @Test
    fun editingACustomDebtSendsTheNoteButNoMonth() =
        runTest {
            backend.mutationAnswers[EDIT] = Ok(JsonNull)

            repository.editCharge("d1", expectedVersion = 3, draft = custom)

            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 4_250)
                    put("date", "2026-03-10")
                    put("debtId", "d1")
                    put("expectedVersion", 3)
                    put("note", "Water bill, Feb")
                }
            assertEquals(listOf(EDIT to expectedArgs), backend.mutations)
        }

    @Test
    fun serverErrorCodesBecomeDebtErrors() =
        runTest {
            val cases =
                listOf(
                    "ValidationFailed" to DebtError.ValidationFailed(MESSAGE),
                    "PermissionDenied" to DebtError.PermissionDenied(MESSAGE),
                    "NotFound" to DebtError.NotFound,
                    "TenantArchived" to DebtError.TenantArchived(MESSAGE),
                    "EditConflict" to DebtError.EditConflict(MESSAGE, editedByName = null),
                    "Surprise" to DebtError.Unexpected("Surprise: $MESSAGE"),
                )

            cases.forEach { (code, expected) ->
                backend.mutationAnswers[EDIT] = Err(BackendError.Convex(code, MESSAGE, null))

                assertEquals(Err(expected), repository.editCharge("d1", 3, lateFee), "code = $code")
            }
        }

    @Test
    fun anEditConflictCarriesTheNameOfWhoChangedTheCharge() =
        runTest {
            val details =
                buildJsonObject {
                    put("currentVersion", 4)
                    put("lastEditedByName", "Sam Rivera")
                }
            backend.mutationAnswers[EDIT] = Err(BackendError.Convex("EditConflict", MESSAGE, details))

            assertEquals(
                Err(DebtError.EditConflict(MESSAGE, editedByName = "Sam Rivera")),
                repository.editCharge("d1", 3, lateFee),
            )
        }

    @Test
    fun needsConfirmationListsTheExistingLateFees() =
        runTest {
            val details =
                buildJsonObject {
                    put(
                        "matches",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("kind", "lateFee")
                                    put("id", "d0")
                                    put("date", "2026-03-08")
                                    put("amountCents", 5_000)
                                    put("byName", "Sam Rivera")
                                    put("byAccountId", "sam-account")
                                },
                            )
                        },
                    )
                }
            backend.mutationAnswers[ADD_LATE_FEE] = Err(BackendError.Convex("NeedsConfirmation", MESSAGE, details))

            val result = repository.addCharge("c1", "t1", lateFee, DuplicatePolicy.AskFirst)

            assertEquals(
                Err(
                    DebtError.PossibleDuplicate(
                        listOf(DuplicateLateFee(LocalDate(2026, 3, 8), 5_000, "Sam Rivera", "sam-account")),
                    ),
                ),
                result,
            )
        }

    @Test
    fun aChangeWithoutAConnectionReportsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(DebtError.Offline), repository.addCharge("c1", "t1", custom, DuplicatePolicy.AskFirst))
        }

    @Test
    fun theEntrySubscribesToTheTenantScreenWithTheCurrentHour() =
        runTest {
            repository.observeEntry("t1", debtId = null).test {
                skipItems(1)
                cancelAndIgnoreRemainingEvents()
            }

            val expectedArgs =
                buildJsonObject {
                    put("asOf", TEN_O_CLOCK_UTC)
                    put("tenantId", "t1")
                }
            assertEquals(listOf("tenants:screen" to expectedArgs), backend.subscriptions)
        }

    @Test
    fun theEntryOverviewComesFromTheCachedTenantScreen() =
        runTest {
            seedTenant()

            assertEquals(
                ChargeEntryOverview(
                    tenantName = "Apt 3B",
                    groupName = "1st Street",
                    balance = DebtBalance(owedCents = 55_000, creditCents = 0),
                    today = LocalDate(2026, 11, 15),
                    callerAccountId = "account",
                    charge = null,
                ),
                cachedOverview(debtId = null),
            )
        }

    @Test
    fun aSavedLateFeeIsPartOfTheOverviewWhenOneIsRequested() =
        runTest {
            seedTenant()
            database.debtDao().upsert(
                listOf(
                    debtEntity("d1", "t1", DebtKind.LateFee)
                        .copy(forMonth = "2026-02", note = "After grace", date = "2026-02-06", version = 3),
                ),
            )

            assertEquals(
                SavedCharge(
                    debtId = "d1",
                    version = 3,
                    draft = ChargeDraft.LateFee(5_000, LocalDate(2026, 2, 6), YearMonth(2026, 2), "After grace"),
                ),
                cachedOverview(debtId = "d1").charge,
            )
        }

    @Test
    fun aSavedCustomDebtIsPartOfTheOverviewWhenOneIsRequested() =
        runTest {
            seedTenant()
            database.debtDao().upsert(listOf(debtEntity("d1", "t1", DebtKind.Custom).copy(note = "Water bill")))

            assertEquals(
                ChargeDraft.Custom(5_000, LocalDate(2026, 3, 10), "Water bill"),
                cachedOverview(debtId = "d1").charge?.draft,
            )
        }

    @Test
    fun aCancelledChargeIsNotShownForEditing() =
        runTest {
            seedTenant()
            database.debtDao().upsert(listOf(debtEntity("d1", "t1").copy(note = "Water bill", deletedAt = 99)))

            assertEquals(null, cachedOverview(debtId = "d1").charge)
        }

    private suspend fun seedTenant() {
        database.groupDao().upsert(listOf(groupEntity("g1", name = "1st Street")))
        database.tenantDao().upsert(listOf(tenantEntity("t1", "g1", name = "Apt 3B")))
        database.tenantSummaryDao().upsert(
            listOf(tenantSummaryEntity("t1", TenantBalanceState.Owes, rentOwedCents = 55_000)),
        )
        database.membershipDao().upsert(listOf(membershipEntity("m1", "g1")))
        database.accountNameDao().upsert(listOf(accountNameEntity("account", "Dana")))
    }

    private suspend fun cachedOverview(debtId: String?): ChargeEntryOverview {
        val snapshot = repository.observeEntry("t1", debtId).first()
        assertEquals(SyncStatus.Syncing, snapshot.status)
        return checkNotNull(snapshot.value)
    }

    private companion object {
        const val ADD_LATE_FEE = "debts:addLateFee"
        const val ADD_CUSTOM = "debts:addCustom"
        const val EDIT = "debts:edit"
        const val MESSAGE = "Sam changed this charge while you were editing."
        const val TEN_O_CLOCK_UTC = 1_794_736_800_000L
    }
}
