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
        CalendarMonthEntity::class,
        AllocationEntity::class,
        SignedInAccountEntity::class,
        MemberProfileEntity::class,
        InviteEntity::class,
        HomeInvitationEntity::class,
        AccountProfileEntity::class,
        PendingDuplicateMatchEntity::class,
        CacheTimeEntity::class,
    ],
    version = 7,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
        ), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
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

    abstract fun calendarMonthDao(): CalendarMonthDao

    abstract fun allocationDao(): AllocationDao

    abstract fun signedInAccountDao(): SignedInAccountDao

    abstract fun memberProfileDao(): MemberProfileDao

    abstract fun inviteDao(): InviteDao

    abstract fun homeInvitationDao(): HomeInvitationDao

    abstract fun groupCacheDao(): GroupCacheDao

    abstract fun cacheTimeDao(): CacheTimeDao

    abstract fun accountProfileDao(): AccountProfileDao

    companion object {
        const val FILE_NAME = "rent-tracker.db"
    }
}
