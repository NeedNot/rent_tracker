package net.neednot.renttracker.core.data

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PendingPaymentEntity::class,
        GroupEntity::class,
        GroupSummaryEntity::class,
        TenantEntity::class,
        TenantSummaryEntity::class,
        RentTermEntity::class,
    ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(RentTrackerDatabaseConstructor::class)
abstract class RentTrackerDatabase : RoomDatabase() {
    abstract fun pendingPaymentDao(): PendingPaymentDao

    abstract fun groupDao(): GroupDao

    abstract fun groupSummaryDao(): GroupSummaryDao

    abstract fun tenantDao(): TenantDao

    abstract fun tenantSummaryDao(): TenantSummaryDao

    abstract fun rentTermDao(): RentTermDao

    companion object {
        const val FILE_NAME = "rent-tracker.db"
    }
}
