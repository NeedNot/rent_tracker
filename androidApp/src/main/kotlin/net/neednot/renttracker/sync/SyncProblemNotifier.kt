package net.neednot.renttracker.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import net.neednot.renttracker.MainActivity
import net.neednot.renttracker.R
import net.neednot.renttracker.core.data.PendingPaymentEntity

class SyncProblemNotifier(
    private val context: Context,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun notify(needsAttention: List<PendingPaymentEntity>) {
        if (needsAttention.isEmpty() || !notificationManager.areNotificationsEnabled()) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.sync_problem_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        needsAttention.groupBy { it.tenantId }.values.forEach { payments ->
            val first = payments.first()
            notificationManager.notify(first.tenantId, NOTIFICATION_ID, notificationFor(first, payments.size))
        }
    }

    private fun notificationFor(
        first: PendingPaymentEntity,
        count: Int,
    ): Notification =
        Notification
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(
                context.resources.getQuantityString(R.plurals.sync_problem_title, count, count, first.tenantName),
            ).setContentIntent(openPayment(first))
            .setAutoCancel(true)
            .build()

    private fun openPayment(payment: PendingPaymentEntity): PendingIntent =
        PendingIntent.getActivity(
            context,
            payment.tenantId.hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ).putExtra(EXTRA_TENANT_ID, payment.tenantId)
                .putExtra(EXTRA_CLIENT_ID, payment.clientId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val EXTRA_TENANT_ID = "net.neednot.renttracker.extra.TENANT_ID"
        const val EXTRA_CLIENT_ID = "net.neednot.renttracker.extra.PENDING_CLIENT_ID"
        private const val CHANNEL_ID = "payment-sync"
        private const val NOTIFICATION_ID = 1
    }
}
