package net.neednot.renttracker.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.neednot.renttracker.auth.ForegroundActivity
import net.neednot.renttracker.core.data.PendingPaymentSync
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PendingPaymentSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters),
    KoinComponent {
    private val sync: PendingPaymentSync by inject()
    private val notifier: SyncProblemNotifier by inject()
    private val foregroundActivity: ForegroundActivity by inject()

    override suspend fun doWork(): Result {
        val report = sync.syncAll()
        if (foregroundActivity.current == null) notifier.notify(report.needsAttention)
        return if (report.retryLater) Result.retry() else Result.success()
    }
}
