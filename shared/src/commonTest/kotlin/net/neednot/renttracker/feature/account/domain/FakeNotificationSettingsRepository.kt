package net.neednot.renttracker.feature.account.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class FakeNotificationSettingsRepository : NotificationSettingsRepository {
    val settings = MutableStateFlow<Result<List<GroupNotificationSetting>, AccountError>?>(null)
    val saved = mutableListOf<Pair<String, Boolean>>()
    var saveAnswer = CompletableDeferred<Result<Unit, AccountError>>().apply { complete(Ok(Unit)) }
    var blockedOnPhone = false

    override fun observeSettings(): Flow<Result<List<GroupNotificationSetting>, AccountError>> =
        settings.filterNotNull()

    override suspend fun setNotificationsOn(
        groupId: String,
        notificationsOn: Boolean,
    ): Result<Unit, AccountError> {
        saved += groupId to notificationsOn
        return saveAnswer.await()
    }

    override fun areNotificationsBlockedOnPhone(): Boolean = blockedOnPhone
}
