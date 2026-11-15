package net.neednot.renttracker.core.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import kotlin.time.Clock

val coreDataModule =
    module {
        single {
            get<RoomDatabase.Builder<RentTrackerDatabase>>()
                .setDriver(BundledSQLiteDriver())
                .addMigrations(PendingPaymentsMigration)
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        single { get<RentTrackerDatabase>().pendingPaymentDao() }
        single { get<RentTrackerDatabase>().paymentDao() }
        single { get<RentTrackerDatabase>().debtDao() }
        single { get<RentTrackerDatabase>().accountNameDao() }
        single { get<RentTrackerDatabase>().membershipDao() }
        single { get<RentTrackerDatabase>().ledgerRowDao() }
        single { get<RentTrackerDatabase>().dismissedLateFeeSuggestionDao() }
        single { ScreenResultStore(database = get()) }
        single { GroupAccessWatcher(database = get(), screenResultStore = get()) }
        single { AccountProfileSync(backend = get(), database = get(), screenResultStore = get()) }
        single<AsOfClock> { HourlyAsOfClock(clock = Clock.System) }
        single { PendingPaymentQueue(database = get(), scheduler = get()) }
        single { PendingPaymentReader(database = get()) }
        single { TenantRefresher(backend = get(), database = get(), screenResultStore = get(), asOfClock = get()) }
        single { PendingPaymentSender(backend = get(), database = get(), telemetry = get()) }
        single { PendingPaymentSync(refresher = get(), sender = get(), database = get()) }
        single { FreshnessSource(connectivity = get(), database = get(), clock = Clock.System) }
        single { PendingPaymentLinks() }
        single { PendingPaymentAutoSync(connectivity = get(), database = get(), sync = get()) }
    }
