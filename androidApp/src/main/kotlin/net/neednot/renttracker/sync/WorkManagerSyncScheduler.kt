package net.neednot.renttracker.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.neednot.renttracker.core.data.PendingPaymentSyncScheduler
import java.util.concurrent.TimeUnit

class WorkManagerSyncScheduler(
    private val context: Context,
) : PendingPaymentSyncScheduler {
    override fun scheduleSync() {
        val request =
            OneTimeWorkRequestBuilder<PendingPaymentSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "pending-payment-sync"
        const val BACKOFF_SECONDS = 30L
    }
}
