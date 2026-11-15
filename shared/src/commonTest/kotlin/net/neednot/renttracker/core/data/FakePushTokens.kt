package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.MutableSharedFlow

class FakePushTokens(
    var token: String? = "phone-token",
) : PushTokens {
    override val platform: PushPlatform = PushPlatform.Android

    override val changes = MutableSharedFlow<String>()

    var deletedCount = 0
        private set

    override suspend fun currentToken(): String? = token

    override suspend fun deleteToken() {
        deletedCount++
    }
}
