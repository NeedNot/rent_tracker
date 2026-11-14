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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.LateFeeSuggestion
import net.neednot.renttracker.feature.tenants.domain.LedgerBalance
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.model.shared.Group
import net.neednot.renttracker.model.shared.GroupPaymentOrder
import net.neednot.renttracker.model.shared.Tenant
import net.neednot.renttracker.model.shared.TenantStatus
import net.neednot.renttracker.model.shared.TenantSummary
import net.neednot.renttracker.model.shared.TenantSummaryState
import net.neednot.renttracker.model.tenants.Account
import net.neednot.renttracker.model.tenants.Debt
import net.neednot.renttracker.model.tenants.DebtKind
import net.neednot.renttracker.model.tenants.LedgerRow
import net.neednot.renttracker.model.tenants.LedgerRowKind
import net.neednot.renttracker.model.tenants.Membership
import net.neednot.renttracker.model.tenants.MembershipDebts
import net.neednot.renttracker.model.tenants.MembershipMembers
import net.neednot.renttracker.model.tenants.MembershipPayments
import net.neednot.renttracker.model.tenants.MembershipRentTerms
import net.neednot.renttracker.model.tenants.MembershipRole
import net.neednot.renttracker.model.tenants.MembershipSettings
import net.neednot.renttracker.model.tenants.MembershipTenants
import net.neednot.renttracker.model.tenants.Payment
import net.neednot.renttracker.model.tenants.TenantsScreenResult
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import net.neednot.renttracker.model.tenants.PaymentMethod as ConvexPaymentMethod

class TenantLedgerRepositoryTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()

    private val repository =
        TenantRepositoryImpl(
            backend = backend,
            asOfClock = FakeAsOfClock(TEN_O_CLOCK_UTC),
            database = database,
            screenResultStore = ScreenResultStore(database),
            telemetry = NoOpTelemetry(),
        )

    private val ledgerEntries = LedgerEntryRepositoryImpl(backend = backend, telemetry = NoOpTelemetry())

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun theLedgerJoinsRowsWithPaymentsDebtsAndNames() =
        runTest {
            repository.observeTenant("t1").test {
                skipItems(1)
                backend.answerTenantScreen()

                val ledger = awaitSynced().ledger

                assertEquals(listOf("payment:p1", "debt:d1", "rent:2026-03"), ledger.map { it.key })
                val payment = ledger[0] as LedgerEntry.Payment
                assertEquals(PaymentMethod.Zelle, payment.payment.method)
                assertEquals(Person.Named("Sam Rivera"), payment.payment.loggedBy)
                assertEquals(LedgerBalance.Owed(67_000), payment.balance)
                val charge = ledger[1] as LedgerEntry.Charge
                assertEquals(ChargeKind.Custom("Water bill, Feb"), charge.charge.kind)
                assertEquals(Person.You, charge.charge.addedBy)
                assertEquals(EntryStatus.Active, charge.charge.status)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun theCallersMembershipDecidesWhatTheyMayDo() =
        runTest {
            repository.observeTenant("t1").test {
                skipItems(1)
                backend.answerTenantScreen(membership = membership(MembershipPayments.AddEdit, MembershipDebts.Read))

                val overview = awaitSynced()

                assertEquals(true, overview.permissions?.canLogPayments)
                assertEquals(false, overview.permissions?.canAddCharges)
                val payment = overview.ledger[0] as LedgerEntry.Payment
                assertEquals(false, payment.payment.canChange)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun theServersLateFeeSuggestionIsShownToACallerWhoCanAddCharges() =
        runTest {
            repository.observeTenant("t1").test {
                skipItems(1)
                backend.answerTenantScreen(
                    summary = summary().copy(lateFeeSuggestionMonth = "2026-03", lateFeeSuggestionCents = 5_000),
                )

                assertEquals(LateFeeSuggestion(YearMonth(2026, 3), 5_000), awaitSynced().lateFeeSuggestion)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dismissingASuggestionStoresItOnThisPhoneAndHidesItWithoutTellingTheServer() =
        runTest {
            repository.observeTenant("t1").test {
                skipItems(1)
                backend.answerTenantScreen(
                    summary = summary().copy(lateFeeSuggestionMonth = "2026-03", lateFeeSuggestionCents = 5_000),
                )
                awaitSynced()

                repository.dismissLateFeeSuggestion("t1", YearMonth(2026, 3))

                assertNull(awaitSynced().lateFeeSuggestion)
                cancelAndIgnoreRemainingEvents()
            }
            val stored = database.dismissedLateFeeSuggestionDao().observeDismissedMonths("t1").first()
            assertEquals(listOf("2026-03"), stored)
            assertTrue(backend.mutations.isEmpty())
        }

    @Test
    fun dismissingTheSameMonthTwiceIsHarmless() =
        runTest {
            repository.dismissLateFeeSuggestion("t1", YearMonth(2026, 3))
            repository.dismissLateFeeSuggestion("t1", YearMonth(2026, 3))

            assertEquals(
                listOf("2026-03"),
                database.dismissedLateFeeSuggestionDao().observeDismissedMonths("t1").first(),
            )
        }

    @Test
    fun cancellingAPaymentSendsTheReasonAndExpectedVersion() =
        runTest {
            backend.mutationAnswers["payments:delete"] = Ok(JsonNull)

            assertEquals(Ok(Unit), ledgerEntries.cancelPayment("p1", expectedVersion = 4, reason = "Entered twice"))

            val expected =
                buildJsonObject {
                    put("expectedVersion", 4)
                    put("paymentId", "p1")
                    put("reason", "Entered twice")
                }
            assertEquals(listOf("payments:delete" to expected), backend.mutations)
        }

    @Test
    fun restoringAndBouncingAPaymentSendTheExpectedVersion() =
        runTest {
            backend.mutationAnswers["payments:restore"] = Ok(JsonNull)
            backend.mutationAnswers["payments:markBounced"] = Ok(JsonNull)

            assertEquals(Ok(Unit), ledgerEntries.restorePayment("p1", expectedVersion = 4))
            assertEquals(Ok(Unit), ledgerEntries.markPaymentBounced("p2", expectedVersion = 5))

            assertEquals(
                listOf(
                    "payments:restore" to versionArgs("paymentId", "p1", 4),
                    "payments:markBounced" to versionArgs("paymentId", "p2", 5),
                ),
                backend.mutations,
            )
        }

    @Test
    fun cancellingAndRestoringADebtSendTheExpectedVersion() =
        runTest {
            backend.mutationAnswers["debts:delete"] = Ok(JsonNull)
            backend.mutationAnswers["debts:restore"] = Ok(JsonNull)

            assertEquals(Ok(Unit), ledgerEntries.cancelDebt("d1", expectedVersion = 2, reason = "Wrong tenant"))
            assertEquals(Ok(Unit), ledgerEntries.restoreDebt("d1", expectedVersion = 3))

            val cancel =
                buildJsonObject {
                    put("debtId", "d1")
                    put("expectedVersion", 2)
                    put("reason", "Wrong tenant")
                }
            assertEquals(
                listOf("debts:delete" to cancel, "debts:restore" to versionArgs("debtId", "d1", 3)),
                backend.mutations,
            )
        }

    @Test
    fun theBouncedCheckFeeIsACustomDebtDatedTodayWithAFreshClientId() =
        runTest {
            backend.mutationAnswers["debts:addCustom"] = Ok(JsonPrimitive("d9"))

            assertEquals(
                Ok(Unit),
                ledgerEntries.addBouncedCheckFee("t1", amountCents = 3_500, date = LocalDate(2026, 3, 10)),
            )
            assertEquals(
                Ok(Unit),
                ledgerEntries.addBouncedCheckFee("t1", amountCents = 3_500, date = LocalDate(2026, 3, 10)),
            )

            val (names, argsList) = backend.mutations.unzip()
            assertEquals(listOf("debts:addCustom", "debts:addCustom"), names)
            val first = argsList[0]
            assertEquals(JsonPrimitive(3_500), first["amountCents"])
            assertEquals(JsonPrimitive("2026-03-10"), first["date"])
            assertEquals(JsonPrimitive("Bounced-check fee"), first["note"])
            assertEquals(JsonPrimitive("t1"), first["tenantId"])
            assertEquals(setOf("amountCents", "clientId", "date", "note", "tenantId"), first.keys)
            assertTrue(first.getValue("clientId").toString().length > 2)
            assertNotEquals(first["clientId"], argsList[1]["clientId"])
        }

    @Test
    fun serverErrorCodesOnLedgerChangesBecomeTenantErrors() =
        runTest {
            val cases =
                listOf(
                    "PermissionDenied" to TenantError.PermissionDenied(MESSAGE),
                    "NotFound" to TenantError.NotFound,
                    "ValidationFailed" to TenantError.ValidationFailed(MESSAGE),
                    "EditConflict" to TenantError.EditConflict(MESSAGE),
                    "TenantArchived" to TenantError.TenantArchived(MESSAGE),
                )

            cases.forEach { (code, expected) ->
                val error = Err(BackendError.Convex(code, MESSAGE, null))
                listOf(
                    "payments:delete",
                    "payments:restore",
                    "payments:markBounced",
                    "debts:delete",
                    "debts:restore",
                    "debts:addCustom",
                ).forEach { backend.mutationAnswers[it] = error }

                assertEquals(Err(expected), ledgerEntries.cancelPayment("p1", 1, "r"), "cancelPayment $code")
                assertEquals(Err(expected), ledgerEntries.restorePayment("p1", 1), "restorePayment $code")
                assertEquals(Err(expected), ledgerEntries.markPaymentBounced("p1", 1), "markPaymentBounced $code")
                assertEquals(Err(expected), ledgerEntries.cancelDebt("d1", 1, "r"), "cancelDebt $code")
                assertEquals(Err(expected), ledgerEntries.restoreDebt("d1", 1), "restoreDebt $code")
                assertEquals(
                    Err(expected),
                    ledgerEntries.addBouncedCheckFee("t1", 100, LocalDate(2026, 3, 10)),
                    "fee $code",
                )
            }
        }

    @Test
    fun ledgerChangesWithoutAConnectionReportOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(TenantError.Offline), ledgerEntries.cancelPayment("p1", 1, "reason"))
            assertEquals(Err(TenantError.Offline), ledgerEntries.restoreDebt("d1", 1))
        }

    private suspend fun ReceiveTurbine<Synced<TenantOverview?, TenantError>>.awaitSynced(): TenantOverview =
        awaitItem().also { assertEquals(SyncStatus.Synced, it.status) }.value.let(::checkNotNull)

    private fun versionArgs(
        idName: String,
        id: String,
        expectedVersion: Long,
    ) = buildJsonObject {
        put("expectedVersion", expectedVersion)
        put(idName, id)
    }

    private fun FakeBackend.answerTenantScreen(
        summary: TenantSummary = summary(),
        membership: Membership = membership(MembershipPayments.Full, MembershipDebts.Full),
    ) {
        val result =
            TenantsScreenResult(
                accounts = listOf(Account("sam", "Sam Rivera"), Account("me", "Me")),
                debts = listOf(debt()),
                groups = listOf(group()),
                ledgerRows = ledgerRows(),
                memberships = listOf(membership),
                payments = listOf(payment()),
                rentTerms = emptyList(),
                tenantSummaries = listOf(summary),
                tenants = listOf(tenant()),
            )
        answerSubscription(TENANT_SCREEN, Ok(Json.encodeToJsonElement(TenantsScreenResult.serializer(), result)))
    }

    private fun ledgerRows() =
        listOf(
            ledgerRow("payment:p1", LedgerRowKind.Payment, "p1")
                .copy(date = "2026-03-02", position = 2, balanceOwedCents = 67_000, amountCents = 150_000),
            ledgerRow("debt:d1", LedgerRowKind.Debt, "d1")
                .copy(date = "2026-03-01", position = 1, balanceOwedCents = 217_000, amountCents = 12_000),
            ledgerRow("rent:2026-03", LedgerRowKind.Rent, null)
                .copy(date = "2026-03-01", position = 0, balanceOwedCents = 205_000, month = "2026-03"),
        )

    private fun ledgerRow(
        key: String,
        kind: LedgerRowKind,
        entryId: String?,
    ) = LedgerRow(
        amountCents = 150_000,
        date = "2026-03-01",
        entryId = entryId,
        key = key,
        kind = kind,
        mayNotApply = false,
        position = 0,
        tenantId = "t1",
    )

    private fun payment() =
        Payment(
            creationTime = Instant.parse("2026-03-02T18:00:00Z").toEpochMilliseconds().toDouble(),
            id = "p1",
            amountCents = 150_000,
            clientId = "c1",
            dateReceived = "2026-03-02",
            loggedBy = "sam",
            method = ConvexPaymentMethod.Zelle,
            tenantId = "t1",
            version = 4,
        )

    private fun debt() =
        Debt(
            creationTime = Instant.parse("2026-03-01T18:00:00Z").toEpochMilliseconds().toDouble(),
            id = "d1",
            amountCents = 12_000,
            createdBy = "me",
            date = "2026-03-01",
            kind = DebtKind.Custom,
            note = "Water bill, Feb",
            tenantId = "t1",
            version = 2,
        )

    private fun membership(
        payments: MembershipPayments,
        debts: MembershipDebts,
    ) = Membership(
        creationTime = 1.25,
        id = "m1",
        accountId = "me",
        debts = debts,
        editOthers = false,
        groupId = "g1",
        members = MembershipMembers.Read,
        notificationsOn = true,
        payments = payments,
        rentTerms = MembershipRentTerms.Read,
        role = MembershipRole.Member,
        settings = MembershipSettings.Read,
        tenants = MembershipTenants.Read,
        version = 1,
    )

    private fun group() =
        Group(
            creationTime = 1.25,
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
            creationTime = 2.25,
            id = "t1",
            groupId = "g1",
            moveIn = "2024-01-01",
            name = "Apt 3B",
            status = TenantStatus.Active,
            version = 7,
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

    private companion object {
        const val TENANT_SCREEN = "tenants:screen"
        const val MESSAGE = "The server says no."
        const val TEN_O_CLOCK_UTC = 1_794_736_800_000L
    }
}
