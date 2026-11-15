package net.neednot.renttracker.core.data

class RecordingSyncScheduler : PendingPaymentSyncScheduler {
    var scheduledCount = 0
        private set

    override fun scheduleSync() {
        scheduledCount++
    }
}
