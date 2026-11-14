package net.neednot.renttracker.feature.payments.data

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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.DismissedLateFeeSuggestionEntity
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.data.accountNameEntity
import net.neednot.renttracker.core.data.groupEntity
import net.neednot.renttracker.core.data.membershipEntity
import net.neednot.renttracker.core.data.paymentEntity
import net.neednot.renttracker.core.data.tenantEntity
import net.neednot.renttracker.core.data.tenantSummaryEntity
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.payments.domain.DuplicateKind
import net.neednot.renttracker.feature.payments.domain.DuplicateMatch
import net.neednot.renttracker.feature.payments.domain.DuplicatePolicy
import net.neednot.renttracker.feature.payments.domain.LateFeeDraft
import net.neednot.renttracker.feature.payments.domain.LateFeeSuggestion
import net.neednot.renttracker.feature.payments.domain.MonthState
import net.neednot.renttracker.feature.payments.domain.MonthStateChange
import net.neednot.renttracker.feature.payments.domain.PaymentBalance
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.SavedPayment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentRepositoryImplTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()

    private val repository =
        PaymentRepositoryImpl(
            backend = backend,
            asOfClock = FakeAsOfClock(TEN_O_CLOCK_UTC),
            database = database,
            screenResultStore = ScreenResultStore(database),
            telemetry = NoOpTelemetry(),
        )

    private val draft =
        PaymentDraft(
            amountCents = 150_000,
            dateReceived = LocalDate(2026, 3, 10),
            method = PaymentMethod.BankTransfer,
            note = "March rent",
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun loggingAPaymentSendsItsFormWithoutTheOptionalFieldsItLeavesOut() =
        runTest {
            backend.mutationAnswers[LOG] = Ok(buildJsonObject { put("paymentId", "p9") })

            val result =
                repository.logPayment(
                    clientId = "c1",
                    tenantId = "t1",
                    draft = PaymentDraft(150_000, LocalDate(2026, 3, 10), method = null, note = null),
                    lateFee = null,
                    duplicatePolicy = DuplicatePolicy.AskFirst,
                )

            assertEquals(Ok("p9"), result)
            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 150_000)
                    put("clientId", "c1")
                    put("dateReceived", "2026-03-10")
                    put("tenantId", "t1")
                }
            assertEquals(listOf(LOG to expectedArgs), backend.mutations)
        }

    @Test
    fun loggingAPaymentWithALateFeeSendsBothInOneCall() =
        runTest {
            backend.mutationAnswers[LOG] = Ok(buildJsonObject { put("paymentId", "p9") })

            repository.logPayment(
                clientId = "c1",
                tenantId = "t1",
                draft = draft,
                lateFee = LateFeeDraft(amountCents = 5_000, forMonth = YearMonth(2026, 3)),
                duplicatePolicy = DuplicatePolicy.AskFirst,
            )

            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 150_000)
                    put("clientId", "c1")
                    put("dateReceived", "2026-03-10")
                    putJsonObject("lateFee") {
                        put("amountCents", 5_000)
                        put("forMonth", "2026-03")
                    }
                    put("method", "bankTransfer")
                    put("note", "March rent")
                    put("tenantId", "t1")
                }
            assertEquals(listOf(LOG to expectedArgs), backend.mutations)
        }

    @Test
    fun savingAnywayResendsTheSameRequestWithConfirmed() =
        runTest {
            backend.mutationAnswers[LOG] = Ok(buildJsonObject { put("paymentId", "p9") })

            repository.logPayment("c1", "t1", draft, lateFee = null, duplicatePolicy = DuplicatePolicy.AskFirst)
            repository.logPayment("c1", "t1", draft, lateFee = null, duplicatePolicy = DuplicatePolicy.SaveAnyway)

            val (first, second) = backend.mutations.map { it.second }
            assertEquals(null, first["confirmed"])
            assertEquals(JsonElementTrue, second["confirmed"])
            assertEquals(first["clientId"], second["clientId"])
            assertEquals(first - "confirmed", second - "confirmed")
        }

    @Test
    fun editingAPaymentSendsTheExpectedVersionAndTheWholeForm() =
        runTest {
            backend.mutationAnswers[EDIT] = Ok(JsonNull)

            val result = repository.editPayment("p1", expectedVersion = 4, draft = draft)

            assertEquals(Ok(Unit), result)
            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 150_000)
                    put("dateReceived", "2026-03-10")
                    put("expectedVersion", 4)
                    put("method", "bankTransfer")
                    put("note", "March rent")
                    put("paymentId", "p1")
                }
            assertEquals(listOf(EDIT to expectedArgs), backend.mutations)
        }

    @Test
    fun editingAPaymentWithoutMethodOrNoteOmitsThemSoTheServerClearsThem() =
        runTest {
            backend.mutationAnswers[EDIT] = Ok(JsonNull)

            repository.editPayment("p1", expectedVersion = 4, draft = draft.copy(method = null, note = null))

            val expectedArgs =
                buildJsonObject {
                    put("amountCents", 150_000)
                    put("dateReceived", "2026-03-10")
                    put("expectedVersion", 4)
                    put("paymentId", "p1")
                }
            assertEquals(listOf(EDIT to expectedArgs), backend.mutations)
        }

    @Test
    fun previewingAnEditAsksOnceWithTheProposedAmountAndDateAndMapsTheChangedMonths() =
        runTest {
            backend.answerSubscription(
                PREVIEW,
                Ok(
                    buildJsonObject {
                        put(
                            "changes",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("month", "2026-02")
                                        put("before", "paid")
                                        put("after", "partial")
                                    },
                                )
                                add(
                                    buildJsonObject {
                                        put("month", "2026-03")
                                        put("before", "dueNotLate")
                                        put("after", "owed")
                                    },
                                )
                            },
                        )
                    },
                ),
            )

            val result = repository.previewPaymentChange("t1", "p1", 100_000, LocalDate(2026, 3, 5))

            assertEquals(
                Ok(
                    listOf(
                        MonthStateChange(YearMonth(2026, 2), MonthState.Paid, MonthState.Partial),
                        MonthStateChange(YearMonth(2026, 3), MonthState.DueNotLate, MonthState.Owed),
                    ),
                ),
                result,
            )
            val expectedArgs =
                buildJsonObject {
                    put("asOf", TEN_O_CLOCK_UTC)
                    putJsonObject("proposed") {
                        put("amountCents", 100_000)
                        put("dateReceived", "2026-03-05")
                        put("paymentId", "p1")
                    }
                    put("tenantId", "t1")
                }
            assertEquals(listOf(PREVIEW to expectedArgs), backend.subscriptions)
            assertEquals(0, backend.openSubscriptionCount)
        }

    @Test
    fun aPreviewValidationErrorBecomesAPaymentError() =
        runTest {
            backend.answerSubscription(PREVIEW, Err(BackendError.Convex("ValidationFailed", MESSAGE, null)))

            assertEquals(
                Err(PaymentError.ValidationFailed(MESSAGE)),
                repository.previewPaymentChange("t1", "p1", 100_000, LocalDate(2026, 3, 5)),
            )
        }

    @Test
    fun aPreviewWithoutAConnectionReportsOffline() =
        runTest {
            assertEquals(
                Err(PaymentError.Offline),
                repository.previewPaymentChange("t1", "p1", 100_000, LocalDate(2026, 3, 5)),
            )
        }

    @Test
    fun serverErrorCodesBecomePaymentErrors() =
        runTest {
            val cases =
                listOf(
                    "ValidationFailed" to PaymentError.ValidationFailed(MESSAGE),
                    "PermissionDenied" to PaymentError.PermissionDenied(MESSAGE),
                    "NotFound" to PaymentError.NotFound,
                    "TenantArchived" to PaymentError.TenantArchived(MESSAGE),
                    "EditConflict" to PaymentError.EditConflict(MESSAGE, editedByName = null),
                    "Surprise" to PaymentError.Unexpected("Surprise: $MESSAGE"),
                )

            cases.forEach { (code, expected) ->
                backend.mutationAnswers[EDIT] = Err(BackendError.Convex(code, MESSAGE, null))

                assertEquals(Err(expected), repository.editPayment("p1", 4, draft), "code = $code")
            }
        }

    @Test
    fun anEditConflictCarriesTheNameOfWhoChangedThePayment() =
        runTest {
            val details =
                buildJsonObject {
                    put("currentVersion", 5)
                    put("lastEditedBy", "a2")
                    put("lastEditedByName", "Sam Rivera")
                }
            backend.mutationAnswers[EDIT] = Err(BackendError.Convex("EditConflict", MESSAGE, details))

            assertEquals(
                Err(PaymentError.EditConflict(MESSAGE, editedByName = "Sam Rivera")),
                repository.editPayment("p1", 4, draft),
            )
        }

    @Test
    fun needsConfirmationListsThePaymentAndLateFeeMatches() =
        runTest {
            val details =
                buildJsonObject {
                    put(
                        "matches",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("kind", "payment")
                                    put("id", "p0")
                                    put("date", "2026-03-02")
                                    put("amountCents", 150_000)
                                    put("method", "check")
                                    put("byName", "Sam Rivera")
                                    put("byAccountId", "sam-account")
                                },
                            )
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
            backend.mutationAnswers[LOG] = Err(BackendError.Convex("NeedsConfirmation", MESSAGE, details))

            val result = repository.logPayment("c1", "t1", draft, null, DuplicatePolicy.AskFirst)

            val expected =
                PaymentError.PossibleDuplicate(
                    listOf(
                        DuplicateMatch(
                            DuplicateKind.Payment,
                            LocalDate(2026, 3, 2),
                            150_000,
                            PaymentMethod.Check,
                            "Sam Rivera",
                            "sam-account",
                        ),
                        DuplicateMatch(
                            DuplicateKind.LateFee,
                            LocalDate(2026, 3, 8),
                            5_000,
                            null,
                            "Sam Rivera",
                            "sam-account",
                        ),
                    ),
                )
            assertEquals(Err(expected), result)
        }

    @Test
    fun needsConfirmationWithoutReadableDetailsIsUnexpected() =
        runTest {
            backend.mutationAnswers[LOG] = Err(BackendError.Convex("NeedsConfirmation", MESSAGE, null))

            assertEquals(
                Err(PaymentError.Unexpected("NeedsConfirmation: $MESSAGE")),
                repository.logPayment("c1", "t1", draft, null, DuplicatePolicy.AskFirst),
            )
        }

    @Test
    fun aChangeWithoutAConnectionReportsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(PaymentError.Offline), repository.editPayment("p1", 4, draft))
        }

    @Test
    fun theEntrySubscribesToTheTenantScreenWithTheCurrentHour() =
        runTest {
            repository.observeEntry("t1", paymentId = null).test {
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
            seedTenant(lastPaymentMethod = PaymentMethod.Zelle, suggestionMonth = "2026-11", suggestionCents = 5_000)

            val overview = cachedOverview(paymentId = null)

            assertEquals(
                PaymentEntryOverview(
                    tenantName = "Apt 3B",
                    groupName = "1st Street",
                    balance = PaymentBalance(owedCents = 55_000, creditCents = 0),
                    today = LocalDate(2026, 11, 15),
                    lastPaymentMethod = PaymentMethod.Zelle,
                    lateFeeSuggestion = LateFeeSuggestion(YearMonth(2026, 11), 5_000),
                    canAddDebts = true,
                    callerAccountId = "account",
                    payment = null,
                ),
                overview,
            )
        }

    @Test
    fun aSuggestionDismissedOnThisPhoneIsLeftOut() =
        runTest {
            seedTenant(suggestionMonth = "2026-11", suggestionCents = 5_000)
            database.dismissedLateFeeSuggestionDao().insert(DismissedLateFeeSuggestionEntity("t1", "2026-11"))

            assertEquals(null, cachedOverview(paymentId = null).lateFeeSuggestion)
        }

    @Test
    fun aMemberWhoCannotAddDebtsIsNotOfferedTheLateFee() =
        runTest {
            seedTenant(role = MembershipRole.Member, debts = AccessLevel.Read)

            assertEquals(false, cachedOverview(paymentId = null).canAddDebts)
        }

    @Test
    fun aMemberWhoMayAddAndEditDebtsCanAddThem() =
        runTest {
            seedTenant(role = MembershipRole.Member, debts = AccessLevel.AddEdit)

            assertEquals(true, cachedOverview(paymentId = null).canAddDebts)
        }

    @Test
    fun theSavedPaymentIsPartOfTheOverviewWhenOneIsRequested() =
        runTest {
            seedTenant()
            database.paymentDao().upsert(
                listOf(
                    paymentEntity("p1", "t1", amountCents = 150_000)
                        .copy(
                            dateReceived = "2026-03-02",
                            method = PaymentMethod.Check,
                            note = "March rent",
                            version = 3,
                        ),
                ),
            )

            assertEquals(
                SavedPayment(
                    paymentId = "p1",
                    version = 3,
                    details = PaymentDraft(150_000, LocalDate(2026, 3, 2), PaymentMethod.Check, "March rent"),
                ),
                cachedOverview(paymentId = "p1").payment,
            )
        }

    @Test
    fun aDeletedPaymentIsNotShownForEditing() =
        runTest {
            seedTenant()
            database.paymentDao().upsert(listOf(paymentEntity("p1", "t1").copy(deletedAt = 99)))

            assertEquals(null, cachedOverview(paymentId = "p1").payment)
        }

    private suspend fun seedTenant(
        lastPaymentMethod: PaymentMethod? = null,
        suggestionMonth: String? = null,
        suggestionCents: Long? = null,
        role: MembershipRole = MembershipRole.Owner,
        debts: AccessLevel = AccessLevel.Full,
    ) {
        database.groupDao().upsert(listOf(groupEntity("g1", name = "1st Street")))
        database.tenantDao().upsert(
            listOf(tenantEntity("t1", "g1", name = "Apt 3B").copy(lastPaymentMethod = lastPaymentMethod)),
        )
        database.tenantSummaryDao().upsert(
            listOf(
                tenantSummaryEntity("t1", TenantBalanceState.Owes, rentOwedCents = 55_000)
                    .copy(lateFeeSuggestionMonth = suggestionMonth, lateFeeSuggestionCents = suggestionCents),
            ),
        )
        database.membershipDao().upsert(listOf(membershipEntity("m1", "g1", role).copy(debts = debts)))
        database.accountNameDao().upsert(listOf(accountNameEntity("account", "Dana")))
    }

    private suspend fun cachedOverview(paymentId: String?): PaymentEntryOverview {
        val snapshot = repository.observeEntry("t1", paymentId).first()
        assertEquals(SyncStatus.Syncing, snapshot.status)
        return checkNotNull(snapshot.value)
    }

    private companion object {
        const val LOG = "payments:log"
        const val EDIT = "payments:edit"
        const val PREVIEW = "payments:preview"
        const val MESSAGE = "Sam changed this payment while you were editing."
        const val TEN_O_CLOCK_UTC = 1_794_736_800_000L
        val JsonElementTrue: JsonElement = JsonPrimitive(true)
    }
}
