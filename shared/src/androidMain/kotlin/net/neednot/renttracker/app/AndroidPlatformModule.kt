package net.neednot.renttracker.app

import androidx.room.Room
import androidx.room.RoomDatabase
import net.neednot.renttracker.core.data.RentTrackerDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidPlatformModule =
    module {
        single<RoomDatabase.Builder<RentTrackerDatabase>> {
            val context = androidContext()
            Room.databaseBuilder<RentTrackerDatabase>(
                context = context,
                name = context.getDatabasePath(RentTrackerDatabase.FILE_NAME).absolutePath,
            )
        }
    }
