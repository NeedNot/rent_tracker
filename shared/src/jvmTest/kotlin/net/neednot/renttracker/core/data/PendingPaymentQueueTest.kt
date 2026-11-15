package net.neednot.renttracker.core.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingPaymentQueueTest {
    private val database = inMemoryDatabase()
    private val scheduler = RecordingSyncScheduler()
    private val queue = PendingPaymentQueue(database, scheduler)
    private val dao = database.pendingPaymentDao()
    private val store = ScreenResultStore(database)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aQueuedPaymentRemembersTheTenantAsThePhoneSawIt() =
        runTest {
            store.replaceTenant("t1", tenantScreen(tenantVersion = 7, payments = listOf(serverPayment("p1"))))

            queue.enqueue("c1", "t1", marchSecondPayment)

            val queued = checkNotNull(dao.byClientId("c1"))
            assertEquals("g1", queued.groupId)
            assertEquals("Apt 3B", queued.tenantName)
            assertEquals(55_000L, queued.balanceCentsWhenLogged)
            assertEquals("payment:p1:1,tenant:7", queued.tenantFingerprint)
            assertEquals(1, scheduler.scheduledCount)
        }

    @Test
    fun queuedPaymentsAreNumberedInTheOrderTheyWereLogged() =
        runTest {
            store.replaceTenant("t1", tenantScreen())

            queue.enqueue("later-id", "t1", marchSecondPayment)
            queue.enqueue("earlier-id", "t1", marchSecondPayment)

            assertEquals(listOf("later-id", "earlier-id"), dao.observeAll().first().map { it.clientId })
        }

    @Test
    fun aTenantMissingFromTheCacheCannotBeQueued() =
        runTest {
            assertEquals(Err(PendingQueueError.TenantNotCached), queue.enqueue("c1", "t1", marchSecondPayment))
            assertEquals(0, scheduler.scheduledCount)
        }

    @Test
    fun editingAPaymentThatNeedsConfirmationSendsItAgainWithoutTheOldAnswer() =
        runTest {
            store.replaceTenant("t1", tenantScreen())
            dao.insert(possibleDuplicate("c1").copy(duplicateConfirmed = true))
            dao.insertDuplicateMatches(listOf(duplicateMatch("c1")))

            val result = queue.edit("c1", marchSecondPayment.copy(amountCents = 140_000))

            assertEquals(Ok(Unit), result)
            val edited = checkNotNull(dao.byClientId("c1"))
            assertEquals(140_000L, edited.amountCents)
            assertEquals(PendingPaymentStatus.Pending, edited.status)
            assertNull(edited.confirmationReason)
            assertFalse(edited.duplicateConfirmed)
            assertEquals(emptyList(), queue.observeDuplicateMatches("c1").first())
            assertEquals(1, scheduler.scheduledCount)
        }

    @Test
    fun confirmingAPossibleDuplicateSendsItWithConfirmation() =
        runTest {
            store.replaceTenant("t1", tenantScreen())
            dao.insert(possibleDuplicate("c1"))

            queue.confirm("c1")

            val confirmed = checkNotNull(dao.byClientId("c1"))
            assertTrue(confirmed.duplicateConfirmed)
            assertEquals(PendingPaymentStatus.Pending, confirmed.status)
        }

    @Test
    fun confirmingAfterTheTenantChangedAcceptsTheTenantAsItIsNow() =
        runTest {
            store.replaceTenant("t1", tenantScreen(tenantVersion = 8, rentOwedCents = 0))
            dao.insert(
                pendingPaymentEntity("c1", "t1").copy(
                    status = PendingPaymentStatus.NeedsConfirmation,
                    confirmationReason = PendingConfirmationReason.TenantChanged,
                    tenantFingerprint = "tenant:7",
                    balanceCentsWhenLogged = 55_000,
                ),
            )

            queue.confirm("c1")

            val confirmed = checkNotNull(dao.byClientId("c1"))
            assertEquals("tenant:8", confirmed.tenantFingerprint)
            assertEquals(0L, confirmed.balanceCentsWhenLogged)
            assertFalse(confirmed.duplicateConfirmed)
            assertEquals(PendingPaymentStatus.Pending, confirmed.status)
        }

    @Test
    fun aRejectedPaymentCanOnlyBeDismissed() =
        runTest {
            dao.insert(pendingPaymentEntity("c1", "t1").copy(status = PendingPaymentStatus.Rejected))

            assertEquals(Err(PendingQueueError.NotEditable), queue.edit("c1", marchSecondPayment))
            assertEquals(Err(PendingQueueError.NotEditable), queue.confirm("c1"))

            queue.discard("c1")

            assertNull(dao.byClientId("c1"))
        }

    @Test
    fun aPaymentThatAlreadySyncedIsNotFound() =
        runTest {
            assertEquals(Err(PendingQueueError.NotFound), queue.edit("c1", marchSecondPayment))
        }

    @Test
    fun discardingRemovesTheDuplicateMatchesToo() =
        runTest {
            dao.insert(possibleDuplicate("c1"))
            dao.insertDuplicateMatches(listOf(duplicateMatch("c1")))

            queue.discard("c1")

            assertEquals(emptyList(), queue.observeDuplicateMatches("c1").first())
        }

    @Test
    fun clearingEmptiesTheQueue() =
        runTest {
            dao.insert(pendingPaymentEntity("c1", "t1"))
            dao.insert(possibleDuplicate("c2").copy(sequence = 2))
            dao.insertDuplicateMatches(listOf(duplicateMatch("c2")))

            queue.clear()

            assertEquals(0, queue.observeUnsyncedCount().first())
            assertEquals(emptyList(), queue.observeDuplicateMatches("c2").first())
        }

    @Test
    fun theFingerprintChangesWhenAnotherMemberLogsAPayment() =
        runTest {
            store.replaceTenant("t1", tenantScreen())
            val before = dao.tenantFingerprint("t1")

            store.replaceTenant("t1", tenantScreen(payments = listOf(serverPayment("p2"))))

            assertNotEquals(before, dao.tenantFingerprint("t1"))
        }

    private fun possibleDuplicate(clientId: String) =
        pendingPaymentEntity(clientId, "t1").copy(
            status = PendingPaymentStatus.NeedsConfirmation,
            confirmationReason = PendingConfirmationReason.PossibleDuplicate,
        )

    private fun duplicateMatch(clientId: String) =
        PendingDuplicateMatchEntity(
            clientId = clientId,
            matchId = "p1",
            kind = DuplicateMatchKind.Payment,
            date = "2026-03-02",
            amountCents = 150_000,
            method = PaymentMethod.Check,
            byName = "Sam Rivera",
            byAccountId = "sam",
        )
}
