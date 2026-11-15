package net.neednot.renttracker.push

import android.content.Context
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onErr
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.neednot.renttracker.core.data.PushPlatform
import net.neednot.renttracker.core.data.PushTokens
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry

class FirebasePushTokens(
    private val context: Context,
    private val telemetry: Telemetry,
) : PushTokens {
    private val newTokens =
        MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val platform: PushPlatform = PushPlatform.Android

    override val changes: Flow<String> = newTokens.asSharedFlow()

    init {
        if (!FirebaseSetup.isConfigured) telemetry.log(TAG, "Firebase is not configured, push is off", Severity.Warning)
    }

    fun onNewToken(token: String) {
        newTokens.tryEmit(token)
    }

    override suspend fun currentToken(): String? {
        val messaging = messagingOrNull() ?: return null
        return messaging.token
            .awaitResult()
            .onErr { reason -> telemetry.log(TAG, "Could not read the push token: $reason", Severity.Warning) }
            .get()
    }

    override suspend fun deleteToken() {
        val messaging = messagingOrNull() ?: return
        messaging
            .deleteToken()
            .awaitResult()
            .onErr { reason -> telemetry.log(TAG, "Could not delete the push token: $reason", Severity.Warning) }
    }

    private fun messagingOrNull(): FirebaseMessaging? =
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseMessaging.getInstance()

    private companion object {
        const val TAG = "FirebasePushTokens"
    }
}
