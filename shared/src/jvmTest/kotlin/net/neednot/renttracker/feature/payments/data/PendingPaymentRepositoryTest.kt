package net.neednot.renttracker.feature.payments.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.CacheScope
import net.neednot.renttracker.core.data.CacheTimeEntity
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.FakeConnectivity
import net.neednot.renttracker.core.data.FreshnessSource
import net.neednot.renttracker.core.data.GroupAccessWatcher
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.core.data.PendingPaymentReader
import net.neednot.renttracker.core.data.PendingPaymentStatus
import net.neednot.renttracker.core.data.RecordingSyncScheduler
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.SyncAlerts
import net.neednot.renttracker.core.data.pendingPaymentEntity
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PendingPayment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PendingPaymentRepositoryTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val connectivity = FakeConnectivity()
    private val syncScheduler = RecordingSyncScheduler()
    private val queue = PendingPaymentQueue(database, syncScheduler)
    private val now = Instant.fromEpochMilliseconds(NOW)

    private val repository =
        PaymentRepositoryImpl(
            calls = PaymentBackendCalls(FakeBackend(), NoOpTelemetry()),
            asOfClock = FakeAsOfClock(NOW),
            database = database,
            screenResultStore = ScreenResultStore(database),
            groupAccessWatcher = GroupAccessWatcher(database, ScreenResultStore(database)),
            phoneOutbox =
                PhoneOutbox(
                    connectivity,
                    queue,
                    PendingPaymentReader(database),
                    FreshnessSource(
                        connectivity,
                        database,
                        clock =
                            object : Clock {
                                override fun now() = now
                            },
                    ),
                ),
        )

    private val edited =
        PaymentDraft(
            amountCents = 90_000,
            dateReceived = LocalDate(2026, 3, 4),
            method = PaymentMethod.Zelle,
            note = "Corrected",
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aPendingPaymentIsObservedAsADraftWithTheTenantName() =
        runTest {
            database.pendingPaymentDao().insert(pendingPaymentEntity("c1", "t1"))

            val pending = repository.observePendingPayment("c1").first()

            val expected = PaymentDraft(150_000, LocalDate(2026, 3, 2), PaymentMethod.Check, note = null)
            assertEquals(PendingPayment("Tenant t1", expected), pending)
        }

    @Test
    fun aPendingPaymentThatIsGoneObservesAsNothing() =
        runTest {
            assertEquals(null, repository.observePendingPayment("missing").first())
        }

    @Test
    fun editingAPendingPaymentChangesItOnThePhoneAndSchedulesASync() =
        runTest {
            database.pendingPaymentDao().insert(pendingPaymentEntity("c1", "t1"))

            val result = repository.editPendingPayment("c1", edited)

            assertEquals(Ok(Unit), result)
            val stored = database.pendingPaymentDao().byClientId("c1")
            assertEquals(
                listOf(90_000L, "2026-03-04", PaymentMethod.Zelle, "Corrected", PendingPaymentStatus.Pending),
                listOf(stored?.amountCents, stored?.dateReceived, stored?.method, stored?.note, stored?.status),
            )
            assertEquals(1, syncScheduler.scheduledCount)
        }

    @Test
    fun editingAPendingPaymentThatSyncedMeanwhileIsNotFound() =
        runTest {
            assertEquals(Err(PaymentError.NotFound), repository.editPendingPayment("missing", edited))
        }

    @Test
    fun editingARejectedPaymentIsNotFound() =
        runTest {
            database
                .pendingPaymentDao()
                .insert(pendingPaymentEntity("c1", "t1").copy(status = PendingPaymentStatus.Rejected))

            assertEquals(Err(PaymentError.NotFound), repository.editPendingPayment("c1", edited))
        }

    @Test
    fun freshnessIsOnlineWhenTheConnectionIsUp() =
        runTest {
            assertEquals(Freshness.Online, repository.observeFreshness("t1").first())
        }

    @Test
    fun freshnessOfflineIsTheAgeOfTheTenantsCache() =
        runTest {
            connectivity.isOnline.value = false
            database.cacheTimeDao().upsert(
                CacheTimeEntity(CacheScope.tenant("t1"), NOW - 40.minutes.inWholeMilliseconds),
            )

            assertEquals(Freshness.Offline(40.minutes), repository.observeFreshness("t1").first())
        }

    @Test
    fun syncAlertsDelegateToTheCoreAlerts() {
        val core = RecordingSyncAlerts()
        val alerts = PaymentSyncAlertsImpl(core)

        val shouldExplain = alerts.shouldExplain()
        alerts.markExplained()
        alerts.requestPermission()

        assertEquals(listOf(true, 1, 1), listOf(shouldExplain, core.explained, core.permissionRequests))
    }

    private class RecordingSyncAlerts : SyncAlerts {
        var explained = 0
        var permissionRequests = 0

        override fun shouldExplain(): Boolean = true

        override fun markExplained() {
            explained++
        }

        override fun requestPermission() {
            permissionRequests++
        }
    }

    private companion object {
        const val NOW = 1_794_736_800_000L
    }
}
