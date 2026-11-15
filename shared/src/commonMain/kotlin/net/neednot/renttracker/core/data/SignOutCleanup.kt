package net.neednot.renttracker.core.data

class SignOutCleanup(
    private val pendingPaymentQueue: PendingPaymentQueue,
    private val pushTokenRegistration: PushTokenRegistration,
) {
    suspend fun beforeSignOut() {
        pushTokenRegistration.unregisterCurrentToken()
    }

    suspend fun afterSignOut() {
        pendingPaymentQueue.clear()
        pushTokenRegistration.deleteLocalToken()
    }
}
