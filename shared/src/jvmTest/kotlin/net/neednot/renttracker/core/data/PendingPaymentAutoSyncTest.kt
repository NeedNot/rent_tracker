package net.neednot.renttracker.core.data

import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class PendingPaymentAutoSyncTest {
    private val database = inMemoryDatabase()
    private val backend = FakeBackend()
    private val connectivity = FakeConnectivity(online = false)
    private val reader = PendingPaymentReader(database)
    private val queue = PendingPaymentQueue(database, RecordingSyncScheduler())
    private val sync =
        PendingPaymentSync(
            refresher = TenantRefresher(backend, database, ScreenResultStore(database), FakeAsOfClock(0)),
            sender = PendingPaymentSender(backend, database, NoOpTelemetry()),
            database = database,
        )
    private val autoSync = PendingPaymentAutoSync(connectivity, database, sync)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun paymentsLoggedOfflineAreSentWhenTheConnectionReturns() =
        runTest {
            ScreenResultStore(database).replaceTenant("t1", tenantScreen())
            backend.answerTenantScreen(tenantScreen())
            backend.mutationAnswers[LOG_PAYMENT] = Ok(buildJsonObject { put("paymentId", "p9") })
            queue.enqueue("c1", "t1", marchSecondPayment)
            val running = launch { autoSync.syncWhileOnline() }

            connectivity.isOnline.value = true

            withContext(Dispatchers.Default) {
                withTimeout(5.seconds) { reader.observeUnsyncedCount().filter { it == 0 }.first() }
            }
            assertEquals(listOf(LOG_PAYMENT), backend.mutations.map { it.first })
            running.cancel()
        }

    @Test
    fun nothingIsSentWhileOffline() =
        runTest {
            ScreenResultStore(database).replaceTenant("t1", tenantScreen())
            queue.enqueue("c1", "t1", marchSecondPayment)
            val running = launch { autoSync.syncWhileOnline() }

            testScheduler.advanceUntilIdle()

            assertEquals(emptyList(), backend.mutations)
            running.cancel()
        }
}
