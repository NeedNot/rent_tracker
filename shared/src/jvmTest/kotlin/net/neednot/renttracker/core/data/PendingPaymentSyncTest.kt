package net.neednot.renttracker.core.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.model.shared.TenantStatus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingPaymentSyncTest {
    private val database = inMemoryDatabase()
    private val backend = FakeBackend()
    private val store = ScreenResultStore(database)
    private val reader = PendingPaymentReader(database)
    private val queue = PendingPaymentQueue(database, RecordingSyncScheduler())
    private val dao = database.pendingPaymentDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun anAcceptedPaymentLeavesThePhoneAndNamesTheGroupItWasLoggedIn() =
        runTest {
            queueWhileOffline("c1")
            backend.answerTenantScreen(tenantScreen())
            backend.mutationAnswers[LOG_PAYMENT] = Ok(buildJsonObject { put("paymentId", "p9") })

            val report = sync().syncAll()

            assertEquals(PendingSyncReport(emptyList(), retryLater = false), report)
            assertNull(dao.byClientId("c1"))
            val (name, args) = backend.mutations.single()
            assertEquals(LOG_PAYMENT, name)
            assertEquals(JsonPrimitive("c1"), args["clientId"])
            assertEquals(JsonPrimitive("g1"), args["expectedGroupId"])
            assertNull(args["confirmed"])
        }

    @Test
    fun paymentsAreSentInTheOrderTheyWereLoggedNotByTheirIds() =
        runTest {
            queueWhileOffline("z-first")
            queueWhileOffline("a-second")
            backend.answerTenantScreen(tenantScreen())
            backend.mutationAnswers[LOG_PAYMENT] = Ok(buildJsonObject { put("paymentId", "p9") })

            sync().syncAll()

            assertEquals(listOf("z-first", "a-second"), backend.mutations.map { it.second.clientId() })
        }

    @Test
    fun ourOwnAcceptedPaymentDoesNotCountAsAChangeForTheNextOne() =
        runTest {
            queueWhileOffline("c1")
            queueWhileOffline("c2")
            backend.answerTenantScreen(tenantScreen())
            val server = ServerThatShowsLoggedPayments(backend)

            val report = sync(server).syncAll()

            assertEquals(emptyList(), report.needsAttention)
            assertEquals(listOf("c1", "c2"), backend.mutations.map { it.second.clientId() })
        }

    @Test
    fun aTenantChangedOnTheServerWhileOfflineWaitsForTheMembersAnswer() =
        runTest {
            queueWhileOffline("c1")
            backend.answerTenantScreen(tenantScreen(payments = listOf(serverPayment("p-by-sam"))))

            val report = sync().syncAll()

            val waiting = checkNotNull(dao.byClientId("c1"))
            assertEquals(PendingPaymentStatus.NeedsConfirmation, waiting.status)
            assertEquals(PendingConfirmationReason.TenantChanged, waiting.confirmationReason)
            assertEquals(listOf(waiting), report.needsAttention)
            assertEquals(emptyList(), backend.mutations)
        }

    @Test
    fun anArchivedTenantIsLeftForTheServerToRefuse() =
        runTest {
            queueWhileOffline("c1")
            backend.answerTenantScreen(tenantScreen(tenantVersion = 2, status = TenantStatus.Archived))
            backend.mutationAnswers[LOG_PAYMENT] = Err(BackendError.Convex("TenantArchived", ARCHIVED, null))

            val report = sync().syncAll()

            val rejected = checkNotNull(dao.byClientId("c1"))
            assertEquals(PendingPaymentStatus.Rejected, rejected.status)
            assertEquals(PendingRejectionReason.TenantArchived, rejected.rejectionReason)
            assertEquals(ARCHIVED, rejected.rejectionMessage)
            assertEquals(listOf(rejected), report.needsAttention)
        }

    @Test
    fun aTenantTheMemberCanNoLongerSeeIsStillSentSoTheServerCanSayWhy() =
        runTest {
            queueWhileOffline("c1")
            backend.answerSubscription(TENANT_SCREEN, Err(BackendError.Convex("PermissionDenied", NO_ACCESS, null)))
            backend.mutationAnswers[LOG_PAYMENT] = Err(BackendError.Convex("PermissionDenied", NO_ACCESS, null))

            sync().syncAll()

            assertEquals(NO_ACCESS, dao.byClientId("c1")?.rejectionMessage)
        }

    @Test
    fun aPossibleDuplicateKeepsTheMatchesToShowTheMember() =
        runTest {
            queueWhileOffline("c1")
            backend.answerTenantScreen(tenantScreen())
            backend.mutationAnswers[LOG_PAYMENT] =
                Err(BackendError.Convex("NeedsConfirmation", "A similar payment was logged.", duplicateDetails()))

            val report = sync().syncAll()

            assertEquals(PendingConfirmationReason.PossibleDuplicate, report.needsAttention.single().confirmationReason)
            val match = reader.observeDuplicateMatches("c1").first().single()
            assertEquals(listOf("p1", "Sam Rivera", "2026-03-01"), listOf(match.matchId, match.byName, match.date))
            assertEquals(DuplicateMatchKind.Payment, match.kind)
        }

    @Test
    fun aConfirmedDuplicateIsSentWithConfirmation() =
        runTest {
            queueWhileOffline("c1")
            dao.update(checkNotNull(dao.byClientId("c1")).copy(duplicateConfirmed = true))
            backend.answerTenantScreen(tenantScreen())
            backend.mutationAnswers[LOG_PAYMENT] = Ok(buildJsonObject { put("paymentId", "p9") })

            sync().syncAll()

            assertEquals(JsonPrimitive(true), backend.mutations.single().second["confirmed"])
        }

    @Test
    fun noConnectionLeavesEverythingPendingForALaterTry() =
        runTest {
            queueWhileOffline("c1")
            backend.answerTenantScreen(tenantScreen())
            backend.isReachable = false

            val report = sync().syncAll()

            assertTrue(report.retryLater)
            assertEquals(PendingPaymentStatus.Pending, dao.byClientId("c1")?.status)
        }

    @Test
    fun anExpiredSessionIsTriedAgainLaterRatherThanRejected() =
        runTest {
            queueWhileOffline("c1")
            backend.answerTenantScreen(tenantScreen())
            backend.mutationAnswers[LOG_PAYMENT] = Err(BackendError.Convex("NotSignedIn", "Sign in again.", null))

            val report = sync().syncAll()

            assertTrue(report.retryLater)
            assertEquals(PendingPaymentStatus.Pending, dao.byClientId("c1")?.status)
        }

    @Test
    fun paymentsWaitingForAnAnswerAreNotSentAgain() =
        runTest {
            queueWhileOffline("c1")
            dao.update(
                checkNotNull(dao.byClientId("c1")).copy(
                    status = PendingPaymentStatus.NeedsConfirmation,
                    confirmationReason = PendingConfirmationReason.PossibleDuplicate,
                ),
            )
            backend.answerTenantScreen(tenantScreen())

            val report = sync().syncAll()

            assertFalse(report.retryLater)
            assertEquals(emptyList(), backend.mutations)
        }

    private suspend fun queueWhileOffline(clientId: String) {
        store.replaceTenant("t1", tenantScreen())
        queue.enqueue(clientId, "t1", marchSecondPayment)
    }

    private fun sync(server: Backend = backend) =
        PendingPaymentSync(
            refresher = TenantRefresher(server, database, store, FakeAsOfClock(TEN_O_CLOCK_UTC)),
            sender = PendingPaymentSender(server, database, NoOpTelemetry()),
            database = database,
        )

    private fun JsonObject.clientId(): String = (getValue("clientId") as JsonPrimitive).content

    private fun duplicateDetails() =
        buildJsonObject {
            put(
                "matches",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("kind", "payment")
                            put("id", "p1")
                            put("date", "2026-03-01")
                            put("amountCents", 150_000)
                            put("byName", "Sam Rivera")
                            put("byAccountId", "sam")
                        },
                    )
                },
            )
        }

    private class ServerThatShowsLoggedPayments(
        private val fake: FakeBackend,
    ) : Backend by fake {
        private val logged = mutableListOf<String>()

        override suspend fun <T> mutation(
            name: String,
            args: JsonObject,
            resultSerializer: KSerializer<T>,
        ): Result<T, BackendError> {
            val paymentId = "p${logged.size + 1}"
            logged += paymentId
            fake.mutationAnswers[name] = Ok(buildJsonObject { put("paymentId", paymentId) })
            val answer = fake.mutation(name, args, resultSerializer)
            fake.answerTenantScreen(tenantScreen(payments = logged.map { serverPayment(it) }))
            return answer
        }
    }

    private companion object {
        const val TEN_O_CLOCK_UTC = 1_794_736_800_000L
        const val ARCHIVED = "This tenant is archived. Restore the tenant to change payments and charges."
        const val NO_ACCESS = "You don't have access to this group."
    }
}
