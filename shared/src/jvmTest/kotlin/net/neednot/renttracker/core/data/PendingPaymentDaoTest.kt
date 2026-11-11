package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingPaymentDaoTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val dao = database.pendingPaymentDao()

    private val payment =
        PendingPaymentEntity(
            clientId = "c1",
            tenantId = "t1",
            amountCents = 150000,
            dateReceived = "2026-03-02",
            method = "check",
            note = null,
            status = PendingPaymentStatus.Pending,
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertedPaymentIsObserved() =
        runTest {
            dao.insert(payment)

            assertEquals(listOf(payment), dao.observeAll().first())
        }

    @Test
    fun deletedPaymentIsGone() =
        runTest {
            dao.insert(payment)
            dao.insert(payment.copy(clientId = "c2", status = PendingPaymentStatus.Rejected))

            dao.deleteByClientId("c1")

            assertEquals(listOf("c2"), dao.observeAll().first().map { it.clientId })
        }
}
