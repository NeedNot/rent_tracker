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
        single<AsOfClock> { HourlyAsOfClock(clock = Clock.System) }
    }
