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
        PaymentEntity::class,
        DebtEntity::class,
        AccountNameEntity::class,
        MembershipEntity::class,
        LedgerRowEntity::class,
        DismissedLateFeeSuggestionEntity::class,
    ],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@ConstructedBy(RentTrackerDatabaseConstructor::class)
@Suppress("TooManyFunctions")
abstract class RentTrackerDatabase : RoomDatabase() {
    abstract fun pendingPaymentDao(): PendingPaymentDao

    abstract fun groupDao(): GroupDao

    abstract fun groupSummaryDao(): GroupSummaryDao

    abstract fun tenantDao(): TenantDao

    abstract fun tenantSummaryDao(): TenantSummaryDao

    abstract fun rentTermDao(): RentTermDao

    abstract fun paymentDao(): PaymentDao

    abstract fun debtDao(): DebtDao

    abstract fun accountNameDao(): AccountNameDao

    abstract fun membershipDao(): MembershipDao

    abstract fun ledgerRowDao(): LedgerRowDao

    abstract fun dismissedLateFeeSuggestionDao(): DismissedLateFeeSuggestionDao

    companion object {
        const val FILE_NAME = "rent-tracker.db"
    }
}
