package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.Flow

interface PushTokens {
    val platform: PushPlatform

    val changes: Flow<String>

    suspend fun currentToken(): String?

    suspend fun deleteToken()
}
