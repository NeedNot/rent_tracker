package net.neednot.renttracker.core.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

private const val VERSION_WITHOUT_QUEUE = 6
private const val VERSION_WITH_QUEUE = 7

// Nothing wrote to pending_payments before version 7, so the table is recreated instead of copied.
object PendingPaymentsMigration : Migration(VERSION_WITHOUT_QUEUE, VERSION_WITH_QUEUE) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `pending_payments`")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_payments` (`clientId` TEXT NOT NULL, `sequence` INTEGER NOT NULL, " +
                "`groupId` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `tenantName` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `dateReceived` TEXT NOT NULL, `method` TEXT, `note` TEXT, " +
                "`status` TEXT NOT NULL, `confirmationReason` TEXT, `rejectionMessage` TEXT, " +
                "`duplicateConfirmed` INTEGER NOT NULL, `tenantFingerprint` TEXT NOT NULL, " +
                "`balanceCentsWhenLogged` INTEGER, PRIMARY KEY(`clientId`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pending_payments_tenantId` ON `pending_payments` (`tenantId`)",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_payments_sequence` ON `pending_payments` (`sequence`)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_duplicate_matches` (`clientId` TEXT NOT NULL, " +
                "`matchId` TEXT NOT NULL, `kind` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `method` TEXT, `byName` TEXT NOT NULL, " +
                "`byAccountId` TEXT NOT NULL, PRIMARY KEY(`clientId`, `matchId`))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cache_times` (`scope` TEXT NOT NULL, `syncedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`scope`))",
        )
    }
}
