package net.neednot.renttracker.feature.account.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

interface NotificationSettingsRepository {
    fun observeSettings(): Flow<Result<List<GroupNotificationSetting>, AccountError>>

    suspend fun setNotificationsOn(
        groupId: String,
        notificationsOn: Boolean,
    ): Result<Unit, AccountError>

    fun areNotificationsBlockedOnPhone(): Boolean
}
