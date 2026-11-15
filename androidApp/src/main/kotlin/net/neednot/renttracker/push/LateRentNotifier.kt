package net.neednot.renttracker.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import net.neednot.renttracker.MainActivity
import net.neednot.renttracker.R

class LateRentNotifier(
    private val context: Context,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun notify(push: LateRentPush) {
        if (!notificationManager.areNotificationsEnabled()) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.late_rent_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        notificationManager.notify(push.groupId, NOTIFICATION_ID, notificationFor(push))
    }

    private fun notificationFor(push: LateRentPush): Notification =
        Notification
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(push.title)
            .setContentText(push.body)
            .setStyle(Notification.BigTextStyle().bigText(push.body))
            .setContentIntent(openGroup(push.groupId))
            .setAutoCancel(true)
            .build()

    private fun openGroup(groupId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            groupId.hashCode(),
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_GROUP)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ).putExtra(EXTRA_GROUP_ID, groupId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val EXTRA_GROUP_ID = "net.neednot.renttracker.extra.GROUP_ID"
        private const val ACTION_OPEN_GROUP = "net.neednot.renttracker.action.OPEN_GROUP"
        private const val CHANNEL_ID = "late-rent"
        private const val NOTIFICATION_ID = 1
    }
}
