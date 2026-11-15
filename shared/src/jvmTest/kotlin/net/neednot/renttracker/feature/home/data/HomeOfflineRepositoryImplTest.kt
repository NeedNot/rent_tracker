package net.neednot.renttracker.feature.home.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.FakeConnectivity
import net.neednot.renttracker.core.data.FreshnessSource
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.core.data.PendingPaymentReader
import net.neednot.renttracker.core.data.PendingPaymentStatus
import net.neednot.renttracker.core.data.PendingRejectionReason
import net.neednot.renttracker.core.data.RecordingSyncScheduler
import net.neednot.renttracker.core.data.inMemoryDatabase
import net.neednot.renttracker.core.data.pendingPaymentEntity
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.feature.home.domain.RejectedPayment
import net.neednot.renttracker.feature.home.domain.RejectedPaymentReason
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class HomeOfflineRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val connectivity = FakeConnectivity()

    private val repository =
        HomeOfflineRepositoryImpl(
            freshnessSource = FreshnessSource(connectivity, database, Clock.System),
            pendingPaymentReader = PendingPaymentReader(database),
            pendingPaymentQueue = PendingPaymentQueue(database, RecordingSyncScheduler()),
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private suspend fun insertRejected(
        clientId: String,
        reason: PendingRejectionReason,
        message: String? = null,
    ) {
        database.pendingPaymentDao().insert(
            pendingPaymentEntity(clientId, "t1").copy(
                tenantName = "Apt 3B",
                amountCents = 55_000,
                dateReceived = "2026-03-02",
                status = PendingPaymentStatus.Rejected,
                rejectionReason = reason,
                rejectionMessage = message,
            ),
        )
    }

    @Test
    fun freshnessIsOnlineWhileConnected() =
        runTest {
            assertEquals(Freshness.Online, repository.observeFreshness().first())
        }

    @Test
    fun freshnessIsOfflineWithoutAnAgeWhenHomeWasNeverCached() =
        runTest {
            connectivity.isOnline.value = false

            assertEquals(Freshness.Offline(null), repository.observeFreshness().first())
        }

    @Test
    fun aRejectedPaymentWhoseTenantIsNotOnThePhoneIsListedWithItsReason() =
        runTest {
            insertRejected("c1", PendingRejectionReason.TenantMoved)

            val expected =
                RejectedPayment(
                    clientId = "c1",
                    tenantName = "Apt 3B",
                    amountCents = 55_000,
                    dateReceived = LocalDate(2026, 3, 2),
                    reason = RejectedPaymentReason.TenantMoved,
                    message = null,
                )
            assertEquals(listOf(expected), repository.observeRejectedPayments().first())
        }

    @Test
    fun anOtherRejectionKeepsTheServerMessage() =
        runTest {
            insertRejected("c1", PendingRejectionReason.Other, message = "Amount is too large.")

            val payment = repository.observeRejectedPayments().first().single()

            assertEquals(RejectedPaymentReason.Other, payment.reason)
            assertEquals("Amount is too large.", payment.message)
        }

    @Test
    fun aPaymentThatIsStillPendingIsNotListedAsRejected() =
        runTest {
            database.pendingPaymentDao().insert(pendingPaymentEntity("c1", "t1"))

            assertEquals(emptyList(), repository.observeRejectedPayments().first())
        }

    @Test
    fun dismissingARejectedPaymentDiscardsIt() =
        runTest {
            insertRejected("c1", PendingRejectionReason.NoPermission)

            repository.dismissRejectedPayment("c1")

            assertEquals(emptyList(), repository.observeRejectedPayments().first())
        }
}
