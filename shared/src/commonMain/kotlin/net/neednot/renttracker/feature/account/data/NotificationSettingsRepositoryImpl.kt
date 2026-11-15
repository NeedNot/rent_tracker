package net.neednot.renttracker.feature.account.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.data.LateRentAlerts
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.GroupNotificationSetting
import net.neednot.renttracker.feature.account.domain.NotificationSettingsRepository
import net.neednot.renttracker.model.notificationsettings.NotificationSettingsListItem
import net.neednot.renttracker.model.notificationsettings.NotificationSettingsSetArgs
import kotlin.time.Duration.Companion.seconds

class NotificationSettingsRepositoryImpl(
    private val backend: Backend,
    private val lateRentAlerts: LateRentAlerts,
    private val telemetry: Telemetry,
) : NotificationSettingsRepository {
    override fun observeSettings(): Flow<Result<List<GroupNotificationSetting>, AccountError>> =
        backend
            .subscribe(LIST, JsonObject(emptyMap()), ListSerializer(NotificationSettingsListItem.serializer()))
            .map { result ->
                result
                    .map { items -> items.map { it.toGroupNotificationSetting() } }
                    .mapError { it.toAccountError() }
                    .reportingUnexpected()
            }

    override suspend fun setNotificationsOn(
        groupId: String,
        notificationsOn: Boolean,
    ): Result<Unit, AccountError> {
        val args =
            ConvexJson.encodeArgs(
                NotificationSettingsSetArgs.serializer(),
                NotificationSettingsSetArgs(groupId = groupId, notificationsOn = notificationsOn),
            )
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(SET, args, JsonElement.serializer())
            } ?: return Err(AccountError.Offline)
        return answer.map { }.mapError { it.toAccountError() }.reportingUnexpected()
    }

    override fun areNotificationsBlockedOnPhone(): Boolean = lateRentAlerts.areBlockedOnPhone()

    private fun <T> Result<T, AccountError>.reportingUnexpected(): Result<T, AccountError> =
        onErr { error ->
            if (error is AccountError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private fun NotificationSettingsListItem.toGroupNotificationSetting(): GroupNotificationSetting =
        GroupNotificationSetting(groupId = groupId, groupName = groupName, notificationsOn = notificationsOn)

    private companion object {
        const val TAG = "NotificationSettingsRepository"
        const val LIST = "notificationSettings:list"
        const val SET = "notificationSettings:set"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
