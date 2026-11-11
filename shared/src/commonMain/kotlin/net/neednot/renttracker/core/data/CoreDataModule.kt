package net.neednot.renttracker.core.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module

val coreDataModule =
    module {
        single {
            get<RoomDatabase.Builder<RentTrackerDatabase>>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        single { get<RentTrackerDatabase>().pendingPaymentDao() }
    }
