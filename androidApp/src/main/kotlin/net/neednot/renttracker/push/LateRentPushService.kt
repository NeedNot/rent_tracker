package net.neednot.renttracker.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class LateRentPushService : FirebaseMessagingService() {
    private val notifier: LateRentNotifier by inject()
    private val pushTokens: FirebasePushTokens by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        LateRentPush.fromData(message.data)?.let(notifier::notify)
    }

    override fun onNewToken(token: String) {
        pushTokens.onNewToken(token)
    }
}
