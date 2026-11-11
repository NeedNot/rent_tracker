package net.neednot.renttracker.core.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PendingPaymentEntity::class], version = 1)
@ConstructedBy(RentTrackerDatabaseConstructor::class)
abstract class RentTrackerDatabase : RoomDatabase() {
    abstract fun pendingPaymentDao(): PendingPaymentDao

    companion object {
        const val FILE_NAME = "rent-tracker.db"
    }
}
